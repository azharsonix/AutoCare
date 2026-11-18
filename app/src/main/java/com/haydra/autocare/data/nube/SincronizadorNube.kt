package com.haydra.autocare.data.nube

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.haydra.autocare.data.local.db.AutoCareDatabase
import com.haydra.autocare.data.local.db.toModel
import com.haydra.autocare.data.repository.FirebaseAuthRepository
import com.haydra.autocare.notifications.DocumentoScheduler
import com.haydra.autocare.notifications.ReminderScheduler
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Respaldo de los datos en Cloud Firestore (Sprint 6, servicios en la nube).
 *
 * Estructura: `usuarios/{uid}/vehiculos/{id}` y, al mismo nivel, las colecciones
 * `mantenimientos`, `gastos`, `recordatorios` y `documentos`, cada documento con su `vehiculoId`.
 * Las reglas de `firestore.rules` solo dejan leer y escribir al dueño de ese uid.
 *
 * - Cada alta, cambio o baja se manda en cuanto ocurre. Firestore la guarda en su caché y la sube
 *   cuando hay conexión, así la app sigue funcionando sin internet.
 * - Al iniciar sesión, [sincronizar] junta lo de la nube con lo del teléfono: lo que solo está en
 *   la nube se descarga (por ejemplo, en un teléfono nuevo) y, la primera vez en este teléfono, lo
 *   que solo está en el teléfono se sube. En las siguientes veces, lo que falta en la nube es porque
 *   se borró desde otro teléfono, y se borra aquí también.
 */
class SincronizadorNube(
    context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: AutoCareDatabase,
    private val reminderScheduler: ReminderScheduler,
    private val documentoScheduler: DocumentoScheduler
) {
    private val preferencias = context.getSharedPreferences("autocare_nube", Context.MODE_PRIVATE)
    private val candado = Mutex()

    private fun raiz(): DocumentReference? = auth.currentUser?.uid?.let {
        firestore.collection(FirebaseAuthRepository.COLECCION_USUARIOS).document(it)
    }

    private fun coleccion(nombre: String): CollectionReference? = raiz()?.collection(nombre)

    /** Sube (o reemplaza) un documento. Sin sesión de Firebase no hace nada. */
    fun guardar(nombreColeccion: String, id: String, datos: Map<String, Any?>) {
        coleccion(nombreColeccion)?.document(id)?.set(datos)
    }

    fun borrar(nombreColeccion: String, id: String) {
        coleccion(nombreColeccion)?.document(id)?.delete()
    }

    /** Ids de todo lo que cuelga de un vehículo, para borrarlo de la nube junto con él. */
    suspend fun registrosDeVehiculo(vehiculoId: String): Map<String, List<String>> = mapOf(
        MapeoNube.MANTENIMIENTOS to database.maintenanceDao().getByVehiculo(vehiculoId).map { it.id },
        MapeoNube.GASTOS to database.expenseDao().getByVehiculo(vehiculoId).map { it.id },
        MapeoNube.RECORDATORIOS to database.reminderDao().getByVehiculo(vehiculoId).map { it.id },
        MapeoNube.DOCUMENTOS to database.documentoDao().getByVehiculo(vehiculoId).map { it.id }
    )

    /** Borra el vehículo y sus registros de la nube en un solo lote. */
    fun borrarVehiculo(vehiculoId: String, registros: Map<String, List<String>>) {
        val base = raiz() ?: return
        val referencias = registros.flatMap { (nombre, ids) -> ids.map { base.collection(nombre).document(it) } } +
            base.collection(MapeoNube.VEHICULOS).document(vehiculoId)
        referencias.chunked(LIMITE_LOTE).forEach { grupo ->
            firestore.batch().apply { grupo.forEach { delete(it) } }.commit()
        }
    }

    /**
     * Junta los datos de la nube con los del teléfono para el usuario local [usuarioId].
     * Si no hay conexión ni caché, falla y la app sigue con lo que tiene en Room.
     */
    suspend fun sincronizar(usuarioId: String): Result<Unit> = runCatching {
        candado.withLock {
            val base = raiz() ?: return@withLock
            val uid = auth.currentUser?.uid ?: return@withLock
            val claveSubida = "subida_inicial_$uid"
            val primeraVez = !preferencias.getBoolean(claveSubida, false)

            val remotos = (listOf(MapeoNube.VEHICULOS) + MapeoNube.DE_VEHICULO).associateWith { nombre ->
                base.collection(nombre).get(Source.SERVER).await().documents.associate { it.id to (it.data ?: emptyMap()) }
            }
            val subir = mutableListOf<Pair<DocumentReference, Map<String, Any?>>>()

            // Vehículos primero, para que las llaves foráneas de los registros existan.
            val vehiculoDao = database.vehicleDao()
            val vehiculosNube = remotos.getValue(MapeoNube.VEHICULOS)
            vehiculosNube.forEach { (id, datos) -> vehiculoDao.upsert(MapeoNube.vehiculo(id, usuarioId, datos)) }
            vehiculoDao.getByUsuario(usuarioId).filter { it.id !in vehiculosNube }.forEach { local ->
                if (primeraVez) {
                    subir += base.collection(MapeoNube.VEHICULOS).document(local.id) to MapeoNube.aMapa(local)
                } else {
                    vehiculoDao.deleteById(local.id)
                }
            }
            val vehiculos = vehiculoDao.getByUsuario(usuarioId)
            val idsVehiculos = vehiculos.map { it.id }.toSet()

            fun <T> juntar(
                nombre: String,
                locales: List<T>,
                idDe: (T) -> String,
                aMapa: (T) -> Map<String, Any?>,
                guardarLocal: suspend (id: String, datos: Map<String, Any?>, local: T?) -> Unit,
                borrarLocal: suspend (String) -> Unit
            ): suspend () -> Unit = {
                val nube = remotos.getValue(nombre).filter { (it.value["vehiculoId"] as? String) in idsVehiculos }
                val porId = locales.associateBy(idDe)
                nube.forEach { (id, datos) -> guardarLocal(id, datos, porId[id]) }
                locales.filter { idDe(it) !in nube }.forEach { local ->
                    if (primeraVez) subir += base.collection(nombre).document(idDe(local)) to aMapa(local)
                    else borrarLocal(idDe(local))
                }
            }

            val mDao = database.maintenanceDao()
            val gDao = database.expenseDao()
            val rDao = database.reminderDao()
            val dDao = database.documentoDao()
            listOf(
                juntar(
                    MapeoNube.MANTENIMIENTOS, vehiculos.flatMap { mDao.getByVehiculo(it.id) }, { it.id }, MapeoNube::aMapa,
                    { id, datos, local -> mDao.upsert(MapeoNube.mantenimiento(id, datos, local?.foto)) }, mDao::deleteById
                ),
                juntar(
                    MapeoNube.GASTOS, vehiculos.flatMap { gDao.getByVehiculo(it.id) }, { it.id }, MapeoNube::aMapa,
                    { id, datos, local -> gDao.upsert(MapeoNube.gasto(id, datos, local?.foto)) }, gDao::deleteById
                ),
                juntar(
                    MapeoNube.RECORDATORIOS, vehiculos.flatMap { rDao.getByVehiculo(it.id) }, { it.id }, MapeoNube::aMapa,
                    { id, datos, _ ->
                        val recordatorio = MapeoNube.recordatorio(id, datos)
                        rDao.upsert(recordatorio)
                        reminderScheduler.schedule(recordatorio.toModel())
                    },
                    { id -> rDao.deleteById(id); reminderScheduler.cancel(id) }
                ),
                juntar(
                    MapeoNube.DOCUMENTOS, vehiculos.flatMap { dDao.getByVehiculo(it.id) }, { it.id }, MapeoNube::aMapa,
                    { id, datos, local ->
                        val documento = MapeoNube.documento(id, datos, local?.foto)
                        dDao.upsert(documento)
                        vehiculos.firstOrNull { it.id == documento.vehiculoId }?.let { v ->
                            documentoScheduler.schedule(documento.toModel(), "${v.marca} ${v.modelo}")
                        }
                    },
                    { id -> dDao.deleteById(id); documentoScheduler.cancel(id) }
                )
            ).forEach { it() }

            subir.chunked(LIMITE_LOTE).forEach { grupo ->
                firestore.batch().apply { grupo.forEach { (ref, datos) -> set(ref, datos) } }.commit().await()
            }
            preferencias.edit { putBoolean(claveSubida, true) }
        }
    }

    private companion object {
        /** Firestore acepta hasta 500 operaciones por lote. */
        const val LIMITE_LOTE = 400
    }
}
