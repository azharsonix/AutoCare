package com.haydra.autocare.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.haydra.autocare.AutoCareApplication
import com.haydra.autocare.data.export.compartirPdf
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.plan.Limites
import com.haydra.autocare.ui.auth.AuthViewModel
import com.haydra.autocare.ui.auth.ForgotPasswordScreen
import com.haydra.autocare.ui.auth.LoginScreen
import com.haydra.autocare.ui.auth.ProfileScreen
import com.haydra.autocare.ui.auth.RegisterScreen
import com.haydra.autocare.ui.common.Mensaje
import com.haydra.autocare.ui.common.Plantillas
import com.haydra.autocare.ui.common.aMillisUtc
import com.haydra.autocare.ui.common.formatFecha
import com.haydra.autocare.ui.common.formatKm
import com.haydra.autocare.ui.common.formatMoneda
import com.haydra.autocare.ui.common.hoyMillis
import com.haydra.autocare.ui.documentos.DocumentoFormScreen
import com.haydra.autocare.ui.documentos.DocumentoViewModel
import com.haydra.autocare.ui.expenses.ExpenseFormScreen
import com.haydra.autocare.ui.expenses.ExpenseViewModel
import com.haydra.autocare.ui.history.HistoryScreen
import com.haydra.autocare.ui.maintenance.MaintenanceFormScreen
import com.haydra.autocare.ui.maintenance.MaintenanceViewModel
import com.haydra.autocare.ui.plan.PlanViewModel
import com.haydra.autocare.ui.plan.ProScreen
import com.haydra.autocare.ui.reminders.ReminderFormScreen
import com.haydra.autocare.ui.reminders.ReminderViewModel
import com.haydra.autocare.ui.vehicles.InicioViewModel
import com.haydra.autocare.ui.vehicles.PESTANA_DOCUMENTOS
import com.haydra.autocare.ui.vehicles.VehicleFormScreen
import com.haydra.autocare.ui.vehicles.VehicleListScreen
import com.haydra.autocare.ui.vehicles.VehicleViewModel
import com.haydra.autocare.ui.vehicles.VehicleWorkspaceScreen
import java.time.LocalDate
import kotlinx.coroutines.launch
import com.haydra.autocare.ui.components.CargandoContenido

private object AutoCareDestinations {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PRO_ARG = "origen"
    const val PRO = "pro?$PRO_ARG={$PRO_ARG}"

    /** [origen]: "vehiculo", "historial" o "menu" (explica por qué se abre Pro). */
    fun proRoute(origen: String) = "pro?$PRO_ARG=$origen"

    const val VEHICLE_ARG = "vehicleId"
    const val VEHICLE_NEW = "vehicles/new"
    const val VEHICLE_EDIT = "vehicles/edit/{$VEHICLE_ARG}"
    const val PESTANA_ARG = "pestana"
    const val VEHICLE_WORKSPACE = "vehicles/detail/{$VEHICLE_ARG}?$PESTANA_ARG={$PESTANA_ARG}"
    const val HISTORY = "vehicles/{$VEHICLE_ARG}/history"

    const val MAINTENANCE_ARG = "maintenanceId"
    const val MAINTENANCE_NEW = "vehicles/{$VEHICLE_ARG}/maintenance/new"
    const val MAINTENANCE_EDIT = "vehicles/{$VEHICLE_ARG}/maintenance/edit/{$MAINTENANCE_ARG}"

    const val EXPENSE_ARG = "expenseId"
    const val EXPENSE_NEW = "vehicles/{$VEHICLE_ARG}/expenses/new"
    const val EXPENSE_EDIT = "vehicles/{$VEHICLE_ARG}/expenses/edit/{$EXPENSE_ARG}"

    const val REMINDER_ARG = "reminderId"
    const val REMINDER_NEW = "vehicles/{$VEHICLE_ARG}/reminders/new"
    const val REMINDER_EDIT = "vehicles/{$VEHICLE_ARG}/reminders/edit/{$REMINDER_ARG}"

    fun vehicleEditRoute(id: String) = "vehicles/edit/$id"
    /** [pestana]: nombre de la pestaña con la que abre (p. ej. "DOCUMENTOS"); por defecto Resumen. */
    fun vehicleWorkspaceRoute(id: String, pestana: String? = null) =
        "vehicles/detail/$id" + (pestana?.let { "?$PESTANA_ARG=$it" } ?: "")
    fun historyRoute(vehicleId: String) = "vehicles/$vehicleId/history"

    fun maintenanceNewRoute(vehicleId: String) = "vehicles/$vehicleId/maintenance/new"
    fun maintenanceEditRoute(vehicleId: String, maintenanceId: String) = "vehicles/$vehicleId/maintenance/edit/$maintenanceId"

    fun expenseNewRoute(vehicleId: String) = "vehicles/$vehicleId/expenses/new"
    fun expenseEditRoute(vehicleId: String, expenseId: String) = "vehicles/$vehicleId/expenses/edit/$expenseId"

    fun reminderNewRoute(vehicleId: String) = "vehicles/$vehicleId/reminders/new"
    fun reminderEditRoute(vehicleId: String, reminderId: String) = "vehicles/$vehicleId/reminders/edit/$reminderId"

    const val DOCUMENTO_ARG = "documentoId"
    const val DOCUMENTO_NEW = "vehicles/{$VEHICLE_ARG}/documentos/new"
    const val DOCUMENTO_EDIT = "vehicles/{$VEHICLE_ARG}/documentos/edit/{$DOCUMENTO_ARG}"

    fun documentoNewRoute(vehicleId: String) = "vehicles/$vehicleId/documentos/new"
    fun documentoEditRoute(vehicleId: String, documentoId: String) = "vehicles/$vehicleId/documentos/edit/$documentoId"
}

/**
 * Flujo de navegación de AutoCare. Tras iniciar sesión, "Mis vehículos" es la
 * pantalla de inicio; cada vehículo abre un espacio de trabajo con barra de
 * navegación inferior (Resumen, Mantenimientos, Gastos, Recordatorios;
 * el historial se abre desde Resumen), según AUTOCARE.txt (sección 28).
 *
 * Cada acción termina con un aviso (snackbar): guardar confirma, eliminar
 * ofrece "Deshacer" y registrar un mantenimiento completa su recordatorio y
 * ofrece programar el siguiente. [vehiculoAAbrir] llega al tocar una
 * notificación y abre directamente ese vehículo.
 */
@Composable
fun AutoCareNavHost(
    navController: NavHostController = rememberNavController(),
    vehiculoAAbrir: String? = null,
    pestanaAAbrir: String? = null,
    onVehiculoAbierto: () -> Unit = {}
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as AutoCareApplication).container }
    val mensajes = container.mensajes

    val authViewModel: AuthViewModel = viewModel {
        AuthViewModel(container.authRepository, container.sessionStore)
    }
    val vehicleViewModel: VehicleViewModel = viewModel { VehicleViewModel(container.vehicleRepository) }
    val maintenanceViewModel: MaintenanceViewModel = viewModel {
        MaintenanceViewModel(container.maintenanceRepository)
    }
    val expenseViewModel: ExpenseViewModel = viewModel { ExpenseViewModel(container.expenseRepository) }
    val reminderViewModel: ReminderViewModel = viewModel {
        ReminderViewModel(container.reminderRepository, container.reminderScheduler)
    }
    val documentoViewModel: DocumentoViewModel = viewModel {
        DocumentoViewModel(container.documentoRepository, container.documentoScheduler)
    }
    val inicioViewModel: InicioViewModel = viewModel {
        InicioViewModel(
            container.maintenanceRepository, container.expenseRepository, container.reminderRepository,
            container.documentoRepository
        )
    }
    val planViewModel: PlanViewModel = viewModel {
        PlanViewModel(container.planRepository, container.proveedorPagos)
    }
    val currentUser by authViewModel.currentUser.collectAsState()
    val sessionChecked by authViewModel.sessionChecked.collectAsState()
    val plan by planViewModel.state.collectAsState()

    // El plan sigue a la cuenta con sesión; el límite de vehículos depende de él.
    LaunchedEffect(currentUser?.id) { planViewModel.observar(currentUser?.id) }
    SideEffect { vehicleViewModel.limiteVehiculos = if (plan.esPro) null else Limites.VEHICULOS_GRATIS }

    if (!sessionChecked) {
        CargandoContenido()
        return
    }

    val startDestination = if (currentUser != null) AutoCareDestinations.HOME else AutoCareDestinations.LOGIN

    val scope = rememberCoroutineScope()
    var exportando by remember { mutableStateOf(false) }

    // Genera el reporte PDF del vehículo y abre el menú de compartir.
    fun exportarPdf(vehicleId: String, incluirFotos: Boolean) {
        if (exportando) return
        exportando = true
        scope.launch {
            container.exportadorReporte.exportar(vehicleId, currentUser?.nombre.orEmpty(), incluirFotos)
                .onSuccess { archivo ->
                    if (!compartirPdf(context, archivo, "Reporte de AutoCare")) {
                        mensajes.mostrar("No hay ninguna app para abrir el PDF")
                    }
                }
                .onFailure { mensajes.mostrar("No se pudo generar el reporte") }
            exportando = false
        }
    }

    fun kilometrajeDe(vehicleId: String): Double? =
        vehicleViewModel.selectedVehicle.value?.takeIf { it.id == vehicleId }?.kilometraje

    // Para el texto de los avisos de documentos ("…de tu Toyota Corolla vence…").
    fun nombreDe(vehicleId: String): String =
        vehicleViewModel.selectedVehicle.value?.takeIf { it.id == vehicleId }?.let { "${it.marca} ${it.modelo}" } ?: "vehículo"

    fun abrirNuevoRecordatorio(vehicleId: String) {
        reminderViewModel.startNewReminder()
        navController.navigate(AutoCareDestinations.reminderNewRoute(vehicleId))
    }

    fun editarMantenimiento(vehicleId: String, mantenimiento: Maintenance) {
        maintenanceViewModel.startEditingMaintenance(mantenimiento)
        navController.navigate(AutoCareDestinations.maintenanceEditRoute(vehicleId, mantenimiento.id))
    }

    fun editarGasto(vehicleId: String, gasto: Expense) {
        expenseViewModel.startEditingExpense(gasto)
        navController.navigate(AutoCareDestinations.expenseEditRoute(vehicleId, gasto.id))
    }

    // Cierre común de las pantallas de edición: confirmar y volver.
    fun guardadoYVolver() {
        mensajes.mostrar("Cambios guardados")
        navController.popBackStack()
    }

    // Al eliminar desde una edición: volver y ofrecer "Deshacer" con el registro borrado.
    fun <T> eliminadoYVolver(texto: String, borrado: T?, restaurar: (T) -> Unit) {
        mensajes.mostrar(Mensaje(texto, accion = "Deshacer") { borrado?.let(restaurar) })
        navController.popBackStack()
    }

    /**
     * Tras registrar un mantenimiento: marca como hecho su recordatorio
     * pendiente (mismo nombre) y ofrece programar el siguiente con el
     * intervalo típico de ese servicio.
     */
    fun alRegistrarMantenimiento(vehicleId: String) {
        val guardado = maintenanceViewModel.formState.value.guardado
        navController.popBackStack()
        if (guardado == null) return
        reminderViewModel.completarPorTitulo(vehicleId, guardado.tipo) { hechos ->
            val texto = if (hechos > 0) {
                "${guardado.tipo} registrado · su aviso quedó como hecho"
            } else {
                "${guardado.tipo} registrado"
            }
            val intervalo = Plantillas.intervaloDe(guardado.tipo)
            if (intervalo == null) {
                mensajes.mostrar(texto)
            } else {
                mensajes.mostrar(
                    Mensaje(texto, accion = "Programar próximo") {
                        val desde = maxOf(guardado.fecha, LocalDate.now())
                        reminderViewModel.startNewReminder(
                            titulo = guardado.tipo,
                            fechaMillis = intervalo.meses?.let { desde.plusMonths(it.toLong()).aMillisUtc() },
                            kilometraje = intervalo.km?.let { guardado.kilometraje + it }
                        )
                        navController.navigate(AutoCareDestinations.reminderNewRoute(vehicleId))
                    }
                )
            }
        }
    }

    // Avanzar desliza la nueva pantalla desde la derecha; volver la regresa.
    val duracion = 300
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(tween(duracion)) { it / 4 } + fadeIn(tween(duracion))
        },
        exitTransition = {
            slideOutHorizontally(tween(duracion)) { -it / 8 } + fadeOut(tween(duracion / 2))
        },
        popEnterTransition = {
            slideInHorizontally(tween(duracion)) { -it / 8 } + fadeIn(tween(duracion))
        },
        popExitTransition = {
            slideOutHorizontally(tween(duracion)) { it / 4 } + fadeOut(tween(duracion / 2))
        }
    ) {
        composable(AutoCareDestinations.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(AutoCareDestinations.HOME) {
                        popUpTo(AutoCareDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(AutoCareDestinations.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(AutoCareDestinations.FORGOT_PASSWORD) }
            )
        }
        composable(AutoCareDestinations.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onResetSuccess = { navController.popBackStack() }
            )
        }
        composable(AutoCareDestinations.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(AutoCareDestinations.HOME) {
                        popUpTo(AutoCareDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(AutoCareDestinations.HOME) {
            val usuarioId = currentUser?.id ?: return@composable
            VehicleListScreen(
                usuarioId = usuarioId,
                user = currentUser,
                viewModel = vehicleViewModel,
                inicioViewModel = inicioViewModel,
                mensajes = mensajes,
                onEditProfile = { navController.navigate(AutoCareDestinations.PROFILE) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(AutoCareDestinations.LOGIN) {
                        popUpTo(AutoCareDestinations.HOME) { inclusive = true }
                    }
                },
                onAddVehicle = {
                    val actuales = vehicleViewModel.listState.value.vehiculos.count { it.usuarioId == usuarioId }
                    if (Limites.puedeAgregarVehiculo(plan.esPro, actuales)) {
                        vehicleViewModel.startNewVehicle()
                        navController.navigate(AutoCareDestinations.VEHICLE_NEW)
                    } else {
                        navController.navigate(AutoCareDestinations.proRoute(ORIGEN_VEHICULO))
                    }
                },
                onVehicleClick = { vehiculo ->
                    navController.navigate(AutoCareDestinations.vehicleWorkspaceRoute(vehiculo.id))
                },
                onOpenDocumentos = { vehiculo ->
                    navController.navigate(AutoCareDestinations.vehicleWorkspaceRoute(vehiculo.id, PESTANA_DOCUMENTOS))
                },
                esPro = plan.esPro,
                onOpenPro = { navController.navigate(AutoCareDestinations.proRoute(ORIGEN_MENU)) }
            )
        }
        composable(
            route = AutoCareDestinations.PRO,
            arguments = listOf(navArgument(AutoCareDestinations.PRO_ARG) {
                type = NavType.StringType
                defaultValue = ORIGEN_MENU
            })
        ) { backStackEntry ->
            val origen = backStackEntry.arguments?.getString(AutoCareDestinations.PRO_ARG) ?: ORIGEN_MENU
            ProScreen(
                state = plan,
                onAvisoMostrado = planViewModel::limpiarAviso,
                motivo = when (origen) {
                    ORIGEN_VEHICULO -> "Tu plan Gratis incluye ${Limites.VEHICULOS_GRATIS} vehículo y ya lo registraste."
                    ORIGEN_HISTORIAL -> "El plan Gratis muestra los últimos ${Limites.MESES_HISTORIAL_GRATIS} meses del historial."
                    else -> null
                },
                onBack = { navController.popBackStack() },
                onComprar = {
                    planViewModel.comprar {
                        mensajes.mostrar("¡Listo! Ya tienes AutoCare Pro")
                        if (origen == ORIGEN_VEHICULO) {
                            // Venía a registrar un vehículo: sigue justo donde lo dejó.
                            vehicleViewModel.startNewVehicle()
                            navController.navigate(AutoCareDestinations.VEHICLE_NEW) {
                                popUpTo(AutoCareDestinations.PRO) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                },
                onRestaurar = { planViewModel.restaurar() },
                onVolverAGratis = {
                    planViewModel.volverAGratis()
                    mensajes.mostrar("Volviste al plan Gratis (demostración)")
                    navController.popBackStack()
                }
            )
        }
        composable(AutoCareDestinations.PROFILE) {
            ProfileScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(AutoCareDestinations.VEHICLE_NEW) {
            val usuarioId = currentUser?.id ?: return@composable
            VehicleFormScreen(
                usuarioId = usuarioId,
                viewModel = vehicleViewModel,
                onBack = { navController.popBackStack() },
                onSaved = {
                    // Recién registrado: se abre directamente para empezar a usarlo.
                    val id = vehicleViewModel.formState.value.guardadoId
                    if (id == null) {
                        navController.popBackStack()
                    } else {
                        mensajes.mostrar("Vehículo registrado")
                        navController.navigate(AutoCareDestinations.vehicleWorkspaceRoute(id)) {
                            popUpTo(AutoCareDestinations.VEHICLE_NEW) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = AutoCareDestinations.VEHICLE_EDIT,
            arguments = listOf(navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType })
        ) {
            val usuarioId = currentUser?.id ?: return@composable
            VehicleFormScreen(
                usuarioId = usuarioId,
                viewModel = vehicleViewModel,
                onBack = { navController.popBackStack() },
                onSaved = ::guardadoYVolver
            )
        }
        composable(
            route = AutoCareDestinations.VEHICLE_WORKSPACE,
            arguments = listOf(
                navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType },
                navArgument(AutoCareDestinations.PESTANA_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val usuarioId = currentUser?.id ?: return@composable
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            VehicleWorkspaceScreen(
                vehicleId = vehicleId,
                pestanaInicial = backStackEntry.arguments?.getString(AutoCareDestinations.PESTANA_ARG),
                usuarioId = usuarioId,
                vehicleViewModel = vehicleViewModel,
                maintenanceViewModel = maintenanceViewModel,
                expenseViewModel = expenseViewModel,
                reminderViewModel = reminderViewModel,
                documentoViewModel = documentoViewModel,
                mensajes = mensajes,
                onExportar = { conFotos -> exportarPdf(vehicleId, conFotos) },
                exportando = exportando,
                onBack = { navController.popBackStack() },
                onEditVehicle = {
                    vehicleViewModel.selectedVehicle.value?.let { vehicleViewModel.startEditingVehicle(it) }
                    navController.navigate(AutoCareDestinations.vehicleEditRoute(vehicleId))
                },
                onDeleted = {
                    mensajes.mostrar("Vehículo eliminado")
                    navController.popBackStack()
                },
                onOpenHistory = { navController.navigate(AutoCareDestinations.historyRoute(vehicleId)) },
                onAddMaintenance = {
                    maintenanceViewModel.startNewMaintenance(fechaMillis = hoyMillis(), kilometraje = kilometrajeDe(vehicleId))
                    navController.navigate(AutoCareDestinations.maintenanceNewRoute(vehicleId))
                },
                onEditMaintenance = { editarMantenimiento(vehicleId, it) },
                onAddExpense = {
                    expenseViewModel.startNewExpense(fechaMillis = hoyMillis(), kilometraje = kilometrajeDe(vehicleId))
                    navController.navigate(AutoCareDestinations.expenseNewRoute(vehicleId))
                },
                onEditExpense = { editarGasto(vehicleId, it) },
                onAddReminder = { abrirNuevoRecordatorio(vehicleId) },
                onEditReminder = { recordatorio ->
                    reminderViewModel.startEditingReminder(recordatorio)
                    navController.navigate(AutoCareDestinations.reminderEditRoute(vehicleId, recordatorio.id))
                },
                onAddDocumento = {
                    documentoViewModel.startNewDocumento()
                    navController.navigate(AutoCareDestinations.documentoNewRoute(vehicleId))
                },
                onEditDocumento = { documento ->
                    documentoViewModel.startEditingDocumento(documento)
                    navController.navigate(AutoCareDestinations.documentoEditRoute(vehicleId, documento.id))
                }
            )
        }
        composable(
            route = AutoCareDestinations.HISTORY,
            arguments = listOf(navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            HistoryScreen(
                vehiculoId = vehicleId,
                esPro = plan.esPro,
                onOpenPro = { navController.navigate(AutoCareDestinations.proRoute(ORIGEN_HISTORIAL)) },
                onExportar = { conFotos -> exportarPdf(vehicleId, conFotos) },
                exportando = exportando,
                maintenanceViewModel = maintenanceViewModel,
                expenseViewModel = expenseViewModel,
                onBack = { navController.popBackStack() },
                onEditMaintenance = { editarMantenimiento(vehicleId, it) },
                onEditExpense = { editarGasto(vehicleId, it) }
            )
        }
        composable(
            route = AutoCareDestinations.MAINTENANCE_NEW,
            arguments = listOf(navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            MaintenanceFormScreen(
                vehiculoId = vehicleId,
                viewModel = maintenanceViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { alRegistrarMantenimiento(vehicleId) }
            )
        }
        composable(
            route = AutoCareDestinations.MAINTENANCE_EDIT,
            arguments = listOf(
                navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType },
                navArgument(AutoCareDestinations.MAINTENANCE_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            val maintenanceId = backStackEntry.arguments?.getString(AutoCareDestinations.MAINTENANCE_ARG)
            MaintenanceFormScreen(
                vehiculoId = vehicleId,
                viewModel = maintenanceViewModel,
                onBack = { navController.popBackStack() },
                onSaved = ::guardadoYVolver,
                onDeleted = {
                    eliminadoYVolver(
                        "Mantenimiento eliminado",
                        maintenanceViewModel.listState.value.mantenimientos.find { it.id == maintenanceId },
                        maintenanceViewModel::restaurarMaintenance
                    )
                }
            )
        }
        composable(
            route = AutoCareDestinations.EXPENSE_NEW,
            arguments = listOf(navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            ExpenseFormScreen(
                vehiculoId = vehicleId,
                viewModel = expenseViewModel,
                onBack = { navController.popBackStack() },
                onSaved = {
                    val guardado = expenseViewModel.formState.value.guardado
                    mensajes.mostrar(guardado?.let { "Gasto de ${formatMoneda(it.monto)} registrado" } ?: "Gasto registrado")
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = AutoCareDestinations.EXPENSE_EDIT,
            arguments = listOf(
                navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType },
                navArgument(AutoCareDestinations.EXPENSE_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            val expenseId = backStackEntry.arguments?.getString(AutoCareDestinations.EXPENSE_ARG)
            ExpenseFormScreen(
                vehiculoId = vehicleId,
                viewModel = expenseViewModel,
                onBack = { navController.popBackStack() },
                onSaved = ::guardadoYVolver,
                onDeleted = {
                    eliminadoYVolver(
                        "Gasto eliminado",
                        expenseViewModel.listState.value.gastos.find { it.id == expenseId },
                        expenseViewModel::restaurarExpense
                    )
                }
            )
        }
        composable(
            route = AutoCareDestinations.REMINDER_NEW,
            arguments = listOf(navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            ReminderFormScreen(
                vehiculoId = vehicleId,
                viewModel = reminderViewModel,
                kilometrajeActual = kilometrajeDe(vehicleId),
                onBack = { navController.popBackStack() },
                onSaved = {
                    val guardado = reminderViewModel.formState.value.guardado
                    mensajes.mostrar(guardado?.let { avisoDeRecordatorio(it) } ?: "Recordatorio creado")
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = AutoCareDestinations.REMINDER_EDIT,
            arguments = listOf(
                navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType },
                navArgument(AutoCareDestinations.REMINDER_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            val reminderId = backStackEntry.arguments?.getString(AutoCareDestinations.REMINDER_ARG)
            ReminderFormScreen(
                vehiculoId = vehicleId,
                viewModel = reminderViewModel,
                kilometrajeActual = kilometrajeDe(vehicleId),
                onBack = { navController.popBackStack() },
                onSaved = ::guardadoYVolver,
                onDeleted = {
                    eliminadoYVolver(
                        "Recordatorio eliminado",
                        reminderViewModel.listState.value.recordatorios.find { it.id == reminderId },
                        reminderViewModel::restaurarReminder
                    )
                }
            )
        }
        composable(
            route = AutoCareDestinations.DOCUMENTO_NEW,
            arguments = listOf(navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            DocumentoFormScreen(
                vehiculoId = vehicleId,
                nombreVehiculo = nombreDe(vehicleId),
                viewModel = documentoViewModel,
                onBack = { navController.popBackStack() },
                onSaved = {
                    val guardado = documentoViewModel.formState.value.guardado
                    mensajes.mostrar(
                        when {
                            guardado == null -> "Documento guardado"
                            guardado.vence != null -> "${guardado.tipo} guardado. Te avisaremos antes de que venza"
                            else -> "${guardado.tipo} guardado"
                        }
                    )
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = AutoCareDestinations.DOCUMENTO_EDIT,
            arguments = listOf(
                navArgument(AutoCareDestinations.VEHICLE_ARG) { type = NavType.StringType },
                navArgument(AutoCareDestinations.DOCUMENTO_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString(AutoCareDestinations.VEHICLE_ARG) ?: return@composable
            val documentoId = backStackEntry.arguments?.getString(AutoCareDestinations.DOCUMENTO_ARG)
            DocumentoFormScreen(
                vehiculoId = vehicleId,
                nombreVehiculo = nombreDe(vehicleId),
                viewModel = documentoViewModel,
                onBack = { navController.popBackStack() },
                onSaved = ::guardadoYVolver,
                onDeleted = {
                    eliminadoYVolver(
                        "Documento eliminado",
                        documentoViewModel.listState.value.documentos.find { it.id == documentoId }
                    ) { documentoViewModel.restaurarDocumento(it, nombreDe(vehicleId)) }
                }
            )
        }
    }

    // Al tocar una notificación se abre su vehículo (en Docs si es de un documento).
    LaunchedEffect(vehiculoAAbrir, currentUser) {
        val id = vehiculoAAbrir ?: return@LaunchedEffect
        if (currentUser == null) return@LaunchedEffect
        navController.navigate(AutoCareDestinations.vehicleWorkspaceRoute(id, pestanaAAbrir)) { launchSingleTop = true }
        onVehiculoAbierto()
    }
}

private const val ORIGEN_VEHICULO = "vehiculo"
private const val ORIGEN_HISTORIAL = "historial"
private const val ORIGEN_MENU = "menu"

private fun avisoDeRecordatorio(recordatorio: Reminder): String = buildString {
    append("Te avisaremos el ${formatFecha(recordatorio.fecha)}")
    recordatorio.kilometraje?.let { append(" o a los ${formatKm(it)} km") }
}
