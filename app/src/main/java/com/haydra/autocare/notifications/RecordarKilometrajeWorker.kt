package com.haydra.autocare.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.haydra.autocare.AutoCareApplication
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Una vez por semana pregunta el kilometraje de los vehículos que llevan
 * [DIAS_SIN_ACTUALIZAR] días o más sin ponerlo al día. Sin el km actual los
 * recordatorios por kilometraje no avisan a tiempo. Registrar un servicio o
 * una carga de gasolina con km también cuenta como puesto al día.
 */
class RecordarKilometrajeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = (applicationContext as AutoCareApplication).container.database.vehicleDao()
        val limite = LocalDate.now().minusDays(DIAS_SIN_ACTUALIZAR)
        dao.getAll()
            .filter { it.kmActualizado == null || it.kmActualizado.isBefore(limite) }
            .forEach { vehiculo ->
                notificar(
                    context = applicationContext,
                    id = "km-${vehiculo.id}".hashCode(),
                    canal = Canales.KILOMETRAJE,
                    titulo = "¿Cuántos km lleva tu ${vehiculo.marca} ${vehiculo.modelo}?",
                    texto = "Actualízalo para que tus avisos por kilometraje lleguen a tiempo.",
                    vehiculoId = vehiculo.id
                )
            }
        return Result.success()
    }

    companion object {
        private const val NOMBRE = "recordar-kilometraje"
        const val DIAS_SIN_ACTUALIZAR = 14L

        /** Se llama al abrir la app; si ya estaba programado no se toca. */
        fun programar(context: Context) {
            val solicitud = PeriodicWorkRequestBuilder<RecordarKilometrajeWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NOMBRE, ExistingPeriodicWorkPolicy.KEEP, solicitud)
        }
    }
}
