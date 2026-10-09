package com.haydra.autocare

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.haydra.autocare.notifications.Canales
import com.haydra.autocare.notifications.RecordarKilometrajeWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoCareApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        crearCanalesDeNotificaciones()
        // Fuera del hilo principal: WorkManager y la base de datos se inicializan
        // en segundo plano mientras corre la animación de inicio.
        alcance.launch {
            RecordarKilometrajeWorker.programar(this@AutoCareApplication)
            runCatching { container.limpiarFotos() }
        }
    }

    private fun crearCanalesDeNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canales = listOf(
            NotificationChannel(Canales.RECORDATORIOS, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos de mantenimientos, actividades y documentos por vencer de tus vehículos"
            },
            NotificationChannel(Canales.KILOMETRAJE, "Actualizar kilometraje", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Te pregunta cada semana el kilometraje si no lo has actualizado"
            }
        )
        getSystemService(NotificationManager::class.java).createNotificationChannels(canales)
    }
}
