package com.haydra.autocare

import com.haydra.autocare.data.repository.FakeExpenseRepository
import com.haydra.autocare.ui.expenses.ExpenseViewModel
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
 * del módulo de gastos (RF11-RF13), usando FakeExpenseRepository como
 * sustituto de Supabase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehiculoId = "vehiculo-1"

    private fun fechaMillis(): Long =
        LocalDate.now().minusDays(3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `registrar gasto lo agrega al listado del vehiculo`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ExpenseViewModel(FakeExpenseRepository())
        viewModel.startNewExpense()

        viewModel.onCategoriaChange("Gasolina")
        viewModel.onMontoChange("500")
        viewModel.onFechaChange(fechaMillis())
        viewModel.saveExpense(vehiculoId)
        advanceUntilIdle()

        viewModel.loadExpenses(vehiculoId)
        advanceUntilIdle()

        val gastos = viewModel.listState.value.gastos
        assertEquals(1, gastos.size)
        assertEquals("Gasolina", gastos.first().categoria)
    }

    @Test
    fun `guardar con monto negativo marca error y no guarda`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ExpenseViewModel(FakeExpenseRepository())
        viewModel.startNewExpense()

        viewModel.onCategoriaChange("Gasolina")
        viewModel.onMontoChange("-100")
        viewModel.onFechaChange(fechaMillis())
        viewModel.saveExpense(vehiculoId)
        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.montoError)
        viewModel.loadExpenses(vehiculoId)
        advanceUntilIdle()
        assertTrue(viewModel.listState.value.gastos.isEmpty())
    }

    @Test
    fun `editar gasto actualiza sus datos`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ExpenseViewModel(FakeExpenseRepository())
        viewModel.startNewExpense()
        viewModel.onCategoriaChange("Gasolina")
        viewModel.onMontoChange("500")
        viewModel.onFechaChange(fechaMillis())
        viewModel.saveExpense(vehiculoId)
        advanceUntilIdle()

        val gasto = viewModel.listState.value.gastos.first()
        viewModel.startEditingExpense(gasto)
        viewModel.onMontoChange("750")
        viewModel.saveExpense(vehiculoId)
        advanceUntilIdle()

        val actualizado = viewModel.listState.value.gastos.first()
        assertEquals(750.0, actualizado.monto, 0.0)
    }
}
