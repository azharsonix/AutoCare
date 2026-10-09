package com.haydra.autocare.ui.documentos

import com.haydra.autocare.notifications.DocumentoScheduler
import com.haydra.autocare.ui.common.formatFecha
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Estado del vencimiento de un documento en un solo texto. [urgente] si ya
 * venció o vence dentro de [DocumentoScheduler.DIAS_ANTES] días (cuando llega
 * el primer aviso).
 */
data class Vigencia(val texto: String, val urgente: Boolean, val vencido: Boolean)

fun vigencia(vence: LocalDate?, hoy: LocalDate = LocalDate.now()): Vigencia {
    if (vence == null) return Vigencia("Sin vencimiento", urgente = false, vencido = false)
    val dias = ChronoUnit.DAYS.between(hoy, vence)
    return when {
        dias < 0 -> Vigencia("Venció el ${formatFecha(vence)}", urgente = true, vencido = true)
        dias == 0L -> Vigencia("Vence hoy", urgente = true, vencido = false)
        dias == 1L -> Vigencia("Vence mañana", urgente = true, vencido = false)
        dias <= DocumentoScheduler.DIAS_ANTES -> Vigencia("Vence en $dias días", urgente = true, vencido = false)
        else -> Vigencia("Vigente hasta el ${formatFecha(vence)}", urgente = false, vencido = false)
    }
}
