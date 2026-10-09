package com.haydra.autocare.ui.vehicles

import androidx.compose.runtime.Immutable
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.ui.common.formatFecha
import com.haydra.autocare.ui.common.formatKm
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Categoría con la que se agrupan los mantenimientos junto a los gastos. */
const val CATEGORIA_MANTENIMIENTO = "Mantenimiento"

/** Por debajo de estos km recorridos el costo por km no es representativo. */
private const val KM_MINIMOS_COSTO_POR_KM = 100.0

@Immutable
data class CategoriaGasto(val nombre: String, val total: Double)

/** Situación de un recordatorio respecto a hoy y al kilometraje actual. */
sealed interface EstadoRecordatorio {
    data object Completado : EstadoRecordatorio

    data class Vencido(val porFecha: Boolean, val porKilometraje: Boolean) : EstadoRecordatorio

    /** [kmRestantes] es null cuando el recordatorio no tiene kilometraje. */
    data class Pendiente(val diasRestantes: Long, val kmRestantes: Double?) : EstadoRecordatorio
}

@Immutable
data class VehicleStats(
    val gastoMes: Double,
    val gastoAnio: Double,
    val gastoTotal: Double,
    /** null si todavía no hay suficientes km registrados para calcularlo. */
    val costoPorKm: Double?,
    val totalMantenimientos: Int,
    /** Gasto del año actual por categoría, de mayor a menor. */
    val categoriasAnio: List<CategoriaGasto>,
    val proximoRecordatorio: Reminder?,
    val estadoProximo: EstadoRecordatorio?,
    val recordatoriosVencidos: Int,
    val ultimoMantenimiento: Maintenance?
)

fun estadoRecordatorio(
    recordatorio: Reminder,
    kilometrajeActual: Double,
    hoy: LocalDate = LocalDate.now()
): EstadoRecordatorio {
    if (recordatorio.completado) return EstadoRecordatorio.Completado

    val porFecha = recordatorio.fecha.isBefore(hoy)
    val porKilometraje = recordatorio.kilometraje?.let { it <= kilometrajeActual } ?: false
    if (porFecha || porKilometraje) return EstadoRecordatorio.Vencido(porFecha, porKilometraje)

    return EstadoRecordatorio.Pendiente(
        diasRestantes = ChronoUnit.DAYS.between(hoy, recordatorio.fecha),
        kmRestantes = recordatorio.kilometraje?.let { it - kilometrajeActual }
    )
}

/**
 * Resume la información de un vehículo para la pestaña "Resumen". Los
 * mantenimientos cuentan como gasto (categoría [CATEGORIA_MANTENIMIENTO]).
 * Las listas pueden traer registros de otro vehículo (los ViewModels se
 * comparten entre vehículos), por eso se filtran por id.
 */
fun calcularEstadisticas(
    vehiculo: Vehicle,
    mantenimientos: List<Maintenance>,
    gastos: List<Expense>,
    recordatorios: List<Reminder>,
    hoy: LocalDate = LocalDate.now()
): VehicleStats {
    val propiosMantenimientos = mantenimientos.filter { it.vehiculoId == vehiculo.id }
    val propiosGastos = gastos.filter { it.vehiculoId == vehiculo.id }
    val propiosRecordatorios = recordatorios.filter { it.vehiculoId == vehiculo.id }

    val movimientos: List<Pair<LocalDate, CategoriaGasto>> =
        propiosGastos.map { it.fecha to CategoriaGasto(it.categoria.trim(), it.monto) } +
            propiosMantenimientos.map { it.fecha to CategoriaGasto(CATEGORIA_MANTENIMIENTO, it.costo) }

    val delAnio = movimientos.filter { (fecha, _) -> fecha.year == hoy.year }
    val gastoMes = delAnio.filter { (fecha, _) -> fecha.month == hoy.month }.sumOf { it.second.total }
    val gastoAnio = delAnio.sumOf { it.second.total }
    val gastoTotal = movimientos.sumOf { it.second.total }

    val categoriasAnio = delAnio
        .groupBy({ it.second.nombre.ifBlank { "Otros" } }, { it.second.total })
        .map { (nombre, montos) -> CategoriaGasto(nombre, montos.sum()) }
        .filter { it.total > 0 }
        .sortedByDescending { it.total }

    // Costo por km desde el primer kilometraje registrado en un mantenimiento.
    val kmInicial = propiosMantenimientos.map { it.kilometraje }.filter { it > 0 }.minOrNull()
    val kmRecorridos = kmInicial?.let { vehiculo.kilometraje - it }
    val costoPorKm = kmRecorridos
        ?.takeIf { it >= KM_MINIMOS_COSTO_POR_KM }
        ?.let { gastoTotal / it }

    val activos = propiosRecordatorios
        .filterNot { it.completado }
        .map { it to estadoRecordatorio(it, vehiculo.kilometraje, hoy) }
    val vencidos = activos.filter { it.second is EstadoRecordatorio.Vencido }
    // Primero lo vencido; después lo que llega antes, por fecha.
    val proximo = (vencidos.sortedBy { it.first.fecha } + activos.filter { it.second is EstadoRecordatorio.Pendiente }
        .sortedBy { it.first.fecha })
        .firstOrNull()

    return VehicleStats(
        gastoMes = gastoMes,
        gastoAnio = gastoAnio,
        gastoTotal = gastoTotal,
        costoPorKm = costoPorKm,
        totalMantenimientos = propiosMantenimientos.size,
        categoriasAnio = categoriasAnio,
        proximoRecordatorio = proximo?.first,
        estadoProximo = proximo?.second,
        recordatoriosVencidos = vencidos.size,
        ultimoMantenimiento = propiosMantenimientos.maxByOrNull { it.fecha }
    )
}

/** A partir de estos días se muestra la fecha en vez de "En N días". */
private const val DIAS_TEXTO_RELATIVO = 30

/**
 * Único texto con el estado de un aviso: cuándo toca (o desde cuándo está
 * vencido) y cuántos km faltan. Sustituye a mostrar la fecha y el km objetivo
 * y además "en N días · faltan X km", que era el mismo dato dos veces.
 * [conKm] = false lo acorta para espacios estrechos.
 */
fun textoAviso(recordatorio: Reminder, estado: EstadoRecordatorio, conKm: Boolean = true): String = when (estado) {
    EstadoRecordatorio.Completado -> "Hecho"
    is EstadoRecordatorio.Vencido -> {
        val km = recordatorio.kilometraje?.let { "${formatKm(it)} km" }
        when {
            estado.porFecha && estado.porKilometraje && km != null ->
                "Venció el ${formatFecha(recordatorio.fecha)} y ya pasó los $km"
            estado.porKilometraje && km != null -> "Ya pasó los $km"
            else -> "Venció el ${formatFecha(recordatorio.fecha)}"
        }
    }
    is EstadoRecordatorio.Pendiente -> buildString {
        append(
            when {
                estado.diasRestantes == 0L -> "Hoy"
                estado.diasRestantes == 1L -> "Mañana"
                estado.diasRestantes <= DIAS_TEXTO_RELATIVO -> "En ${estado.diasRestantes} días"
                else -> "El ${formatFecha(recordatorio.fecha)}"
            }
        )
        if (conKm) estado.kmRestantes?.let { append(" · faltan ${formatKm(it)} km") }
    }
}

/** Una cifra de gasto con su etiqueta, lista para mostrar en la app o en el PDF. */
data class CifraGasto(val etiqueta: String, val monto: Double)

/**
 * Cifras de gasto de un vehículo sin repetir: una cifra igual a la anterior
 * (p. ej. todo el gasto del año es de este mes) no se muestra otra vez con
 * otra etiqueta. [conMes] = false omite el mes (el PDF resume por año).
 */
fun cifrasDeGasto(
    stats: VehicleStats,
    etiquetaAnio: String = "Este año",
    conMes: Boolean = true
): List<CifraGasto> = buildList {
    if (conMes) add(CifraGasto("Este mes", stats.gastoMes))
    if (!conMes || stats.gastoAnio != stats.gastoMes) add(CifraGasto(etiquetaAnio, stats.gastoAnio))
    if (stats.gastoTotal != stats.gastoAnio) add(CifraGasto("Gasto total", stats.gastoTotal))
}
