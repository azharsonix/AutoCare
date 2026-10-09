package com.haydra.autocare.data.repository

import com.haydra.autocare.data.local.db.VehicleDao
import com.haydra.autocare.data.local.db.toEntity
import com.haydra.autocare.data.local.db.toModel
import com.haydra.autocare.data.model.Vehicle
import java.time.LocalDate
import java.util.UUID

/**
 * Implementación de [VehicleRepository] sobre Room. Al eliminar un vehículo,
 * la base borra en cascada sus mantenimientos, gastos y recordatorios.
 */
class RoomVehicleRepository(private val vehicleDao: VehicleDao) : VehicleRepository {

    override suspend fun getVehicles(usuarioId: String): Result<List<Vehicle>> = runCatching {
        vehicleDao.getByUsuario(usuarioId).map { it.toModel() }
    }

    override suspend fun getVehicle(id: String): Result<Vehicle> = runCatching {
        vehicleDao.getById(id)?.toModel() ?: throw NoSuchElementException("El vehículo no existe")
    }

    override suspend fun insertVehicle(
        usuarioId: String,
        marca: String,
        modelo: String,
        anio: Int,
        color: String,
        placas: String,
        kilometraje: Double
    ): Result<Vehicle> = runCatching {
        val vehiculo = Vehicle(
            id = UUID.randomUUID().toString(),
            usuarioId = usuarioId,
            marca = marca.trim(),
            modelo = modelo.trim(),
            anio = anio,
            color = color.trim(),
            placas = placas.trim().uppercase(),
            kilometraje = kilometraje,
            kmActualizado = LocalDate.now()
        )
        vehicleDao.insert(vehiculo.toEntity())
        vehiculo
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Vehicle> = runCatching {
        if (vehicleDao.update(vehicle.toEntity()) == 0) throw NoSuchElementException("El vehículo no existe")
        vehicle
    }

    override suspend fun deleteVehicle(id: String): Result<Unit> = runCatching {
        vehicleDao.deleteById(id)
    }
}
