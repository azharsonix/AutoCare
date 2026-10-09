package com.haydra.autocare.data.repository

import com.haydra.autocare.data.local.db.ReminderDao
import com.haydra.autocare.data.local.db.toEntity
import com.haydra.autocare.data.local.db.toModel
import com.haydra.autocare.data.model.Reminder
import java.time.LocalDate
import java.util.UUID

/** Implementación de [ReminderRepository] sobre Room. */
class RoomReminderRepository(private val reminderDao: ReminderDao) : ReminderRepository {

    override suspend fun getReminders(vehiculoId: String): Result<List<Reminder>> = runCatching {
        reminderDao.getByVehiculo(vehiculoId).map { it.toModel() }
    }

    override suspend fun insertReminder(
        vehiculoId: String,
        titulo: String,
        fecha: LocalDate,
        kilometraje: Double?
    ): Result<Reminder> = runCatching {
        val recordatorio = Reminder(
            id = UUID.randomUUID().toString(),
            vehiculoId = vehiculoId,
            titulo = titulo.trim(),
            fecha = fecha,
            kilometraje = kilometraje,
            completado = false
        )
        reminderDao.insert(recordatorio.toEntity())
        recordatorio
    }

    override suspend fun updateReminder(reminder: Reminder): Result<Reminder> = runCatching {
        if (reminderDao.update(reminder.toEntity()) == 0) {
            throw NoSuchElementException("El recordatorio no existe")
        }
        reminder
    }

    override suspend fun setCompletado(id: String, completado: Boolean): Result<Unit> = runCatching {
        if (reminderDao.setCompletado(id, completado) == 0) {
            throw NoSuchElementException("El recordatorio no existe")
        }
    }

    override suspend fun deleteReminder(id: String): Result<Unit> = runCatching {
        reminderDao.deleteById(id)
    }
}
