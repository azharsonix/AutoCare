package com.haydra.autocare.data.repository

import com.haydra.autocare.data.model.Reminder
import java.time.LocalDate

/**
 * Contrato de administración de recordatorios (RF15-RF16, RF18). La app usa
 * [RoomReminderRepository] y las pruebas [FakeReminderRepository]; en el Sprint 6 se añadirá
 * una implementación respaldada por la tabla "recordatorios" de Supabase
 * (AUTOCARE.txt, sección 32).
 */
interface ReminderRepository {
    suspend fun getReminders(vehiculoId: String): Result<List<Reminder>>
    suspend fun insertReminder(
        vehiculoId: String,
        titulo: String,
        fecha: LocalDate,
        kilometraje: Double?
    ): Result<Reminder>
    suspend fun updateReminder(reminder: Reminder): Result<Reminder>
    suspend fun setCompletado(id: String, completado: Boolean): Result<Unit>
    suspend fun deleteReminder(id: String): Result<Unit>
}
