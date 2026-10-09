package com.haydra.autocare.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.haydra.autocare.data.model.Reminder
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Programa y cancela las notificaciones locales de los recordatorios
 * (HU08, RF15-RF16) usando WorkManager, identificando cada trabajo por el id
 * del recordatorio para poder reemplazarlo o cancelarlo al editar, completar
 * o eliminar.
 */
class ReminderScheduler(private val context: Context) {

    fun schedule(reminder: Reminder) {
        if (reminder.completado) {
            cancel(reminder.id)
            return
        }

        val disparoMillis = reminder.fecha
            .atTime(HORA_AVISO)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val demoraMillis = disparoMillis - System.currentTimeMillis()
        if (demoraMillis <= 0) {
            cancel(reminder.id)
            return
        }

        val datos = workDataOf(
            ReminderNotificationWorker.KEY_REMINDER_ID to reminder.id,
            ReminderNotificationWorker.KEY_TITULO to reminder.titulo,
            ReminderNotificationWorker.KEY_NOTIFICATION_ID to reminder.id.hashCode()
        )
        val solicitud = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(demoraMillis, TimeUnit.MILLISECONDS)
            .setInputData(datos)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(reminder.id, ExistingWorkPolicy.REPLACE, solicitud)
    }

    /** Notifica ya (p. ej. al alcanzar el kilometraje objetivo), sin tocar el aviso por fecha. */
    fun notifyNow(reminder: Reminder) {
        if (reminder.completado) return
        val datos = workDataOf(
            ReminderNotificationWorker.KEY_REMINDER_ID to reminder.id,
            ReminderNotificationWorker.KEY_TITULO to "${reminder.titulo}: ya llegaste al kilometraje indicado",
            ReminderNotificationWorker.KEY_NOTIFICATION_ID to "${reminder.id}-km".hashCode()
        )
        val solicitud = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInputData(datos)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("${reminder.id}-km", ExistingWorkPolicy.REPLACE, solicitud)
    }

    fun cancel(reminderId: String) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(reminderId)
        workManager.cancelUniqueWork("$reminderId-km")
    }

}

/** Hora a la que llegan los avisos programados por fecha. */
internal val HORA_AVISO: LocalTime = LocalTime.of(9, 0)
