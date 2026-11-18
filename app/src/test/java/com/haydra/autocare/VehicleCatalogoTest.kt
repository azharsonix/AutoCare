package com.haydra.autocare

import com.haydra.autocare.data.catalogo.CatalogoVehiculos
import com.haydra.autocare.data.catalogo.DatosVin
import com.haydra.autocare.data.repository.FakeVehicleRepository
import com.haydra.autocare.ui.vehicles.VehicleViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Formulario de vehículo con el catálogo de la API de terceros (sustituido por uno falso). */
@OptIn(ExperimentalCoroutinesApi::class)
class VehicleCatalogoTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class CatalogoFalso(private val falla: Boolean = false) : CatalogoVehiculos {
        override suspend fun modelos(marca: String, anio: Int?) =
            Result.success(if (marca == "Nissan") listOf("March", "Sentra", "Versa") else emptyList())

        override suspend fun decodificarVin(vin: String) =
            if (falla) Result.failure(IllegalStateException("sin conexión"))
            else Result.success(DatosVin("Bmw", "X1", 2021))
    }

    @Test
    fun `al escribir la marca se sugieren sus modelos`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = VehicleViewModel(FakeVehicleRepository(), CatalogoFalso())
        viewModel.onMarcaChange("Nissan")
        advanceUntilIdle()
        assertEquals(listOf("March", "Sentra", "Versa"), viewModel.formState.value.modelosCatalogo)
    }

    @Test
    fun `el VIN llena marca, modelo y anio`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = VehicleViewModel(FakeVehicleRepository(), CatalogoFalso())
        viewModel.onVinChange("wba 11aa0-5m7h12345")
        viewModel.buscarPorVin()
        advanceUntilIdle()
        val estado = viewModel.formState.value
        assertEquals("WBA11AA05M7H12345", estado.vin)
        assertEquals("BMW", estado.marca)
        assertEquals("X1", estado.modelo)
        assertEquals("2021", estado.anio)
    }

    @Test
    fun `un VIN incompleto no se consulta`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = VehicleViewModel(FakeVehicleRepository(), CatalogoFalso())
        viewModel.onVinChange("ABC123")
        viewModel.buscarPorVin()
        assertTrue(viewModel.formState.value.vinMensaje!!.contains("17"))
        assertEquals("", viewModel.formState.value.marca)
    }

    @Test
    fun `sin conexion avisa y deja el formulario como estaba`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = VehicleViewModel(FakeVehicleRepository(), CatalogoFalso(falla = true))
        viewModel.onMarcaChange("Kia")
        viewModel.onVinChange("WBA11AA05M7H12345")
        viewModel.buscarPorVin()
        advanceUntilIdle()
        assertEquals("Kia", viewModel.formState.value.marca)
        assertTrue(viewModel.formState.value.vinMensaje!!.contains("conexión"))
    }
}
