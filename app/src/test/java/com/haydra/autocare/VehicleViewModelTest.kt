package com.haydra.autocare

import com.haydra.autocare.data.repository.FakeVehicleRepository
import com.haydra.autocare.ui.vehicles.VehicleViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Prueba de integración ViewModel + Repository (AUTOCARE.txt, sección 35)
 * del módulo de vehículos (RF04-RF07), usando FakeVehicleRepository como
 * sustituto de Firebase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun TestScope.crearVehiculo(viewModel: VehicleViewModel, usuarioId: String) {
        viewModel.startNewVehicle()
        viewModel.onMarcaChange("Toyota")
        viewModel.onModeloChange("Corolla")
        viewModel.onAnioChange("2022")
        viewModel.onColorChange("Rojo")
        viewModel.onPlacasChange("ABC-123")
        viewModel.onKilometrajeChange("15000")
        viewModel.saveVehicle(usuarioId)
        advanceUntilIdle()
    }

    @Test
    fun `registrar vehiculo lo agrega al listado del usuario`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = VehicleViewModel(FakeVehicleRepository())
        val usuarioId = "usuario-1"

        crearVehiculo(viewModel, usuarioId)
        viewModel.loadVehicles(usuarioId)
        advanceUntilIdle()

        val vehiculos = viewModel.listState.value.vehiculos
        assertEquals(1, vehiculos.size)
        assertEquals("Toyota", vehiculos.first().marca)
    }

    @Test
    fun `vehiculos de un usuario no se mezclan con los de otro`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeVehicleRepository()
        val viewModel = VehicleViewModel(repository)

        crearVehiculo(viewModel, "usuario-1")
        crearVehiculo(viewModel, "usuario-2")

        viewModel.loadVehicles("usuario-1")
        advanceUntilIdle()
        assertEquals(1, viewModel.listState.value.vehiculos.size)
    }

    @Test
    fun `editar vehiculo actualiza sus datos`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = VehicleViewModel(FakeVehicleRepository())
        val usuarioId = "usuario-1"
        crearVehiculo(viewModel, usuarioId)
        viewModel.loadVehicles(usuarioId)
        advanceUntilIdle()
        val vehiculo = viewModel.listState.value.vehiculos.first()

        viewModel.startEditingVehicle(vehiculo)
        viewModel.onKilometrajeChange("20000")
        viewModel.saveVehicle(usuarioId)
        advanceUntilIdle()

        val actualizado = viewModel.listState.value.vehiculos.first()
        assertEquals(20000.0, actualizado.kilometraje, 0.0)
    }

    @Test
    fun `eliminar vehiculo lo quita del listado`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = VehicleViewModel(FakeVehicleRepository())
        val usuarioId = "usuario-1"
        crearVehiculo(viewModel, usuarioId)
        viewModel.loadVehicles(usuarioId)
        advanceUntilIdle()
        val vehiculo = viewModel.listState.value.vehiculos.first()

        viewModel.deleteVehicle(usuarioId, vehiculo.id)
        advanceUntilIdle()

        assertTrue(viewModel.listState.value.vehiculos.isEmpty())
    }

    @Test
    fun `registrar con año invalido no guarda el vehiculo`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = VehicleViewModel(FakeVehicleRepository())
        val usuarioId = "usuario-1"

        viewModel.startNewVehicle()
        viewModel.onMarcaChange("Toyota")
        viewModel.onModeloChange("Corolla")
        viewModel.onAnioChange("3000")
        viewModel.onColorChange("Rojo")
        viewModel.onPlacasChange("ABC-123")
        viewModel.onKilometrajeChange("15000")
        viewModel.saveVehicle(usuarioId)
        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.anioError)
        viewModel.loadVehicles(usuarioId)
        advanceUntilIdle()
        assertTrue(viewModel.listState.value.vehiculos.isEmpty())
    }
}
