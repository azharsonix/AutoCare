package com.haydra.autocare

import com.haydra.autocare.data.DemoData
import com.haydra.autocare.data.plan.Limites
import com.haydra.autocare.data.plan.PlanRepository
import com.haydra.autocare.data.plan.ProveedorPagos
import com.haydra.autocare.data.repository.FakeVehicleRepository
import com.haydra.autocare.ui.history.Actividad
import com.haydra.autocare.ui.history.filtrarHistorialPorPlan
import com.haydra.autocare.ui.plan.PlanViewModel
import com.haydra.autocare.ui.vehicles.VehicleViewModel
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Modelo freemium: límites del plan Gratis, compra de Pro y su efecto. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlanTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class PlanEnMemoria : PlanRepository {
        val pro = MutableStateFlow(setOf<String>())
        override fun esPro(usuarioId: String): Flow<Boolean> = pro.map { usuarioId in it }
        override suspend fun activarPro(usuarioId: String) { pro.value = pro.value + usuarioId }
        override suspend fun quitarPro(usuarioId: String) { pro.value = pro.value - usuarioId }
    }

    private class PagosDePrueba(private val aprobar: Boolean) : ProveedorPagos {
        override val esDemostracion = true
        override suspend fun comprarPro(): Result<Unit> =
            if (aprobar) Result.success(Unit) else Result.failure(IllegalStateException("Pago rechazado"))
        override suspend fun restaurarCompra(): Result<Boolean> = Result.success(false)
    }

    @Test
    fun `el plan Gratis permite un vehiculo y Pro ilimitados`() {
        assertTrue(Limites.puedeAgregarVehiculo(esPro = false, vehiculosActuales = 0))
        assertFalse(Limites.puedeAgregarVehiculo(esPro = false, vehiculosActuales = 1))
        assertFalse(Limites.puedeAgregarVehiculo(esPro = false, vehiculosActuales = 2))
        assertTrue(Limites.puedeAgregarVehiculo(esPro = true, vehiculosActuales = 10))
    }

    @Test
    fun `comprar Pro lo activa solo para esa cuenta`() = runTest(mainDispatcherRule.testDispatcher) {
        val plan = PlanEnMemoria()
        val viewModel = PlanViewModel(plan, PagosDePrueba(aprobar = true))
        viewModel.observar("ana")
        advanceUntilIdle()
        assertFalse(viewModel.state.value.esPro)

        var exito = false
        viewModel.comprar { exito = true }
        advanceUntilIdle()

        assertTrue(exito)
        assertTrue(viewModel.state.value.esPro)
        viewModel.observar("luis")
        advanceUntilIdle()
        assertFalse(viewModel.state.value.esPro)
    }

    @Test
    fun `un pago rechazado no activa Pro y muestra el error`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PlanViewModel(PlanEnMemoria(), PagosDePrueba(aprobar = false))
        viewModel.observar("ana")
        viewModel.comprar()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.esPro)
        assertFalse(viewModel.state.value.procesando)
        assertEquals("Pago rechazado", viewModel.state.value.errorMessage)
    }

    @Test
    fun `volver a Gratis en demostracion quita Pro`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = PlanViewModel(PlanEnMemoria(), PagosDePrueba(aprobar = true))
        viewModel.observar("ana")
        viewModel.comprar()
        advanceUntilIdle()
        viewModel.volverAGratis()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.esPro)
    }

    @Test
    fun `con el limite alcanzado no se registra otro vehiculo pero si se edita`() = runTest(mainDispatcherRule.testDispatcher) {
        // La cuenta demo ya tiene dos vehículos: los conserva, pero no puede añadir más.
        val viewModel = VehicleViewModel(FakeVehicleRepository())
        viewModel.limiteVehiculos = Limites.VEHICULOS_GRATIS
        viewModel.startNewVehicle()
        viewModel.onMarcaChange("Mazda")
        viewModel.onModeloChange("3")
        viewModel.onAnioChange("2021")
        viewModel.onColorChange("Azul")
        viewModel.onPlacasChange("XYZ-001")
        viewModel.onKilometrajeChange("10000")
        viewModel.saveVehicle(DemoData.USER_ID)
        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.errorMessage)
        assertFalse(viewModel.formState.value.savedSuccessfully)

        viewModel.loadVehicles(DemoData.USER_ID)
        advanceUntilIdle()
        val existente = viewModel.listState.value.vehiculos.first()
        viewModel.startEditingVehicle(existente)
        viewModel.onColorChange("Negro")
        viewModel.saveVehicle(DemoData.USER_ID)
        advanceUntilIdle()

        assertNull(viewModel.formState.value.errorMessage)
        assertTrue(viewModel.formState.value.savedSuccessfully)
    }

    @Test
    fun `el historial Gratis muestra los ultimos 12 meses`() {
        val hoy = LocalDate.of(2026, 9, 25)
        val actividades = listOf(
            Actividad("a", hoy, "Reciente", "", 100.0),
            Actividad("b", hoy.minusMonths(12), "Justo en el límite", "", 100.0),
            Actividad("c", hoy.minusMonths(12).minusDays(1), "Antigua", "", 100.0),
            Actividad("d", hoy.minusYears(3), "Muy antigua", "", 100.0)
        )

        val (gratis, ocultas) = filtrarHistorialPorPlan(actividades, esPro = false, hoy = hoy)
        assertEquals(listOf("a", "b"), gratis.map { it.id })
        assertEquals(2, ocultas)

        val (pro, ocultasPro) = filtrarHistorialPorPlan(actividades, esPro = true, hoy = hoy)
        assertEquals(4, pro.size)
        assertEquals(0, ocultasPro)
    }
}
