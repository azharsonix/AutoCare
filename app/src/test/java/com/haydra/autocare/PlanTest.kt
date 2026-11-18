package com.haydra.autocare

import com.haydra.autocare.data.plan.Limites
import com.haydra.autocare.data.plan.PlanRepository
import com.haydra.autocare.data.plan.ProveedorPagos
import com.haydra.autocare.ui.plan.PlanViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
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
}
