package com.haydra.autocare.data.repository

import com.haydra.autocare.data.local.db.MaintenanceDao
import com.haydra.autocare.data.local.db.toEntity
import com.haydra.autocare.data.local.db.toModel
import com.haydra.autocare.data.model.Maintenance
import java.time.LocalDate
import java.util.UUID

/** Implementación de [MaintenanceRepository] sobre Room. */
class RoomMaintenanceRepository(private val maintenanceDao: MaintenanceDao) : MaintenanceRepository {

    override suspend fun getMaintenances(vehiculoId: String): Result<List<Maintenance>> = runCatching {
        maintenanceDao.getByVehiculo(vehiculoId).map { it.toModel() }
    }

    override suspend fun insertMaintenance(
        vehiculoId: String,
        tipo: String,
        fecha: LocalDate,
        kilometraje: Double,
        costo: Double,
        descripcion: String,
        foto: String?
    ): Result<Maintenance> = runCatching {
        val mantenimiento = Maintenance(
            id = UUID.randomUUID().toString(),
            vehiculoId = vehiculoId,
            tipo = tipo.trim(),
            fecha = fecha,
            kilometraje = kilometraje,
            costo = costo,
            descripcion = descripcion.trim(),
            foto = foto
        )
        maintenanceDao.insert(mantenimiento.toEntity())
        mantenimiento
    }

    override suspend fun updateMaintenance(maintenance: Maintenance): Result<Maintenance> = runCatching {
        if (maintenanceDao.update(maintenance.toEntity()) == 0) {
            throw NoSuchElementException("El mantenimiento no existe")
        }
        maintenance
    }

    override suspend fun deleteMaintenance(id: String): Result<Unit> = runCatching {
        maintenanceDao.deleteById(id)
    }
}
