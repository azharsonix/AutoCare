package com.haydra.autocare.ui.vehicles

import androidx.compose.runtime.Immutable
import com.haydra.autocare.data.model.Expense

/** Categoría de gasto que activa los campos de litros y kilometraje. */
const val CATEGORIA_GASOLINA = "Gasolina"

fun esGasolina(categoria: String): Boolean = categoria.trim().equals(CATEGORIA_GASOLINA, ignoreCase = true)

/** Una caída mayor a esta (15 %) frente al promedio se avisa. */
private const val CAIDA_PARA_AVISAR = 0.15

/** Tramos fuera de esto son errores de captura (km mal escritos), no rendimiento real. */
private const val KM_POR_LITRO_MAXIMO = 60.0

@Immutable
data class Rendimiento(
    /** Promedio ponderado (km totales / litros totales) de todos los tramos. */
    val kmPorLitro: Double,
    /** El último tramo (entre las dos cargas más recientes). */
    val ultimo: Double,
    /** Cuánto bajó el último tramo frente al promedio de los anteriores (0.18 = 18 %); null si no bajó de forma notable. */
    val caida: Double?
)

/**
 * Rendimiento de gasolina a partir de las cargas con litros y kilometraje,
 * suponiendo tanque lleno en cada carga: los km entre una carga y la
 * siguiente se recorrieron con los litros de esa siguiente carga. Hacen falta
 * al menos dos cargas; null si no hay datos suficientes.
 */
fun calcularRendimiento(gastos: List<Expense>): Rendimiento? {
    val cargas = cargasConDatos(gastos)

    // (km recorridos, litros) de cada tramo válido.
    val tramos = cargas.zipWithNext { anterior, siguiente ->
        (siguiente.kilometraje!! - anterior.kilometraje!!) to siguiente.litros!!
    }.filter { (km, litros) -> km > 0 && km / litros <= KM_POR_LITRO_MAXIMO }

    if (tramos.isEmpty()) return null

    val promedio = tramos.sumOf { it.first } / tramos.sumOf { it.second }
    val ultimo = tramos.last().let { (km, litros) -> km / litros }

    // La caída se mide contra los tramos anteriores, no contra un promedio que ya incluye el último.
    val anteriores = tramos.dropLast(1)
    val caida = if (anteriores.size >= 2) {
        val previo = anteriores.sumOf { it.first } / anteriores.sumOf { it.second }
        (1 - ultimo / previo).takeIf { it >= CAIDA_PARA_AVISAR }
    } else {
        null
    }

    return Rendimiento(kmPorLitro = promedio, ultimo = ultimo, caida = caida)
}

/** Cargas de gasolina con litros y km, ordenadas por kilometraje. */
private fun cargasConDatos(gastos: List<Expense>): List<Expense> = gastos
    .filter { esGasolina(it.categoria) && (it.litros ?: 0.0) > 0 && it.kilometraje != null }
    .sortedBy { it.kilometraje }

/** Una carga con lo que rindió desde la anterior (null en la primera o si el tramo no es válido). */
@Immutable
data class TramoRendimiento(val carga: Expense, val kmRecorridos: Double?, val kmPorLitro: Double?)

/** El detalle carga por carga que resume [calcularRendimiento] (para el reporte PDF). */
fun tramosRendimiento(gastos: List<Expense>): List<TramoRendimiento> {
    val cargas = cargasConDatos(gastos)
    return cargas.mapIndexed { i, carga ->
        val anterior = cargas.getOrNull(i - 1)
        val km = anterior?.let { carga.kilometraje!! - it.kilometraje!! }?.takeIf { it > 0 }
        val kmPorLitro = km?.let { it / carga.litros!! }?.takeIf { it <= KM_POR_LITRO_MAXIMO }
        TramoRendimiento(carga, km.takeIf { kmPorLitro != null }, kmPorLitro)
    }
}
