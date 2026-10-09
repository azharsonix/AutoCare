package com.haydra.autocare.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.plan.Limites
import com.haydra.autocare.ui.common.formatDia
import com.haydra.autocare.ui.common.formatFecha
import com.haydra.autocare.ui.common.formatKm
import com.haydra.autocare.ui.common.formatMoneda
import com.haydra.autocare.ui.components.AutoCareTopBar
import com.haydra.autocare.ui.components.BotonExportarPdf
import com.haydra.autocare.ui.components.EmptyState
import com.haydra.autocare.ui.components.PosicionEnGrupo
import com.haydra.autocare.ui.components.RegistroItem
import com.haydra.autocare.ui.components.agruparPorMes
import com.haydra.autocare.ui.components.seccionesPorMes
import com.haydra.autocare.ui.expenses.ExpenseViewModel
import com.haydra.autocare.ui.maintenance.MaintenanceViewModel
import java.time.LocalDate

/** Un movimiento de la línea de tiempo: un mantenimiento o un gasto. */
data class Actividad(
    val id: String,
    val fecha: LocalDate,
    val titulo: String,
    val detalle: String,
    val monto: Double,
    val mantenimiento: Maintenance? = null,
    val gasto: Expense? = null
)

/** Mantenimientos y gastos de un vehículo en una sola lista, del más reciente al más antiguo. */
fun construirActividad(vehiculoId: String, mantenimientos: List<Maintenance>, gastos: List<Expense>): List<Actividad> {
    val deMantenimientos = mantenimientos.filter { it.vehiculoId == vehiculoId }.map {
        Actividad(it.id, it.fecha, it.tipo, "${formatKm(it.kilometraje)} km", it.costo, mantenimiento = it)
    }
    val deGastos = gastos.filter { it.vehiculoId == vehiculoId }.map {
        Actividad(it.id, it.fecha, it.categoria, it.descripcion, it.monto, gasto = it)
    }
    return (deMantenimientos + deGastos).sortedByDescending { it.fecha }
}

/**
 * [fecha] decide cómo se muestra el día: completo en "Actividad reciente" y
 * solo el día en el historial, donde el mes ya está en el encabezado.
 */
@Composable
fun ActividadItem(
    actividad: Actividad,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fecha: (LocalDate) -> String = ::formatFecha,
    posicion: PosicionEnGrupo = PosicionEnGrupo.UNICA
) {
    val esGasto = actividad.gasto != null
    RegistroItem(
        icono = if (esGasto) Icons.Filled.Payments else Icons.Filled.Build,
        titulo = actividad.titulo,
        detalle = listOf(fecha(actividad.fecha), actividad.detalle).filter { it.isNotBlank() }.joinToString(" · "),
        monto = formatMoneda(actividad.monto),
        colorIcono = if (esGasto) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        colorSobreIcono = if (esGasto) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        posicion = posicion,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Historial completo del vehículo (RF14, HU07): mantenimientos y gastos en
 * una línea de tiempo agrupada por mes, con el total de cada mes. Se abre
 * desde "Actividad reciente" en la pestaña Resumen.
 */
@Composable
fun HistoryScreen(
    vehiculoId: String,
    maintenanceViewModel: MaintenanceViewModel,
    expenseViewModel: ExpenseViewModel,
    onBack: () -> Unit,
    onEditMaintenance: (Maintenance) -> Unit,
    onEditExpense: (Expense) -> Unit,
    onExportar: (incluirFotos: Boolean) -> Unit = {},
    exportando: Boolean = false,
    esPro: Boolean = true,
    onOpenPro: () -> Unit = {}
) {
    val maintenanceState by maintenanceViewModel.listState.collectAsState()
    val expenseState by expenseViewModel.listState.collectAsState()

    LaunchedEffect(vehiculoId) {
        maintenanceViewModel.loadMaintenances(vehiculoId)
        expenseViewModel.loadExpenses(vehiculoId)
    }

    val (porMes, ocultas) = remember(maintenanceState.mantenimientos, expenseState.gastos, esPro) {
        val (visibles, fuera) = filtrarHistorialPorPlan(
            construirActividad(vehiculoId, maintenanceState.mantenimientos, expenseState.gastos),
            esPro
        )
        agruparPorMes(visibles) { it.fecha } to fuera
    }

    Scaffold(
        topBar = {
            AutoCareTopBar(
                title = "Historial",
                onBack = onBack,
                actions = { BotonExportarPdf(exportando = exportando, onExportar = onExportar) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (porMes.isEmpty() && ocultas == 0) {
                EmptyState(
                    icon = Icons.Filled.History,
                    message = "Todavía no hay actividad.\nAquí verás mantenimientos y gastos juntos.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    seccionesPorMes(porMes, clave = { it.id }, monto = { it.monto }) { actividad, posicion ->
                        ActividadItem(
                            actividad = actividad,
                            onClick = {
                                actividad.mantenimiento?.let(onEditMaintenance)
                                actividad.gasto?.let(onEditExpense)
                            },
                            fecha = ::formatDia,
                            posicion = posicion,
                            modifier = Modifier.animateItem()
                        )
                    }
                    if (ocultas > 0) {
                        item(key = "pro") {
                            HistorialEnPro(ocultas = ocultas, onOpenPro = onOpenPro, modifier = Modifier.padding(top = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * El plan Gratis ve los últimos [Limites.MESES_HISTORIAL_GRATIS] meses.
 * Devuelve lo que se muestra y cuántos movimientos anteriores quedan fuera.
 */
fun filtrarHistorialPorPlan(
    actividades: List<Actividad>,
    esPro: Boolean,
    hoy: LocalDate = LocalDate.now()
): Pair<List<Actividad>, Int> {
    if (esPro) return actividades to 0
    val inicio = Limites.inicioHistorialGratis(hoy)
    val visibles = actividades.filter { !it.fecha.isBefore(inicio) }
    return visibles to (actividades.size - visibles.size)
}

@Composable
private fun HistorialEnPro(ocultas: Int, onOpenPro: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (ocultas == 1) "1 movimiento más antiguo" else "$ocultas movimientos más antiguos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "El plan Gratis muestra los últimos ${Limites.MESES_HISTORIAL_GRATIS} meses. " +
                    "Siguen guardados y aparecen en el reporte PDF; con Pro los ves aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
            TextButton(onClick = onOpenPro, modifier = Modifier.padding(top = 4.dp)) {
                Text("Ver historial completo con Pro")
            }
        }
    }
}
