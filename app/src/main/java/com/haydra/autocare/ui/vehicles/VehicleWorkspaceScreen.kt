package com.haydra.autocare.ui.vehicles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import com.haydra.autocare.data.model.Documento
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.ui.common.Mensaje
import com.haydra.autocare.ui.common.Mensajes
import com.haydra.autocare.ui.common.MostrarMensajes
import com.haydra.autocare.ui.components.AutoCareTopBar
import com.haydra.autocare.ui.components.BotonExportarPdf
import com.haydra.autocare.ui.components.CargandoContenido
import com.haydra.autocare.ui.components.EliminarAccion
import com.haydra.autocare.ui.documentos.DocumentoViewModel
import com.haydra.autocare.ui.documentos.DocumentosTabContent
import com.haydra.autocare.ui.expenses.ExpenseTabContent
import com.haydra.autocare.ui.expenses.ExpenseViewModel
import com.haydra.autocare.ui.maintenance.MaintenanceTabContent
import com.haydra.autocare.ui.maintenance.MaintenanceViewModel
import com.haydra.autocare.ui.reminders.ReminderTabContent
import com.haydra.autocare.ui.reminders.ReminderViewModel

/** Para abrir el vehículo directamente en la pestaña Docs (desde el inicio). */
const val PESTANA_DOCUMENTOS = "DOCUMENTOS"

/** [accion] es el texto del botón flotante de la pestaña (null = sin botón). */
private enum class VehicleTab(val label: String, val icon: ImageVector, val accion: String?) {
    RESUMEN("Resumen", Icons.Filled.DirectionsCar, null),
    MANTENIMIENTOS("Servicios", Icons.Filled.Build, "Mantenimiento"),
    GASTOS("Gastos", Icons.Filled.Payments, "Gasto"),
    RECORDATORIOS("Avisos", Icons.Filled.Notifications, "Recordatorio"),
    DOCUMENTOS("Docs", Icons.Filled.Description, "Documento")
}

/**
 * Espacio de trabajo de un vehículo: Resumen, Mantenimientos, Gastos,
 * Recordatorios y Documentos como pestañas de una barra de navegación inferior. El
 * historial (RF14) se abre desde "Actividad reciente" en Resumen para no
 * repetir en otra pestaña lo que ya muestran Mantenimientos y Gastos.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VehicleWorkspaceScreen(
    vehicleId: String,
    /** Nombre de [VehicleTab] con el que abre; null = Resumen. */
    pestanaInicial: String? = null,
    usuarioId: String,
    vehicleViewModel: VehicleViewModel,
    maintenanceViewModel: MaintenanceViewModel,
    expenseViewModel: ExpenseViewModel,
    reminderViewModel: ReminderViewModel,
    documentoViewModel: DocumentoViewModel,
    onBack: () -> Unit,
    onEditVehicle: () -> Unit,
    onDeleted: () -> Unit,
    onOpenHistory: () -> Unit,
    onAddMaintenance: () -> Unit,
    onEditMaintenance: (Maintenance) -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onAddReminder: () -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onAddDocumento: () -> Unit,
    onEditDocumento: (Documento) -> Unit,
    mensajes: Mensajes? = null,
    onExportar: (incluirFotos: Boolean) -> Unit = {},
    exportando: Boolean = false
) {
    val snackbarHostState = remember { SnackbarHostState() }
    mensajes?.let { MostrarMensajes(it, snackbarHostState) }
    var selectedTabIndex by rememberSaveable {
        mutableStateOf(VehicleTab.entries.find { it.name == pestanaInicial }?.ordinal ?: VehicleTab.RESUMEN.ordinal)
    }
    val selectedTab = VehicleTab.entries[selectedTabIndex]
    val vehiculo by vehicleViewModel.selectedVehicle.collectAsState()

    LaunchedEffect(vehicleId) {
        vehicleViewModel.loadVehicle(vehicleId)
    }

    val actual = vehiculo?.takeIf { it.id == vehicleId }
    val mantenimientos by maintenanceViewModel.listState.collectAsState()

    val actualizarKilometraje: (Double) -> Unit = { nuevoKm ->
        actual?.let { v ->
            vehicleViewModel.updateKilometraje(v, nuevoKm) { kmAnterior ->
                reminderViewModel.avisarPorKilometraje(vehicleId, kmAnterior, nuevoKm)
            }
        }
    }

    // Registrar un mantenimiento o una carga de gasolina con más km que el
    // vehículo actualiza su kilometraje.
    val gastos by expenseViewModel.listState.collectAsState()
    val kmMaximoRegistrado = (
        mantenimientos.mantenimientos.filter { it.vehiculoId == vehicleId }.map { it.kilometraje } +
            gastos.gastos.filter { it.vehiculoId == vehicleId }.mapNotNull { it.kilometraje }
        ).maxOrNull()
    LaunchedEffect(kmMaximoRegistrado, actual?.id) {
        val km = kmMaximoRegistrado ?: return@LaunchedEffect
        if (actual != null && km > actual.kilometraje) actualizarKilometraje(km)
    }

    val titulo = actual?.let { "${it.marca} ${it.modelo}" } ?: "Vehículo"

    // Borrado al deslizar una fila: se elimina al momento y se ofrece "Deshacer".
    fun <T> borrarConDeshacer(texto: String, registro: T, borrar: (T) -> Unit, restaurar: (T) -> Unit) {
        borrar(registro)
        mensajes?.mostrar(Mensaje(texto, accion = "Deshacer") { restaurar(registro) })
    }

    // La barra toma otro tono al bajar por el contenido, para separarla de él.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AutoCareTopBar(
                title = titulo,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (actual != null) {
                        BotonExportarPdf(exportando = exportando, onExportar = onExportar)
                        IconButton(onClick = onEditVehicle) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar vehículo")
                        }
                        EliminarAccion(
                            titulo = "Eliminar vehículo",
                            mensaje = "Se eliminará ${actual.marca} ${actual.modelo} (${actual.placas}) con todos sus " +
                                "mantenimientos, gastos y recordatorios. Esta acción no se puede deshacer.",
                            onConfirm = {
                                vehicleViewModel.deleteVehicle(usuarioId, actual.id)
                                onDeleted()
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                VehicleTab.entries.forEach { tab ->
                    ShortNavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTabIndex = tab.ordinal },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = ShortNavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColorTopIconPosition = MaterialTheme.colorScheme.primary,
                            selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            selectedTab.accion?.let { accion ->
                ExtendedFloatingActionButton(
                    onClick = when (selectedTab) {
                        VehicleTab.MANTENIMIENTOS -> onAddMaintenance
                        VehicleTab.GASTOS -> onAddExpense
                        VehicleTab.DOCUMENTOS -> onAddDocumento
                        else -> onAddReminder
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(accion) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (actual == null) {
                CargandoContenido()
            } else {
                AnimatedContent(
                    targetState = selectedTab,
                    // Se desliza hacia el lado de la pestaña elegida, como en la barra de abajo.
                    transitionSpec = {
                        val haciaDerecha = targetState.ordinal > initialState.ordinal
                        val desplazamiento = { ancho: Int -> if (haciaDerecha) ancho / 5 else -ancho / 5 }
                        (slideInHorizontally(initialOffsetX = desplazamiento) + fadeIn()) togetherWith
                            (slideOutHorizontally(targetOffsetX = { -desplazamiento(it) }) + fadeOut())
                    },
                    label = "pestaña"
                ) { tab ->
                    when (tab) {
                        VehicleTab.RESUMEN -> VehicleSummaryTabContent(
                            vehiculo = actual,
                            maintenanceViewModel = maintenanceViewModel,
                            expenseViewModel = expenseViewModel,
                            reminderViewModel = reminderViewModel,
                            onUpdateKilometraje = actualizarKilometraje,
                            onOpenReminders = { selectedTabIndex = VehicleTab.RECORDATORIOS.ordinal },
                            onOpenHistory = onOpenHistory,
                            onEditMaintenance = onEditMaintenance,
                            onEditExpense = onEditExpense
                        )
                        VehicleTab.MANTENIMIENTOS -> MaintenanceTabContent(
                            vehiculoId = vehicleId,
                            viewModel = maintenanceViewModel,
                            onEditMaintenance = onEditMaintenance,
                            onDeleteMaintenance = { m ->
                                borrarConDeshacer("Mantenimiento eliminado", m, { maintenanceViewModel.deleteMaintenance(vehicleId, it.id) }, maintenanceViewModel::restaurarMaintenance)
                            }
                        )
                        VehicleTab.GASTOS -> ExpenseTabContent(
                            vehiculoId = vehicleId,
                            viewModel = expenseViewModel,
                            onEditExpense = onEditExpense,
                            onDeleteExpense = { g ->
                                borrarConDeshacer("Gasto eliminado", g, { expenseViewModel.deleteExpense(vehicleId, it.id) }, expenseViewModel::restaurarExpense)
                            }
                        )
                        VehicleTab.RECORDATORIOS -> ReminderTabContent(
                            vehiculoId = vehicleId,
                            viewModel = reminderViewModel,
                            onEditReminder = onEditReminder,
                            onDeleteReminder = { r ->
                                borrarConDeshacer("Recordatorio eliminado", r, { reminderViewModel.deleteReminder(vehicleId, it.id) }, reminderViewModel::restaurarReminder)
                            },
                            kilometrajeActual = actual.kilometraje
                        )
                        VehicleTab.DOCUMENTOS -> DocumentosTabContent(
                            vehiculoId = vehicleId,
                            viewModel = documentoViewModel,
                            onEditDocumento = onEditDocumento,
                            onDeleteDocumento = { d ->
                                borrarConDeshacer("Documento eliminado", d, { documentoViewModel.deleteDocumento(vehicleId, it.id) }) {
                                    documentoViewModel.restaurarDocumento(it, titulo)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
