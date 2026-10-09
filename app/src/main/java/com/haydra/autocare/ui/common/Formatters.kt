package com.haydra.autocare.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Idioma y región de todos los textos con formato de la app (fechas, moneda, km). */
val localeMx: Locale = Locale.Builder().setLanguage("es").setRegion("MX").build()

private val fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", localeMx)
private val fechaLargaFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", localeMx)
private val mesFormatter = DateTimeFormatter.ofPattern("MMMM", localeMx)
private val mesAnioFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", localeMx)
private val diaFormatter = DateTimeFormatter.ofPattern("EEE d", localeMx)

/** 25/09/2026 */
fun formatFecha(fecha: LocalDate): String = fecha.format(fechaFormatter)

/** Viernes 25 de septiembre */
fun formatFechaLarga(fecha: LocalDate): String = fecha.format(fechaLargaFormatter).mayusculaInicial()

/** septiembre */
fun formatMes(fecha: LocalDate): String = fecha.format(mesFormatter)

/** Septiembre 2026 */
fun formatMesAnio(fecha: LocalDate): String = fecha.format(mesAnioFormatter).mayusculaInicial()

/** Vie 25 (cuando el mes ya se ve en otro sitio). */
fun formatDia(fecha: LocalDate): String = fecha.format(diaFormatter).mayusculaInicial()

fun formatFechaMillis(millis: Long): String = formatFecha(millis.aFechaUtc())

/** Número sin ".0" cuando es entero (para rellenar campos de texto). */
fun formatCantidad(valor: Double): String =
    if (valor == valor.toLong().toDouble()) valor.toLong().toString() else valor.toString()

/** Rendimiento de gasolina con un decimal (p. ej. 12.4 km/l). */
fun formatKmPorLitro(valor: Double): String = String.format(localeMx, "%.1f km/l", valor)

fun formatMoneda(valor: Double): String = "$" + String.format(localeMx, "%,.2f", valor)

/** Kilometraje con separador de miles, sin decimales (p. ej. 42,000). */
fun formatKm(km: Double): String = String.format(localeMx, "%,d", Math.round(km))

/**
 * El selector de fecha de Material trabaja con milisegundos a medianoche
 * UTC; estas dos funciones convierten entre ese formato y [LocalDate].
 */
fun LocalDate.aMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.aFechaUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Hoy, en el formato del selector de fecha. */
fun hoyMillis(): Long = LocalDate.now().aMillisUtc()

private fun String.mayusculaInicial(): String = replaceFirstChar { it.titlecase(localeMx) }
