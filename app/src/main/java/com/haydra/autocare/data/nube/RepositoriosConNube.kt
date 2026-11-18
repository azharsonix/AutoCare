package com.haydra.autocare.data.nube

import com.haydra.autocare.data.local.db.ReminderDao
import com.haydra.autocare.data.local.db.toEntity
import com.haydra.autocare.data.model.Documento
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.data.repository.DocumentoRepository
import com.haydra.autocare.data.repository.ExpenseRepository
import com.haydra.autocare.data.repository.MaintenanceRepository
import com.haydra.autocare.data.repository.ReminderRepository
import com.haydra.autocare.data.repository.VehicleRepository
import java.time.LocalDate

/*
 * Patrón Decorador: cada clase envuelve al repositorio de Room, deja que guarde en el teléfono
 * y, si salió bien, manda el mismo cambio a Cloud Firestore con [SincronizadorNube].
 * Los ViewModels no cambian: siguen usando las mismas interfaces.
 */

class VehicleRepositoryConNube(
    private val local: VehicleRepository,
    private val nube: SincronizadorNube
) : VehicleRepository by local {

    override suspend fun insertVehicle(
        usuarioId: String, marca: String, modelo: String, anio: Int, color: String, placas: String, kilometraje: Double
    ): Result<Vehicle> = local.insertVehicle(usuarioId, marca, modelo, anio, color, placas, kilometraje)
        .onSuccess(::subir)

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Vehicle> =
        local.updateVehicle(vehicle).onSuccess(::subir)

    override suspend fun deleteVehicle(id: String): Result<Unit> {
        // Room borra los registros en cascada; se toman sus ids antes para borrarlos también de la nube.
        val registros = nube.registrosDeVehiculo(id)
        return local.deleteVehicle(id).onSuccess { nube.borrarVehiculo(id, registros) }
    }

    private fun subir(v: Vehicle) = nube.guardar(MapeoNube.VEHICULOS, v.id, MapeoNube.aMapa(v.toEntity()))
}

class MaintenanceRepositoryConNube(
    private val local: MaintenanceRepository,
    private val nube: SincronizadorNube
) : MaintenanceRepository by local {

    override suspend fun insertMaintenance(
        vehiculoId: String, tipo: String, fecha: LocalDate, kilometraje: Double, costo: Double, descripcion: String, foto: String?
    ): Result<Maintenance> = local.insertMaintenance(vehiculoId, tipo, fecha, kilometraje, costo, descripcion, foto)
        .onSuccess(::subir)

    override suspend fun updateMaintenance(maintenance: Maintenance): Result<Maintenance> =
        local.updateMaintenance(maintenance).onSuccess(::subir)

    override suspend fun deleteMaintenance(id: String): Result<Unit> =
        local.deleteMaintenance(id).onSuccess { nube.borrar(MapeoNube.MANTENIMIENTOS, id) }

    private fun subir(m: Maintenance) = nube.guardar(MapeoNube.MANTENIMIENTOS, m.id, MapeoNube.aMapa(m.toEntity()))
}

class ExpenseRepositoryConNube(
    private val local: ExpenseRepository,
    private val nube: SincronizadorNube
) : ExpenseRepository by local {

    override suspend fun insertExpense(
        vehiculoId: String, categoria: String, monto: Double, fecha: LocalDate, descripcion: String,
        litros: Double?, kilometraje: Double?, foto: String?
    ): Result<Expense> = local.insertExpense(vehiculoId, categoria, monto, fecha, descripcion, litros, kilometraje, foto)
        .onSuccess(::subir)

    override suspend fun updateExpense(expense: Expense): Result<Expense> =
        local.updateExpense(expense).onSuccess(::subir)

    override suspend fun deleteExpense(id: String): Result<Unit> =
        local.deleteExpense(id).onSuccess { nube.borrar(MapeoNube.GASTOS, id) }

    private fun subir(g: Expense) = nube.guardar(MapeoNube.GASTOS, g.id, MapeoNube.aMapa(g.toEntity()))
}

class ReminderRepositoryConNube(
    private val local: ReminderRepository,
    private val nube: SincronizadorNube,
    private val reminderDao: ReminderDao
) : ReminderRepository by local {

    override suspend fun insertReminder(
        vehiculoId: String, titulo: String, fecha: LocalDate, kilometraje: Double?
    ): Result<Reminder> = local.insertReminder(vehiculoId, titulo, fecha, kilometraje).onSuccess(::subir)

    override suspend fun updateReminder(reminder: Reminder): Result<Reminder> =
        local.updateReminder(reminder).onSuccess(::subir)

    override suspend fun setCompletado(id: String, completado: Boolean): Result<Unit> =
        local.setCompletado(id, completado).onSuccess {
            reminderDao.getById(id)?.let { nube.guardar(MapeoNube.RECORDATORIOS, id, MapeoNube.aMapa(it)) }
        }

    override suspend fun deleteReminder(id: String): Result<Unit> =
        local.deleteReminder(id).onSuccess { nube.borrar(MapeoNube.RECORDATORIOS, id) }

    private fun subir(r: Reminder) = nube.guardar(MapeoNube.RECORDATORIOS, r.id, MapeoNube.aMapa(r.toEntity()))
}

class DocumentoRepositoryConNube(
    private val local: DocumentoRepository,
    private val nube: SincronizadorNube
) : DocumentoRepository by local {

    override suspend fun insertDocumento(
        vehiculoId: String, tipo: String, vence: LocalDate?, foto: String?, notas: String
    ): Result<Documento> = local.insertDocumento(vehiculoId, tipo, vence, foto, notas).onSuccess(::subir)

    override suspend fun updateDocumento(documento: Documento): Result<Documento> =
        local.updateDocumento(documento).onSuccess(::subir)

    override suspend fun deleteDocumento(id: String): Result<Unit> =
        local.deleteDocumento(id).onSuccess { nube.borrar(MapeoNube.DOCUMENTOS, id) }

    private fun subir(d: Documento) = nube.guardar(MapeoNube.DOCUMENTOS, d.id, MapeoNube.aMapa(d.toEntity()))
}
