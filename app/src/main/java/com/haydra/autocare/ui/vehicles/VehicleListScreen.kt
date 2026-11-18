package com.haydra.autocare.ui.vehicles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.model.User
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.data.plan.Limites
import com.haydra.autocare.ui.common.Mensajes
import com.haydra.autocare.ui.common.MostrarMensajes
import com.haydra.autocare.ui.common.formatFechaLarga
import com.haydra.autocare.ui.common.formatKm
import com.haydra.autocare.ui.common.formatMes
import com.haydra.autocare.ui.common.formatMoneda
import com.haydra.autocare.ui.components.AccountMenuButton
import com.haydra.autocare.ui.components.EmptyState
import com.haydra.autocare.ui.components.Esqueleto
import com.haydra.autocare.ui.components.Hueso
import com.haydra.autocare.ui.components.InicioEsqueleto
import com.haydra.autocare.ui.components.TituloSeccion
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pantalla de inicio de AutoCare tras iniciar sesión (RF02, RF04-RF05): de un
 * vistazo, cuánto se gastó este mes entre todos los vehículos, qué avisos
 * están vencidos y el estado de cada vehículo. Cuenta y cierre de sesión
 * (RF03) están en el menú de la barra.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VehicleListScreen(
    usuarioId: String,
    user: User?,
    viewModel: VehicleViewModel,
    inicioViewModel: InicioViewModel,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onAddVehicle: () -> Unit,
    onVehicleClick: (Vehicle) -> Unit,
    onOpenDocumentos: (Vehicle) -> Unit = onVehicleClick,
    mensajes: Mensajes? = null,
    esPro: Boolean = false,
    onOpenPro: () -> Unit = {}
) {
    val state by viewModel.listState.collectAsState()
    val inicio by inicioViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    mensajes?.let { MostrarMensajes(it, snackbarHostState) }
    // Tras cerrar sesión la lista puede traer aún los vehículos de otra cuenta.
    val vehiculos = state.vehiculos.filter { it.usuarioId == usuarioId }

    LaunchedEffect(usuarioId) {
        viewModel.loadVehicles(usuarioId)
    }
    // Se recalcula al volver a la pantalla (p. ej. tras registrar un gasto).
    LaunchedEffect(vehiculos) {
        if (vehiculos.isNotEmpty()) inicioViewModel.cargar(vehiculos)
    }

    // Barra grande con el saludo; al bajar por la lista se encoge.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("${saludo()}, ${user?.nombre.orEmpty().substringBefore(' ')}") },
                subtitle = { Text(formatFechaLarga(LocalDate.now())) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                actions = {
                    AccountMenuButton(
                        nombre = user?.nombre.orEmpty(),
                        correo = user?.correo.orEmpty(),
                        onEditProfile = onEditProfile,
                        onLogout = onLogout,
                        esPro = esPro,
                        onOpenPro = onOpenPro
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (vehiculos.isNotEmpty()) ExtendedFloatingActionButton(
                onClick = onAddVehicle,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Vehículo") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && vehiculos.isEmpty() -> InicioEsqueleto()
                vehiculos.isEmpty() -> EmptyState(
                    icon = Icons.Filled.DirectionsCar,
                    message = "Registra tu vehículo para llevar sus servicios, gastos y avisos.",
                    modifier = Modifier.align(Alignment.Center),
                    actionLabel = "Registrar mi vehículo",
                    onAction = onAddVehicle
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "mes") {
                        ResumenMes(gastoMes = inicio.gastoMes, vehiculos = vehiculos.size, cargado = inicio.cargado)
                    }

                    if (inicio.alertas.isNotEmpty() || inicio.documentos.isNotEmpty()) {
                        item(key = "atencion") { TituloSeccion("Requiere atención") }
                        items(inicio.alertas, key = { "alerta-${it.recordatorio.id}" }) { alerta ->
                            AlertaItem(
                                alerta = alerta,
                                onClick = { onVehicleClick(alerta.vehiculo) },
                                modifier = Modifier.animateItem()
                            )
                        }
                        items(inicio.documentos, key = { "doc-${it.documento.id}" }) { alerta ->
                            AlertaDocumentoItem(
                                alerta = alerta,
                                onClick = { onOpenDocumentos(alerta.vehiculo) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    item(key = "titulo-vehiculos") {
                        TituloSeccion(if (vehiculos.size == 1) "Mi vehículo" else "Mis vehículos (${vehiculos.size})")
                    }
                    items(vehiculos, key = { it.id }) { vehiculo ->
                        VehicleCard(
                            vehiculo = vehiculo,
                            proximo = inicio.proximos[vehiculo.id],
                            onClick = { onVehicleClick(vehiculo) },
                            modifier = Modifier.animateItem()
                        )
                    }

                    if (!esPro) {
                        item(key = "plan") {
                            TarjetaPlanGratis(
                                vehiculos = vehiculos.size,
                                onOpenPro = onOpenPro,
                                modifier = Modifier.animateItem().padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun saludo(hora: LocalTime = LocalTime.now()): String = when {
    hora.hour < 12 -> "Buenos días"
    hora.hour < 19 -> "Buenas tardes"
    else -> "Buenas noches"
}


/**
 * Gasto del mes entre todos los vehículos. Tarjeta clara con icono, a
 * propósito distinta de la tarjeta azul de cada vehículo para que no se
 * confundan.
 */
@Composable
private fun ResumenMes(gastoMes: Double, vehiculos: Int, cargado: Boolean) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = "Gasto de ${formatMes(LocalDate.now())}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (cargado) {
                    Text(
                        text = formatMoneda(gastoMes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    // Mismo alto que la cifra: al llegar el total la tarjeta no cambia de tamaño.
                    Esqueleto(modifier = Modifier.padding(vertical = 4.dp)) {
                        Hueso(modifier = Modifier.width(120.dp).height(24.dp), forma = MaterialTheme.shapes.extraSmall)
                    }
                }
                Text(
                    text = if (vehiculos == 1) "en tu vehículo" else "entre tus $vehiculos vehículos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlertaItem(alerta: Alerta, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = alerta.recordatorio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${alerta.vehiculo.marca} ${alerta.vehiculo.modelo} · ${textoAviso(alerta.recordatorio, alerta.estado)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/** Vencido: en rojo como los avisos vencidos. Por vencer: en verde azulado, para renovarlo a tiempo. */
@Composable
private fun AlertaDocumentoItem(alerta: AlertaDocumento, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val vencido = alerta.vigencia.vencido
    val fondo = if (vencido) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val texto = if (vencido) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = fondo, contentColor = texto),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (vencido) Icons.Filled.Warning else Icons.Filled.Description, contentDescription = null)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = alerta.documento.tipo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${alerta.vehiculo.marca} ${alerta.vehiculo.modelo} · ${alerta.vigencia.texto}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun VehicleCard(
    vehiculo: Vehicle,
    proximo: Pair<Reminder, EstadoRecordatorio.Pendiente>?,
    onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "${vehiculo.marca} ${vehiculo.modelo} · ${vehiculo.anio}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${vehiculo.placas} · ${formatKm(vehiculo.kilometraje)} km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }

        // Solo el próximo aviso que aún no vence: los vencidos ya salen en "Requiere
        // atención" y el gasto en el total del mes y en el Resumen del vehículo.
        AnimatedVisibility(visible = proximo != null) {
            proximo?.let { (recordatorio, estado) -> ProximoAviso(recordatorio, estado) }
        }
    }
}

/** Pie de la tarjeta: el próximo servicio del vehículo. */
@Composable
private fun ProximoAviso(recordatorio: Reminder, estado: EstadoRecordatorio.Pendiente) {
    Column {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Próximo: ${recordatorio.titulo} · ${textoAviso(recordatorio, estado, conKm = false)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/** Recuerda en qué plan está el usuario y cuánto le queda, con acceso a Pro. */
@Composable
private fun TarjetaPlanGratis(vehiculos: Int, onOpenPro: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onOpenPro,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = if (vehiculos < Limites.VEHICULOS_GRATIS) {
                        "Plan Gratis · $vehiculos de ${Limites.VEHICULOS_GRATIS} vehículo"
                    } else {
                        "Plan Gratis · incluye ${Limites.VEHICULOS_GRATIS} vehículo"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = "Mejorar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
