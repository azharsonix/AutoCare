package com.haydra.autocare.data.repository

import com.haydra.autocare.data.DemoData
import com.haydra.autocare.data.model.Vehicle
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Implementación en memoria de [VehicleRepository], usada por las pruebas unitarias (la app
 * usa la versión Room, ver AppContainer). Antes de Room era la única y servía
 * mientras no existiera un proyecto
 * de Supabase. Guarda los vehículos en memoria, asociados al usuarioId que los
 * creó. Se precargan dos vehículos de ejemplo para la cuenta demo (ver [DemoData]).
 */
class FakeVehicleRepository : VehicleRepository {

    private val vehiculosPorId = mutableMapOf<String, Vehicle>()

    init {
        val vehiculo1 = Vehicle(
            id = DemoData.VEHICLE_1_ID,
            usuarioId = DemoData.USER_ID,
            marca = "Toyota",
            modelo = "Corolla",
            anio = 2022,
            color = "Rojo",
            placas = "ABC-123",
            kilometraje = 15000.0
        )
        val vehiculo2 = Vehicle(
            id = DemoData.VEHICLE_2_ID,
            usuarioId = DemoData.USER_ID,
            marca = "Honda",
            modelo = "CR-V",
            anio = 2020,
            color = "Gris",
            placas = "XYZ-789",
            kilometraje = 42000.0
        )
        vehiculosPorId[vehiculo1.id] = vehiculo1
        vehiculosPorId[vehiculo2.id] = vehiculo2
    }

    override suspend fun getVehicles(usuarioId: String): Result<List<Vehicle>> {
        delay(200)
        val vehiculos = vehiculosPorId.values
            .filter { it.usuarioId == usuarioId }
            .sortedBy { it.marca }
        return Result.success(vehiculos)
    }

    override suspend fun getVehicle(id: String): Result<Vehicle> {
        delay(150)
        val vehiculo = vehiculosPorId[id]
            ?: return Result.failure(NoSuchElementException("El vehículo no existe"))
        return Result.success(vehiculo)
    }

    override suspend fun insertVehicle(
        usuarioId: String,
        marca: String,
        modelo: String,
        anio: Int,
        color: String,
        placas: String,
        kilometraje: Double
    ): Result<Vehicle> {
        delay(300)
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
        vehiculosPorId[vehiculo.id] = vehiculo
        return Result.success(vehiculo)
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Vehicle> {
        delay(300)
        if (!vehiculosPorId.containsKey(vehicle.id)) {
            return Result.failure(NoSuchElementException("El vehículo no existe"))
        }
        vehiculosPorId[vehicle.id] = vehicle
        return Result.success(vehicle)
    }

    override suspend fun deleteVehicle(id: String): Result<Unit> {
        delay(200)
        vehiculosPorId.remove(id)
        return Result.success(Unit)
    }
}
