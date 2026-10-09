package com.haydra.autocare.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.haydra.autocare.AutoCareApplication
import com.haydra.autocare.ui.vehicles.PESTANA_DOCUMENTOS

/**
 * Muestra la notificación de un recordatorio (HU08, RF15-RF16) o del
 * vencimiento de un documento cuando llega su fecha. Se programa con
 * [ReminderScheduler] o [DocumentoScheduler] usando WorkManager.
 */
class ReminderNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = (applicationContext as AutoCareApplication).container.database

        // El recordatorio o documento pudo completarse o eliminarse (p. ej. al
        // borrar su vehículo, que elimina en cascada) sin que se cancelara este trabajo.
        var vehiculoId: String? = null
        var pestana: String? = null
        inputData.getString(KEY_REMINDER_ID)?.let { id ->
            val recordatorio = database.reminderDao().getById(id)
            if (recordatorio == null || recordatorio.completado) return Result.success()
            vehiculoId = recordatorio.vehiculoId
        }
        inputData.getString(KEY_DOCUMENTO_ID)?.let { id ->
            val documento = database.documentoDao().getById(id) ?: return Result.success()
            vehiculoId = documento.vehiculoId
            pestana = PESTANA_DOCUMENTOS
        }

        notificar(
            context = applicationContext,
            id = inputData.getInt(KEY_NOTIFICATION_ID, 0),
            canal = Canales.RECORDATORIOS,
            titulo = inputData.getString(KEY_ENCABEZADO) ?: "Recordatorio de AutoCare",
            texto = inputData.getString(KEY_TITULO) ?: "Recordatorio de AutoCare",
            vehiculoId = vehiculoId,
            pestana = pestana
        )
        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminderId"
        const val KEY_DOCUMENTO_ID = "documentoId"
        const val KEY_ENCABEZADO = "encabezado"
        const val KEY_TITULO = "titulo"
        const val KEY_NOTIFICATION_ID = "notificationId"
    }
}
