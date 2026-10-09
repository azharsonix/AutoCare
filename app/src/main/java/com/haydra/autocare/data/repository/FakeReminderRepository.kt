package com.haydra.autocare.data.repository

import com.haydra.autocare.data.DemoData
import com.haydra.autocare.data.model.Reminder
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Implementación en memoria de [ReminderRepository], usada por las pruebas unitarias (la app
 * usa la versión Room, ver AppContainer). Antes de Room era la única y servía
 * mientras no existiera un
 * proyecto de Supabase. Guarda los recordatorios en memoria, asociados al
 * vehículo al que pertenecen. Se precargan un par de ejemplos para el primer
 * vehículo de la cuenta demo (ver [DemoData]).
 */
class FakeReminderRepository : ReminderRepository {

    private val recordatoriosPorId = mutableMapOf<String, Reminder>()

    init {
        val hoy = LocalDate.now()
        val ejemplo1 = Reminder(
            id = UUID.randomUUID().toString(),
            vehiculoId = DemoData.VEHICLE_1_ID,
            titulo = "Cambio de aceite",
            fecha = hoy.plusMonths(1),
            kilometraje = 18000.0,
            completado = false
        )
        val ejemplo2 = Reminder(
            id = UUID.randomUUID().toString(),
            vehiculoId = DemoData.VEHICLE_1_ID,
            titulo = "Verificación vehicular",
            fecha = hoy.plusMonths(4),
            kilometraje = null,
            completado = false
        )
        recordatoriosPorId[ejemplo1.id] = ejemplo1
        recordatoriosPorId[ejemplo2.id] = ejemplo2
    }

    override suspend fun getReminders(vehiculoId: String): Result<List<Reminder>> {
        delay(200)
        val recordatorios = recordatoriosPorId.values
            .filter { it.vehiculoId == vehiculoId }
            .sortedWith(compareBy({ it.completado }, { it.fecha }))
        return Result.success(recordatorios)
    }

    override suspend fun insertReminder(
        vehiculoId: String,
        titulo: String,
        fecha: LocalDate,
        kilometraje: Double?
    ): Result<Reminder> {
        delay(300)
        val recordatorio = Reminder(
            id = UUID.randomUUID().toString(),
            vehiculoId = vehiculoId,
            titulo = titulo.trim(),
            fecha = fecha,
            kilometraje = kilometraje,
            completado = false
        )
        recordatoriosPorId[recordatorio.id] = recordatorio
        return Result.success(recordatorio)
    }

    override suspend fun updateReminder(reminder: Reminder): Result<Reminder> {
        delay(300)
        if (!recordatoriosPorId.containsKey(reminder.id)) {
            return Result.failure(NoSuchElementException("El recordatorio no existe"))
        }
        recordatoriosPorId[reminder.id] = reminder
        return Result.success(reminder)
    }

    override suspend fun setCompletado(id: String, completado: Boolean): Result<Unit> {
        delay(150)
        val actual = recordatoriosPorId[id]
            ?: return Result.failure(NoSuchElementException("El recordatorio no existe"))
        recordatoriosPorId[id] = actual.copy(completado = completado)
        return Result.success(Unit)
    }

    override suspend fun deleteReminder(id: String): Result<Unit> {
        delay(200)
        recordatoriosPorId.remove(id)
        return Result.success(Unit)
    }
}
