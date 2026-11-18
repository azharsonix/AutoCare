package com.haydra.autocare.ui.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.ui.components.DeslizarParaBorrar
import com.haydra.autocare.ui.components.EmptyState
import com.haydra.autocare.ui.components.EncabezadoGrupo
import com.haydra.autocare.ui.components.FilaEnGrupo
import com.haydra.autocare.ui.components.ListaEsqueleto
import com.haydra.autocare.ui.components.PosicionEnGrupo
import com.haydra.autocare.ui.vehicles.EstadoRecordatorio
import com.haydra.autocare.ui.vehicles.estadoRecordatorio
import com.haydra.autocare.ui.vehicles.textoAviso

/**
 * Contenido de la pestaña "Avisos" dentro del flujo por vehículo (ver
 * [com.haydra.autocare.ui.vehicles.VehicleWorkspaceScreen]): recordatorios en
 * tres grupos, Vencidos, Próximos y Hechos. La casilla lo marca como hecho;
 * tocarlo abre su edición y deslizarlo a la izquierda lo elimina.
 */
@Composable
fun ReminderTabContent(
    vehiculoId: String,
    viewModel: ReminderViewModel,
    onEditReminder: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    kilometrajeActual: Double,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listState.collectAsState()

    LaunchedEffect(vehiculoId) {
        viewModel.loadReminders(vehiculoId)
    }

    val conEstado = state.recordatorios
        .filter { it.vehiculoId == vehiculoId }
        .sortedBy { it.fecha }
        .map { it to estadoRecordatorio(it, kilometrajeActual) }
    val vencidos = conEstado.filter { it.second is EstadoRecordatorio.Vencido }
    val proximos = conEstado.filter { it.second is EstadoRecordatorio.Pendiente }
    val hechos = conEstado.filter { it.second is EstadoRecordatorio.Completado }

    val alternar = { recordatorio: Reminder -> viewModel.toggleCompletado(vehiculoId, recordatorio) }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && conEstado.isEmpty() ->
                ListaEsqueleto()
            conEstado.isEmpty() -> EmptyState(
                icon = Icons.Filled.Notifications,
                message = "Sin recordatorios.\nCrea uno por fecha o por kilometraje y te avisamos.",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)) {
                grupo("Vencidos", vencidos, alternar, onEditReminder, onDeleteReminder)
                grupo("Próximos", proximos, alternar, onEditReminder, onDeleteReminder)
                grupo("Hechos", hechos, alternar, onEditReminder, onDeleteReminder)
            }
        }
    }
}

/** Un grupo de avisos con su título y cuántos tiene; no se pinta si está vacío. */
private fun LazyListScope.grupo(
    titulo: String,
    avisos: List<Pair<Reminder, EstadoRecordatorio>>,
    onAlternar: (Reminder) -> Unit,
    onEditar: (Reminder) -> Unit,
    onBorrar: (Reminder) -> Unit
) {
    if (avisos.isEmpty()) return
    item(key = "grupo-$titulo") {
        EncabezadoGrupo("$titulo (${avisos.size})", modifier = Modifier.animateItem())
    }
    itemsIndexed(avisos, key = { _, (recordatorio, _) -> recordatorio.id }) { indice, (recordatorio, estado) ->
        val posicion = PosicionEnGrupo.de(indice, avisos.size)
        DeslizarParaBorrar(posicion = posicion, onBorrar = { onBorrar(recordatorio) }, modifier = Modifier.animateItem()) {
            FilaAviso(
                recordatorio = recordatorio,
                estado = estado,
                posicion = posicion,
                onAlternar = { onAlternar(recordatorio) },
                onClick = { onEditar(recordatorio) }
            )
        }
    }
}

@Composable
private fun FilaAviso(
    recordatorio: Reminder,
    estado: EstadoRecordatorio,
    posicion: PosicionEnGrupo,
    onAlternar: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vencido = estado is EstadoRecordatorio.Vencido
    // El divisor empieza donde el texto, pasada la casilla.
    FilaEnGrupo(posicion = posicion, onClick = onClick, inicioDivisor = 56.dp, modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = recordatorio.completado, onCheckedChange = { onAlternar() })
            Column(modifier = Modifier.weight(1f).padding(vertical = 14.dp)) {
                Text(
                    text = recordatorio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (recordatorio.completado) TextDecoration.LineThrough else null,
                    color = if (recordatorio.completado) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Una sola línea de estado; en los hechos la casilla ya lo dice.
                if (estado !is EstadoRecordatorio.Completado) {
                    Text(
                        text = textoAviso(recordatorio, estado),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (vencido) FontWeight.SemiBold else null,
                        color = if (vencido) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
