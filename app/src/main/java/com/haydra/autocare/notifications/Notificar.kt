package com.haydra.autocare.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.haydra.autocare.MainActivity
import com.haydra.autocare.R

/** Canales de notificación de la app (se crean en AutoCareApplication). */
object Canales {
    const val RECORDATORIOS = "recordatorios"
    const val KILOMETRAJE = "kilometraje"
}

/**
 * Muestra una notificación de AutoCare. Al tocarla abre la app directamente
 * en [vehiculoId] (si se indica). No hace nada si el usuario no dio permiso.
 */
fun notificar(
    context: Context,
    id: Int,
    canal: String,
    titulo: String,
    texto: String,
    vehiculoId: String?,
    /** Pestaña del vehículo con la que abre (p. ej. Docs); null = Resumen. */
    pestana: String? = null
) {
    val puedeNotificar = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (!puedeNotificar) return

    val alTocar = PendingIntent.getActivity(
        context,
        id,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            vehiculoId?.let { putExtra(MainActivity.EXTRA_VEHICULO_ID, it) }
            pestana?.let { putExtra(MainActivity.EXTRA_PESTANA, it) }
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notificacion = NotificationCompat.Builder(context, canal)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(titulo)
        .setContentText(texto)
        .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
        .setContentIntent(alTocar)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(context).notify(id, notificacion)
}
