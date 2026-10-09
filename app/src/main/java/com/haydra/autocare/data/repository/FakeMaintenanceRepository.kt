package com.haydra.autocare.data.repository

import com.haydra.autocare.data.DemoData
import com.haydra.autocare.data.model.Maintenance
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Implementación en memoria de [MaintenanceRepository], usada por las pruebas unitarias (la app
 * usa la versión Room, ver AppContainer). Antes de Room era la única y servía
 * mientras no existiera un
 * proyecto de Supabase. Guarda los mantenimientos en memoria, asociados al
 * vehículo al que pertenecen. Se precargan un par de ejemplos para el primer
 * vehículo de la cuenta demo (ver [DemoData]).
 */
class FakeMaintenanceRepository : MaintenanceRepository {

    private val mantenimientosPorId = mutableMapOf<String, Maintenance>()

    init {
        val hoy = LocalDate.now()
        val ejemplo1 = Maintenance(
            id = UUID.randomUUID().toString(),
            vehiculoId = DemoData.VEHICLE_1_ID,
            tipo = "Cambio de aceite",
            fecha = hoy.minusMonths(2),
            kilometraje = 13000.0,
            costo = 850.0,
            descripcion = "Aceite sintético 5W-30 y filtro"
        )
        val ejemplo2 = Maintenance(
            id = UUID.randomUUID().toString(),
            vehiculoId = DemoData.VEHICLE_1_ID,
            tipo = "Rotación de llantas",
            fecha = hoy.minusMonths(1),
            kilometraje = 14500.0,
            costo = 300.0,
            descripcion = ""
        )
        mantenimientosPorId[ejemplo1.id] = ejemplo1
        mantenimientosPorId[ejemplo2.id] = ejemplo2
    }

    override suspend fun getMaintenances(vehiculoId: String): Result<List<Maintenance>> {
        delay(200)
        val mantenimientos = mantenimientosPorId.values
            .filter { it.vehiculoId == vehiculoId }
            .sortedByDescending { it.fecha }
        return Result.success(mantenimientos)
    }

    override suspend fun insertMaintenance(
        vehiculoId: String,
        tipo: String,
        fecha: LocalDate,
        kilometraje: Double,
        costo: Double,
        descripcion: String,
        foto: String?
    ): Result<Maintenance> {
        delay(300)
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
        mantenimientosPorId[mantenimiento.id] = mantenimiento
        return Result.success(mantenimiento)
    }

    override suspend fun updateMaintenance(maintenance: Maintenance): Result<Maintenance> {
        delay(300)
        if (!mantenimientosPorId.containsKey(maintenance.id)) {
            return Result.failure(NoSuchElementException("El mantenimiento no existe"))
        }
        mantenimientosPorId[maintenance.id] = maintenance
        return Result.success(maintenance)
    }

    override suspend fun deleteMaintenance(id: String): Result<Unit> {
        delay(200)
        mantenimientosPorId.remove(id)
        return Result.success(Unit)
    }
}
