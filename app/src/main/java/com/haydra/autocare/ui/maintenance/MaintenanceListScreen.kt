package com.haydra.autocare.ui.maintenance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.ui.common.formatDia
import com.haydra.autocare.ui.common.formatKm
import com.haydra.autocare.ui.common.formatMoneda
import com.haydra.autocare.ui.components.CargandoContenido
import com.haydra.autocare.ui.components.DeslizarParaBorrar
import com.haydra.autocare.ui.components.EmptyState
import com.haydra.autocare.ui.components.ListaEncabezado
import com.haydra.autocare.ui.components.RegistroItem
import com.haydra.autocare.ui.components.agruparPorMes
import com.haydra.autocare.ui.components.seccionesPorMes

/**
 * Contenido de la pestaña "Servicios" dentro del flujo por vehículo (ver
 * [com.haydra.autocare.ui.vehicles.VehicleWorkspaceScreen]): mantenimientos
 * agrupados por mes. Tocar uno abre su edición; deslizarlo a la izquierda lo elimina.
 */
@Composable
fun MaintenanceTabContent(
    vehiculoId: String,
    viewModel: MaintenanceViewModel,
    onEditMaintenance: (Maintenance) -> Unit,
    onDeleteMaintenance: (Maintenance) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listState.collectAsState()

    LaunchedEffect(vehiculoId) {
        viewModel.loadMaintenances(vehiculoId)
    }

    val mantenimientos = state.mantenimientos.filter { it.vehiculoId == vehiculoId }
    val porMes = agruparPorMes(mantenimientos) { it.fecha }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && mantenimientos.isEmpty() ->
                CargandoContenido()
            mantenimientos.isEmpty() -> EmptyState(
                icon = Icons.Filled.Build,
                message = "Aún no hay mantenimientos.\nToca + para registrar el primero.",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)) {
                item(key = "encabezado") {
                    ListaEncabezado(
                        texto = if (mantenimientos.size == 1) "1 mantenimiento" else "${mantenimientos.size} mantenimientos",
                        total = if (mantenimientos.size > 1) formatMoneda(mantenimientos.sumOf { it.costo }) else null
                    )
                }
                // Con un solo mes, su total sería el mismo que el general.
                seccionesPorMes(porMes, clave = { it.id }, monto = { it.costo }, conTotales = porMes.size > 1) { mantenimiento, posicion ->
                    DeslizarParaBorrar(posicion = posicion, onBorrar = { onDeleteMaintenance(mantenimiento) }, modifier = Modifier.animateItem()) {
                        RegistroItem(
                            icono = Icons.Filled.Build,
                            titulo = mantenimiento.tipo,
                            detalle = "${formatDia(mantenimiento.fecha)} · ${formatKm(mantenimiento.kilometraje)} km",
                            monto = formatMoneda(mantenimiento.costo),
                            posicion = posicion,
                            conFoto = mantenimiento.foto != null,
                            onClick = { onEditMaintenance(mantenimiento) }
                        )
                    }
                }
            }
        }
    }
}
