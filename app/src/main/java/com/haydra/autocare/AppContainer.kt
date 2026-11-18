package com.haydra.autocare

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.haydra.autocare.data.catalogo.CatalogoVehiculos
import com.haydra.autocare.data.catalogo.CatalogoVpic
import com.haydra.autocare.data.export.ExportadorReporte
import com.haydra.autocare.data.local.AlmacenFotos
import com.haydra.autocare.data.local.PlanStore
import com.haydra.autocare.data.local.SessionStore
import com.haydra.autocare.data.local.db.AutoCareDatabase
import com.haydra.autocare.data.nube.AuthRepositoryConNube
import com.haydra.autocare.data.nube.DocumentoRepositoryConNube
import com.haydra.autocare.data.nube.ExpenseRepositoryConNube
import com.haydra.autocare.data.nube.MaintenanceRepositoryConNube
import com.haydra.autocare.data.nube.ReminderRepositoryConNube
import com.haydra.autocare.data.nube.SincronizadorNube
import com.haydra.autocare.data.nube.VehicleRepositoryConNube
import com.haydra.autocare.data.plan.PagosDemo
import com.haydra.autocare.data.plan.PlanRepository
import com.haydra.autocare.data.plan.ProveedorPagos
import com.haydra.autocare.data.repository.AuthRepository
import com.haydra.autocare.data.repository.DocumentoRepository
import com.haydra.autocare.data.repository.ExpenseRepository
import com.haydra.autocare.data.repository.MaintenanceRepository
import com.haydra.autocare.data.repository.ReminderRepository
import com.haydra.autocare.data.repository.FirebaseAuthRepository
import com.haydra.autocare.data.repository.RoomDocumentoRepository
import com.haydra.autocare.data.repository.RoomExpenseRepository
import com.haydra.autocare.data.repository.RoomMaintenanceRepository
import com.haydra.autocare.data.repository.RoomReminderRepository
import com.haydra.autocare.data.repository.RoomVehicleRepository
import com.haydra.autocare.data.repository.VehicleRepository
import com.haydra.autocare.notifications.DocumentoScheduler
import com.haydra.autocare.notifications.ReminderScheduler
import com.haydra.autocare.ui.common.Mensajes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Inyección de dependencias manual: crea una sola vez la base de datos y los
 * repositorios que usan los ViewModels. La autenticación va con Firebase.
 * Los datos se guardan primero en Room (funciona sin internet) y cada cambio
 * se respalda en Cloud Firestore por medio de los repositorios "ConNube".
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AutoCareDatabase by lazy { AutoCareDatabase.create(appContext) }

    /** Tareas que deben terminar aunque se cierre la pantalla que las inició. */
    private val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sincronizadorNube: SincronizadorNube by lazy {
        SincronizadorNube(
            appContext, FirebaseAuth.getInstance(), FirebaseFirestore.getInstance(), database,
            reminderScheduler, documentoScheduler
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryConNube(
            FirebaseAuthRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance(), database.userDao()),
            sincronizadorNube,
            alcanceApp,
            borrarFotos = almacenFotos::borrar
        )
    }
    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepositoryConNube(RoomVehicleRepository(database.vehicleDao()), sincronizadorNube)
    }
    val maintenanceRepository: MaintenanceRepository by lazy {
        MaintenanceRepositoryConNube(RoomMaintenanceRepository(database.maintenanceDao()), sincronizadorNube)
    }
    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryConNube(RoomExpenseRepository(database.expenseDao()), sincronizadorNube)
    }
    val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryConNube(RoomReminderRepository(database.reminderDao()), sincronizadorNube, database.reminderDao())
    }
    val documentoRepository: DocumentoRepository by lazy {
        DocumentoRepositoryConNube(RoomDocumentoRepository(database.documentoDao()), sincronizadorNube)
    }

    /** API de terceros vPIC (NHTSA): modelos por marca y año, y lectura del número de serie. */
    val catalogoVehiculos: CatalogoVehiculos by lazy { CatalogoVpic() }

    val sessionStore: SessionStore by lazy { SessionStore(appContext) }
    val planRepository: PlanRepository by lazy { PlanStore(appContext) }
    // Cambiar por la implementación de Google Play Billing al publicar la app.
    val proveedorPagos: ProveedorPagos = PagosDemo()
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }
    val documentoScheduler: DocumentoScheduler by lazy { DocumentoScheduler(appContext) }
    val almacenFotos: AlmacenFotos by lazy { AlmacenFotos(appContext) }
    val mensajes = Mensajes()
    val exportadorReporte: ExportadorReporte by lazy {
        ExportadorReporte(
            appContext, vehicleRepository, maintenanceRepository, expenseRepository, reminderRepository, documentoRepository
        )
    }

    /** Borra las fotos que ya no pertenecen a ningún gasto, servicio o documento. */
    suspend fun limpiarFotos() {
        val enUso = database.expenseDao().fotos() + database.maintenanceDao().fotos() + database.documentoDao().fotos()
        almacenFotos.limpiar(enUso.toSet())
    }
}
