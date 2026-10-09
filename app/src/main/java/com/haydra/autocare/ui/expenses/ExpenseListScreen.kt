package com.haydra.autocare.ui.expenses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.ui.common.formatCantidad
import com.haydra.autocare.ui.common.formatDia
import com.haydra.autocare.ui.common.formatMoneda
import com.haydra.autocare.ui.components.CargandoContenido
import com.haydra.autocare.ui.components.DeslizarParaBorrar
import com.haydra.autocare.ui.components.EmptyState
import com.haydra.autocare.ui.components.ListaEncabezado
import com.haydra.autocare.ui.components.RegistroItem
import com.haydra.autocare.ui.components.agruparPorMes
import com.haydra.autocare.ui.components.seccionesPorMes

/**
 * Contenido de la pestaña "Gastos" dentro del flujo por vehículo (ver
 * [com.haydra.autocare.ui.vehicles.VehicleWorkspaceScreen]): gastos agrupados
 * por mes. Tocar uno abre su edición; deslizarlo a la izquierda lo elimina.
 */
@Composable
fun ExpenseTabContent(
    vehiculoId: String,
    viewModel: ExpenseViewModel,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listState.collectAsState()

    LaunchedEffect(vehiculoId) {
        viewModel.loadExpenses(vehiculoId)
    }

    val gastos = state.gastos.filter { it.vehiculoId == vehiculoId }
    val porMes = agruparPorMes(gastos) { it.fecha }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && gastos.isEmpty() ->
                CargandoContenido()
            gastos.isEmpty() -> EmptyState(
                icon = Icons.Filled.Payments,
                message = "Aún no hay gastos.\nToca + para registrar el primero.",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)) {
                item(key = "encabezado") {
                    ListaEncabezado(
                        texto = if (gastos.size == 1) "1 gasto" else "${gastos.size} gastos",
                        total = if (gastos.size > 1) formatMoneda(gastos.sumOf { it.monto }) else null
                    )
                }
                // Con un solo mes, su total sería el mismo que el general.
                seccionesPorMes(porMes, clave = { it.id }, monto = { it.monto }, conTotales = porMes.size > 1) { gasto, posicion ->
                    DeslizarParaBorrar(posicion = posicion, onBorrar = { onDeleteExpense(gasto) }, modifier = Modifier.animateItem()) {
                        RegistroItem(
                            icono = Icons.Filled.Payments,
                            titulo = gasto.categoria,
                            detalle = listOf(formatDia(gasto.fecha), gasto.litros?.let { "${formatCantidad(it)} L" }.orEmpty(), gasto.descripcion)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            monto = formatMoneda(gasto.monto),
                            colorIcono = MaterialTheme.colorScheme.secondaryContainer,
                            colorSobreIcono = MaterialTheme.colorScheme.onSecondaryContainer,
                            posicion = posicion,
                            conFoto = gasto.foto != null,
                            onClick = { onEditExpense(gasto) }
                        )
                    }
                }
            }
        }
    }
}
