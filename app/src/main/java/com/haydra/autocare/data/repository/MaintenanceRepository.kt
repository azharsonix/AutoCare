package com.haydra.autocare.data.repository

import com.haydra.autocare.data.model.Maintenance
import java.time.LocalDate

/**
 * Contrato de administración de mantenimientos (RF08-RF10, RF18). La app usa
 * [RoomMaintenanceRepository] y las pruebas [FakeMaintenanceRepository]; en el Sprint 6 se
 * añadirá una implementación respaldada por la tabla "mantenimientos" de Supabase
 * (AUTOCARE.txt, sección 32).
 */
interface MaintenanceRepository {
    suspend fun getMaintenances(vehiculoId: String): Result<List<Maintenance>>
    suspend fun insertMaintenance(
        vehiculoId: String,
        tipo: String,
        fecha: LocalDate,
        kilometraje: Double,
        costo: Double,
        descripcion: String,
        foto: String? = null
    ): Result<Maintenance>
    suspend fun updateMaintenance(maintenance: Maintenance): Result<Maintenance>
    suspend fun deleteMaintenance(id: String): Result<Unit>
}
