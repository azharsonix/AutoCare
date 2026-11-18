package com.haydra.autocare.data.nube

import com.haydra.autocare.data.local.db.DocumentoEntity
import com.haydra.autocare.data.local.db.ExpenseEntity
import com.haydra.autocare.data.local.db.MaintenanceEntity
import com.haydra.autocare.data.local.db.ReminderEntity
import com.haydra.autocare.data.local.db.VehicleEntity
import java.time.LocalDate

/**
 * Conversión entre las filas de Room y los documentos de Cloud Firestore.
 *
 * Las fechas viajan como texto "aaaa-mm-dd" y los números como Double. Las fotos no se suben:
 * son archivos del teléfono, así que el campo `foto` se queda solo en Room.
 */
object MapeoNube {

    const val VEHICULOS = "vehiculos"
    const val MANTENIMIENTOS = "mantenimientos"
    const val GASTOS = "gastos"
    const val RECORDATORIOS = "recordatorios"
    const val DOCUMENTOS = "documentos"

    /** Colecciones que cuelgan de un vehículo (se borran junto con él). */
    val DE_VEHICULO = listOf(MANTENIMIENTOS, GASTOS, RECORDATORIOS, DOCUMENTOS)

    fun aMapa(v: VehicleEntity): Map<String, Any?> = mapOf(
        "marca" to v.marca,
        "modelo" to v.modelo,
        "anio" to v.anio,
        "color" to v.color,
        "placas" to v.placas,
        "kilometraje" to v.kilometraje,
        "kmActualizado" to v.kmActualizado?.toString()
    )

    fun aMapa(m: MaintenanceEntity): Map<String, Any?> = mapOf(
        "vehiculoId" to m.vehiculoId,
        "tipo" to m.tipo,
        "fecha" to m.fecha.toString(),
        "kilometraje" to m.kilometraje,
        "costo" to m.costo,
        "descripcion" to m.descripcion
    )

    fun aMapa(g: ExpenseEntity): Map<String, Any?> = mapOf(
        "vehiculoId" to g.vehiculoId,
        "categoria" to g.categoria,
        "monto" to g.monto,
        "fecha" to g.fecha.toString(),
        "descripcion" to g.descripcion,
        "litros" to g.litros,
        "kilometraje" to g.kilometraje
    )

    fun aMapa(r: ReminderEntity): Map<String, Any?> = mapOf(
        "vehiculoId" to r.vehiculoId,
        "titulo" to r.titulo,
        "fecha" to r.fecha.toString(),
        "kilometraje" to r.kilometraje,
        "completado" to r.completado
    )

    fun aMapa(d: DocumentoEntity): Map<String, Any?> = mapOf(
        "vehiculoId" to d.vehiculoId,
        "tipo" to d.tipo,
        "vence" to d.vence?.toString(),
        "notas" to d.notas
    )

    fun vehiculo(id: String, usuarioId: String, m: Map<String, Any?>) = VehicleEntity(
        id = id,
        usuarioId = usuarioId,
        marca = m.texto("marca"),
        modelo = m.texto("modelo"),
        anio = m.numero("anio")?.toInt() ?: 0,
        color = m.texto("color"),
        placas = m.texto("placas"),
        kilometraje = m.numero("kilometraje") ?: 0.0,
        kmActualizado = m.fecha("kmActualizado")
    )

    fun mantenimiento(id: String, m: Map<String, Any?>, foto: String?) = MaintenanceEntity(
        id = id,
        vehiculoId = m.texto("vehiculoId"),
        tipo = m.texto("tipo"),
        fecha = m.fecha("fecha") ?: LocalDate.now(),
        kilometraje = m.numero("kilometraje") ?: 0.0,
        costo = m.numero("costo") ?: 0.0,
        descripcion = m.texto("descripcion"),
        foto = foto
    )

    fun gasto(id: String, m: Map<String, Any?>, foto: String?) = ExpenseEntity(
        id = id,
        vehiculoId = m.texto("vehiculoId"),
        categoria = m.texto("categoria"),
        monto = m.numero("monto") ?: 0.0,
        fecha = m.fecha("fecha") ?: LocalDate.now(),
        descripcion = m.texto("descripcion"),
        litros = m.numero("litros"),
        kilometraje = m.numero("kilometraje"),
        foto = foto
    )

    fun recordatorio(id: String, m: Map<String, Any?>) = ReminderEntity(
        id = id,
        vehiculoId = m.texto("vehiculoId"),
        titulo = m.texto("titulo"),
        fecha = m.fecha("fecha") ?: LocalDate.now(),
        kilometraje = m.numero("kilometraje"),
        completado = m["completado"] as? Boolean ?: false
    )

    fun documento(id: String, m: Map<String, Any?>, foto: String?) = DocumentoEntity(
        id = id,
        vehiculoId = m.texto("vehiculoId"),
        tipo = m.texto("tipo"),
        vence = m.fecha("vence"),
        foto = foto,
        notas = m.texto("notas")
    )

    private fun Map<String, Any?>.texto(campo: String) = this[campo] as? String ?: ""

    // Firestore devuelve los enteros como Long y los decimales como Double.
    private fun Map<String, Any?>.numero(campo: String) = (this[campo] as? Number)?.toDouble()

    private fun Map<String, Any?>.fecha(campo: String) =
        (this[campo] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
