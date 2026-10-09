package com.haydra.autocare.data.repository

import com.haydra.autocare.data.model.Vehicle

/**
 * Contrato de administración de vehículos (RF04-RF07, RF17). La app usa
 * [RoomVehicleRepository] y las pruebas [FakeVehicleRepository]; en el Sprint 6 se añadirá
 * una implementación respaldada por la colección "vehiculos" de Cloud Firestore (AUTOCARE.txt, sección 32).
 */
interface VehicleRepository {
    suspend fun getVehicles(usuarioId: String): Result<List<Vehicle>>
    suspend fun getVehicle(id: String): Result<Vehicle>
    suspend fun insertVehicle(
        usuarioId: String,
        marca: String,
        modelo: String,
        anio: Int,
        color: String,
        placas: String,
        kilometraje: Double
    ): Result<Vehicle>
    suspend fun updateVehicle(vehicle: Vehicle): Result<Vehicle>
    suspend fun deleteVehicle(id: String): Result<Unit>
}
