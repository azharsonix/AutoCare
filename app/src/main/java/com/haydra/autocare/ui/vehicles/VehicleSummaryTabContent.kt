package com.haydra.autocare.ui.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.notifications.RecordarKilometrajeWorker
import com.haydra.autocare.ui.common.formatCantidad
import com.haydra.autocare.ui.common.formatKm
import com.haydra.autocare.ui.common.formatKmPorLitro
import com.haydra.autocare.ui.common.formatMoneda
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.PosicionEnGrupo
import com.haydra.autocare.ui.components.TituloSeccion
import com.haydra.autocare.ui.expenses.ExpenseViewModel
import com.haydra.autocare.ui.history.ActividadItem
import com.haydra.autocare.ui.history.construirActividad
import com.haydra.autocare.ui.maintenance.MaintenanceViewModel
import com.haydra.autocare.ui.reminders.ReminderViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Contenido de la pestaña "Resumen" dentro del flujo por vehículo
 * (ver [VehicleWorkspaceScreen]): panel con kilometraje, gasto del mes y del
 * año, costo por km, próximo servicio, actividad reciente y gasto por
 * categoría. Editar y eliminar el vehículo están en la barra superior.
 */
@Composable
fun VehicleSummaryTabContent(
    vehiculo: Vehicle,
    maintenanceViewModel: MaintenanceViewModel,
    expenseViewModel: ExpenseViewModel,
    reminderViewModel: ReminderViewModel,
    onUpdateKilometraje: (Double) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenHistory: () -> Unit,
    onEditMaintenance: (Maintenance) -> Unit,
    onEditExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    val mantenimientos by maintenanceViewModel.listState.collectAsState()
    val gastos by expenseViewModel.listState.collectAsState()
    val recordatorios by reminderViewModel.listState.collectAsState()
    var mostrarDialogoKm by remember { mutableStateOf(false) }

    LaunchedEffect(vehiculo.id) {
        maintenanceViewModel.loadMaintenances(vehiculo.id)
        expenseViewModel.loadExpenses(vehiculo.id)
        reminderViewModel.loadReminders(vehiculo.id)
    }

    val stats = remember(vehiculo, mantenimientos.mantenimientos, gastos.gastos, recordatorios.recordatorios) {
        calcularEstadisticas(vehiculo, mantenimientos.mantenimientos, gastos.gastos, recordatorios.recordatorios)
    }
    val rendimiento = remember(vehiculo.id, gastos.gastos) {
        calcularRendimiento(gastos.gastos.filter { it.vehiculoId == vehiculo.id })
    }
    val actividadReciente = remember(vehiculo.id, mantenimientos.mantenimientos, gastos.gastos) {
        construirActividad(vehiculo.id, mantenimientos.mantenimientos, gastos.gastos).take(ACTIVIDAD_RECIENTE)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VehicleHeroCard(vehiculo = vehiculo, onActualizarKm = { mostrarDialogoKm = true })

        val indicadores = buildList {
            cifrasDeGasto(stats).forEach { cifra ->
                add(Indicador(cifra.etiqueta, formatMoneda(cifra.monto), Icons.Filled.Payments))
            }
            add(
                Indicador(
                    "Costo por km",
                    stats.costoPorKm?.let { formatMoneda(it) } ?: "—",
                    Icons.Filled.Speed,
                    nota = if (stats.costoPorKm == null) "Registra mantenimientos con km" else null
                )
            )
            add(
                Indicador(
                    "Rendimiento",
                    rendimiento?.let { formatKmPorLitro(it.kmPorLitro) } ?: "—",
                    Icons.Filled.LocalGasStation,
                    nota = if (rendimiento == null) "Anota litros y km al cargar gasolina" else null
                )
            )
        }
        indicadores.chunked(2).forEach { fila ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                fila.forEach { indicador ->
                    StatCard(
                        etiqueta = indicador.etiqueta,
                        valor = indicador.valor,
                        icono = indicador.icono,
                        nota = indicador.nota,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        rendimiento?.caida?.let { caida -> AvisoRendimiento(caida = caida, rendimiento = rendimiento) }

        ProximoServicioCard(stats = stats, onClick = onOpenReminders)

        if (actividadReciente.isNotEmpty()) {
            TituloSeccion("Actividad reciente", accion = "Ver historial", onAccion = onOpenHistory)
            // Un solo bloque: las filas van unidas, sin el espacio entre tarjetas.
            Column {
                actividadReciente.forEachIndexed { indice, actividad ->
                    ActividadItem(
                        actividad = actividad,
                        onClick = {
                            actividad.mantenimiento?.let(onEditMaintenance)
                            actividad.gasto?.let(onEditExpense)
                        },
                        posicion = PosicionEnGrupo.de(indice, actividadReciente.size)
                    )
                }
            }
        }

        // Con una sola categoría el reparto sería "100 %", lo mismo que el gasto del año.
        if (stats.categoriasAnio.size > 1) {
            GastoPorCategoriaCard(categorias = stats.categoriasAnio)
        }
    }

    if (mostrarDialogoKm) {
        ActualizarKmDialog(
            kilometrajeActual = vehiculo.kilometraje,
            onConfirm = { km ->
                mostrarDialogoKm = false
                onUpdateKilometraje(km)
            },
            onDismiss = { mostrarDialogoKm = false }
        )
    }
}

private const val ACTIVIDAD_RECIENTE = 3

private data class Indicador(
    val etiqueta: String,
    val valor: String,
    val icono: ImageVector,
    val nota: String? = null
)

@Composable
private fun VehicleHeroCard(vehiculo: Vehicle, onActualizarKm: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Azul de marca a uno más profundo del mismo tono: con el grafito el degradado se veía turbio.
                .background(Brush.linearGradient(listOf(colores.primary, lerp(colores.primary, colores.onPrimaryContainer, 0.45f))))
                .padding(20.dp)
        ) {
            // Marca y modelo ya están en la barra superior: aquí no se repiten.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${vehiculo.anio} · ${vehiculo.color}",
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = MaterialTheme.shapes.small, color = colores.onPrimary) {
                    Text(
                        text = vehiculo.placas,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colores.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Kilometraje",
                style = MaterialTheme.typography.labelLarge,
                color = colores.onPrimary.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 20.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${formatKm(vehiculo.kilometraje)} km",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colores.onPrimary
                    )
                    textoKmActualizado(vehiculo.kmActualizado)?.let { (texto, atrasado) ->
                        Text(
                            text = texto,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (atrasado) FontWeight.SemiBold else null,
                            color = colores.onPrimary.copy(alpha = if (atrasado) 1f else 0.85f)
                        )
                    }
                }
                TextButton(onClick = onActualizarKm) {
                    Icon(Icons.Filled.Speed, contentDescription = null, tint = colores.onPrimary, modifier = Modifier.size(18.dp))
                    Text("Actualizar", color = colores.onPrimary, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/**
 * Cuándo se puso al día el km; [Pair.second] = true si ya pasó el plazo del
 * aviso semanal (así se entiende por qué llega esa notificación).
 */
private fun textoKmActualizado(fecha: LocalDate?, hoy: LocalDate = LocalDate.now()): Pair<String, Boolean>? {
    fecha ?: return null
    val dias = ChronoUnit.DAYS.between(fecha, hoy)
    val texto = when {
        dias <= 0L -> "Actualizado hoy"
        dias == 1L -> "Actualizado ayer"
        else -> "Actualizado hace $dias días"
    }
    return texto to (dias >= RecordarKilometrajeWorker.DIAS_SIN_ACTUALIZAR)
}

@Composable
private fun StatCard(
    etiqueta: String,
    valor: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    nota: String? = null
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
            nota?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ProximoServicioCard(stats: VehicleStats, onClick: () -> Unit) {
    val recordatorio = stats.proximoRecordatorio
    val vencido = stats.estadoProximo is EstadoRecordatorio.Vencido
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = if (vencido) "Servicio vencido" else "Próximo servicio",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                if (recordatorio == null) {
                    Text(
                        text = "Sin recordatorios pendientes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Crea uno para que te avisemos a tiempo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    Text(
                        text = recordatorio.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stats.estadoProximo?.let { textoAviso(recordatorio, it) }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (vencido) FontWeight.Bold else null,
                        color = if (vencido) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (stats.recordatoriosVencidos > 1) {
                        Text(
                            text = "y ${stats.recordatoriosVencidos - 1} vencido(s) más",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GastoPorCategoriaCard(categorias: List<CategoriaGasto>) {
    val maximo = categorias.maxOf { it.total }
    val mostradas = categorias.take(5)
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "¿En qué se va el dinero? (este año)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            mostradas.forEach { categoria ->
                Column {
                    Row {
                        Text(
                            text = categoria.nombre,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatMoneda(categoria.total),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((categoria.total / maximo).toFloat().coerceIn(0.03f, 1f))
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
            }
            if (categorias.size > mostradas.size) {
                Text(
                    text = "Otras ${categorias.size - mostradas.size} categorías: " +
                        formatMoneda(categorias.drop(mostradas.size).sumOf { it.total }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ActualizarKmDialog(
    kilometrajeActual: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var texto by remember { mutableStateOf(formatCantidad(kilometrajeActual)) }
    val valor = texto.replace(",", "").toDoubleOrNull()
    val error = when {
        texto.isBlank() -> "Escribe el kilometraje"
        valor == null || valor < 0 -> "Kilometraje no válido"
        else -> null
    }
    val aviso = if (error == null && valor!! < kilometrajeActual) {
        "Es menor que el actual (${formatKm(kilometrajeActual)} km). Úsalo solo para corregir un error."
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actualizar kilometraje") },
        text = {
            Column {
                AutoCareTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = "Kilometraje actual",
                    errorText = error,
                    keyboardType = KeyboardType.Number
                )
                aviso?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { valor?.let(onConfirm) }, enabled = error == null) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/**
 * El rendimiento de la última carga cayó bastante frente a las anteriores:
 * suele indicar llantas bajas, filtro de aire sucio o bujías gastadas.
 */
@Composable
private fun AvisoRendimiento(caida: Double, rendimiento: Rendimiento) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.LocalGasStation, contentDescription = null, modifier = Modifier.padding(top = 2.dp))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "Tu rendimiento bajó ${Math.round(caida * 100)} %",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "La última carga rindió ${formatKmPorLitro(rendimiento.ultimo)}. " +
                        "Revisa la presión de las llantas, el filtro de aire y las bujías.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
