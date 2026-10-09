package com.haydra.autocare.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.haydra.autocare.data.model.Documento
import com.haydra.autocare.ui.common.formatFecha
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Avisa antes de que venza un documento: [DIAS_ANTES] días antes (para
 * tener tiempo de renovarlo) y el mismo día. Cada aviso es un trabajo único
 * por documento, así editar o eliminar el documento lo reemplaza o cancela.
 */
class DocumentoScheduler(private val context: Context) {

    fun schedule(documento: Documento, vehiculo: String) {
        cancel(documento.id)
        val vence = documento.vence ?: return
        programar(
            nombre = "${documento.id}-antes",
            dia = vence.minusDays(DIAS_ANTES),
            documento = documento,
            texto = "${documento.tipo} de tu $vehiculo vence el ${formatFecha(vence)}. Renuévalo a tiempo."
        )
        programar(
            nombre = "${documento.id}-dia",
            dia = vence,
            documento = documento,
            texto = "${documento.tipo} de tu $vehiculo vence hoy."
        )
    }

    fun cancel(documentoId: String) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("$documentoId-antes")
        workManager.cancelUniqueWork("$documentoId-dia")
    }

    private fun programar(nombre: String, dia: LocalDate, documento: Documento, texto: String) {
        val demoraMillis = dia.atTime(HORA_AVISO).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
            System.currentTimeMillis()
        if (demoraMillis <= 0) return

        val datos = workDataOf(
            ReminderNotificationWorker.KEY_DOCUMENTO_ID to documento.id,
            ReminderNotificationWorker.KEY_ENCABEZADO to "Documento por vencer",
            ReminderNotificationWorker.KEY_TITULO to texto,
            ReminderNotificationWorker.KEY_NOTIFICATION_ID to nombre.hashCode()
        )
        val solicitud = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(demoraMillis, TimeUnit.MILLISECONDS)
            .setInputData(datos)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(nombre, ExistingWorkPolicy.REPLACE, solicitud)
    }

    companion object {
        const val DIAS_ANTES = 15L
    }
}
