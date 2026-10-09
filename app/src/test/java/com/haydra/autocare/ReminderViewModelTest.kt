package com.haydra.autocare

import com.haydra.autocare.data.repository.FakeReminderRepository
import com.haydra.autocare.ui.reminders.ReminderViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Prueba de integración ViewModel + Repository (AUTOCARE.txt, sección 35)
 * del módulo de recordatorios (RF15-RF16, HU08), usando FakeReminderRepository
 * como sustituto de Firebase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehiculoId = "vehiculo-1"

    private fun fechaMillis(): Long =
        LocalDate.now().plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `crear recordatorio lo agrega al listado del vehiculo`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ReminderViewModel(FakeReminderRepository())
        viewModel.startNewReminder()

        viewModel.onTituloChange("Cambio de aceite")
        viewModel.onFechaChange(fechaMillis())
        viewModel.saveReminder(vehiculoId)
        advanceUntilIdle()

        viewModel.loadReminders(vehiculoId)
        advanceUntilIdle()

        val recordatorios = viewModel.listState.value.recordatorios
        assertEquals(1, recordatorios.size)
        assertEquals("Cambio de aceite", recordatorios.first().titulo)
        assertEquals(false, recordatorios.first().completado)
    }

    @Test
    fun `crear sin titulo ni fecha marca errores y no guarda`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ReminderViewModel(FakeReminderRepository())
        viewModel.startNewReminder()

        viewModel.onKilometrajeChange("-10")
        viewModel.saveReminder(vehiculoId)
        advanceUntilIdle()

        val estado = viewModel.formState.value
        assertNotNull(estado.tituloError)
        assertNotNull(estado.fechaError)
        assertNotNull(estado.kilometrajeError)
    }

    @Test
    fun `marcar recordatorio como completado lo actualiza`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ReminderViewModel(FakeReminderRepository())
        viewModel.startNewReminder()
        viewModel.onTituloChange("Verificación")
        viewModel.onFechaChange(fechaMillis())
        viewModel.saveReminder(vehiculoId)
        advanceUntilIdle()

        val recordatorio = viewModel.listState.value.recordatorios.first()
        viewModel.toggleCompletado(vehiculoId, recordatorio)
        advanceUntilIdle()

        val actualizado = viewModel.listState.value.recordatorios.first()
        assertTrue(actualizado.completado)
    }
}
