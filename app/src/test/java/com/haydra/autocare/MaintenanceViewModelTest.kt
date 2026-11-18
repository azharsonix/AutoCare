package com.haydra.autocare

import com.haydra.autocare.data.repository.FakeMaintenanceRepository
import com.haydra.autocare.ui.maintenance.MaintenanceViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * Prueba de integración ViewModel + Repository (AUTOCARE.txt, sección 35)
 * del módulo de mantenimientos (RF08-RF10), usando FakeMaintenanceRepository
 * como sustituto de Firebase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehiculoId = "vehiculo-1"

    private fun fechaMillis(): Long =
        LocalDate.now().minusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `registrar mantenimiento lo agrega al listado del vehiculo`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = MaintenanceViewModel(FakeMaintenanceRepository())
        viewModel.startNewMaintenance()

        viewModel.onTipoChange("Cambio de aceite")
        viewModel.onFechaChange(fechaMillis())
        viewModel.onKilometrajeChange("12000")
        viewModel.onCostoChange("800")
        viewModel.saveMaintenance(vehiculoId)
        advanceUntilIdle()

        viewModel.loadMaintenances(vehiculoId)
        advanceUntilIdle()

        val mantenimientos = viewModel.listState.value.mantenimientos
        assertEquals(1, mantenimientos.size)
        assertEquals("Cambio de aceite", mantenimientos.first().tipo)
    }

    @Test
    fun `guardar sin fecha ni tipo marca errores y no guarda`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = MaintenanceViewModel(FakeMaintenanceRepository())
        viewModel.startNewMaintenance()

        viewModel.onKilometrajeChange("-5")
        viewModel.onCostoChange("abc")
        viewModel.saveMaintenance(vehiculoId)
        advanceUntilIdle()

        val estado = viewModel.formState.value
        assertNotNull(estado.tipoError)
        assertNotNull(estado.fechaError)
        assertNotNull(estado.kilometrajeError)
        assertNotNull(estado.costoError)
    }
}
