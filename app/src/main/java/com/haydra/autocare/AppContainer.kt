package com.haydra.autocare

import android.content.Context
import com.haydra.autocare.data.export.ExportadorReporte
import com.haydra.autocare.data.local.AlmacenFotos
import com.haydra.autocare.data.local.PlanStore
import com.haydra.autocare.data.local.SessionStore
import com.haydra.autocare.data.local.db.AutoCareDatabase
import com.haydra.autocare.data.plan.PagosDemo
import com.haydra.autocare.data.plan.PlanRepository
import com.haydra.autocare.data.plan.ProveedorPagos
import com.haydra.autocare.data.repository.AuthRepository
import com.haydra.autocare.data.repository.DocumentoRepository
import com.haydra.autocare.data.repository.ExpenseRepository
import com.haydra.autocare.data.repository.MaintenanceRepository
import com.haydra.autocare.data.repository.ReminderRepository
import com.haydra.autocare.data.repository.RoomAuthRepository
import com.haydra.autocare.data.repository.RoomDocumentoRepository
import com.haydra.autocare.data.repository.RoomExpenseRepository
import com.haydra.autocare.data.repository.RoomMaintenanceRepository
import com.haydra.autocare.data.repository.RoomReminderRepository
import com.haydra.autocare.data.repository.RoomVehicleRepository
import com.haydra.autocare.data.repository.VehicleRepository
import com.haydra.autocare.notifications.DocumentoScheduler
import com.haydra.autocare.notifications.ReminderScheduler
import com.haydra.autocare.ui.common.Mensajes

/**
 * Inyección de dependencias manual: crea una sola vez la base de datos y los
 * repositorios que usan los ViewModels. Para cambiar a Supabase basta con
 * sustituir aquí las implementaciones.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AutoCareDatabase by lazy { AutoCareDatabase.create(appContext) }

    val authRepository: AuthRepository by lazy { RoomAuthRepository(database.userDao()) }
    val vehicleRepository: VehicleRepository by lazy { RoomVehicleRepository(database.vehicleDao()) }
    val maintenanceRepository: MaintenanceRepository by lazy { RoomMaintenanceRepository(database.maintenanceDao()) }
    val expenseRepository: ExpenseRepository by lazy { RoomExpenseRepository(database.expenseDao()) }
    val reminderRepository: ReminderRepository by lazy { RoomReminderRepository(database.reminderDao()) }
    val documentoRepository: DocumentoRepository by lazy { RoomDocumentoRepository(database.documentoDao()) }

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
