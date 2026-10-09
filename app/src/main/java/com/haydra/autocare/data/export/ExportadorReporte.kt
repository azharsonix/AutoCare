package com.haydra.autocare.data.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.haydra.autocare.data.repository.DocumentoRepository
import com.haydra.autocare.data.repository.ExpenseRepository
import com.haydra.autocare.data.repository.MaintenanceRepository
import com.haydra.autocare.data.repository.ReminderRepository
import com.haydra.autocare.data.repository.VehicleRepository
import java.io.File

/**
 * Exporta el reporte PDF de un vehículo: lee los datos al día de los
 * repositorios (no de las listas en pantalla, que pueden ser de otro
 * vehículo o estar a medio cargar) y genera el archivo con [ReportePdf].
 */
class ExportadorReporte(
    private val context: Context,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val expenseRepository: ExpenseRepository,
    private val reminderRepository: ReminderRepository,
    private val documentoRepository: DocumentoRepository
) {
    suspend fun exportar(vehiculoId: String, propietario: String, incluirFotos: Boolean = false): Result<File> = runCatching {
        val datos = DatosReporte(
            vehiculo = vehicleRepository.getVehicle(vehiculoId).getOrThrow(),
            propietario = propietario,
            mantenimientos = maintenanceRepository.getMaintenances(vehiculoId).getOrThrow(),
            gastos = expenseRepository.getExpenses(vehiculoId).getOrThrow(),
            recordatorios = reminderRepository.getReminders(vehiculoId).getOrThrow(),
            documentos = documentoRepository.getDocumentos(vehiculoId).getOrThrow(),
            incluirFotos = incluirFotos
        )
        ReportePdf(context).generar(datos)
    }
}

/**
 * Abre el menú de compartir de Android con el PDF (guardar en Archivos o
 * Drive, enviar por WhatsApp o correo…). Devuelve false si no hay ninguna
 * app que pueda recibirlo.
 */
fun compartirPdf(context: Context, archivo: File, titulo: String): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, titulo)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(envio, "Compartir reporte"))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
