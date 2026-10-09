package com.haydra.autocare.ui.expenses

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.repository.ExpenseRepository
import com.haydra.autocare.data.repository.FakeExpenseRepository
import com.haydra.autocare.data.validation.ExpenseValidator
import com.haydra.autocare.ui.common.aFechaUtc
import com.haydra.autocare.ui.common.aMillisUtc
import com.haydra.autocare.ui.common.formatCantidad
import com.haydra.autocare.ui.vehicles.esGasolina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ExpenseListUiState(
    val gastos: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class ExpenseFormUiState(
    val editingExpenseId: String? = null,
    val categoria: String = "",
    val monto: String = "",
    val fechaMillis: Long? = null,
    val descripcion: String = "",
    /** Solo se usan si la categoría es gasolina. */
    val litros: String = "",
    val kilometraje: String = "",
    val foto: String? = null,
    val categoriaError: String? = null,
    val montoError: String? = null,
    val fechaError: String? = null,
    val litrosError: String? = null,
    val kilometrajeError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
    val guardado: Expense? = null
)

/**
 * Administra el estado del listado, alta y edición de gastos (RF11-RF13) de un
 * vehículo, siguiendo el mismo patrón MVVM + Repository que
 * [com.haydra.autocare.ui.maintenance.MaintenanceViewModel].
 */
class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository = FakeExpenseRepository()
) : ViewModel() {

    private val _listState = MutableStateFlow(ExpenseListUiState())
    val listState: StateFlow<ExpenseListUiState> = _listState.asStateFlow()

    private val _formState = MutableStateFlow(ExpenseFormUiState())
    val formState: StateFlow<ExpenseFormUiState> = _formState.asStateFlow()

    fun loadExpenses(vehiculoId: String) {
        _listState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            expenseRepository.getExpenses(vehiculoId)
                .onSuccess { gastos -> _listState.value = ExpenseListUiState(gastos = gastos) }
                .onFailure { error -> _listState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    /** [fechaMillis] prellena la fecha (normalmente hoy) y [kilometraje] el km de una carga de gasolina. */
    fun startNewExpense(fechaMillis: Long? = null, kilometraje: Double? = null) {
        _formState.value = ExpenseFormUiState(
            fechaMillis = fechaMillis,
            kilometraje = kilometraje?.let(::formatCantidad).orEmpty()
        )
    }

    fun startEditingExpense(expense: Expense) {
        _formState.value = ExpenseFormUiState(
            editingExpenseId = expense.id,
            categoria = expense.categoria,
            monto = formatCantidad(expense.monto),
            fechaMillis = expense.fecha.aMillisUtc(),
            descripcion = expense.descripcion,
            litros = expense.litros?.let(::formatCantidad).orEmpty(),
            kilometraje = expense.kilometraje?.let(::formatCantidad).orEmpty(),
            foto = expense.foto
        )
    }

    fun onCategoriaChange(value: String) = _formState.update { it.copy(categoria = value, categoriaError = null, errorMessage = null) }
    fun onMontoChange(value: String) = _formState.update { it.copy(monto = value, montoError = null, errorMessage = null) }
    fun onFechaChange(millis: Long?) = _formState.update { it.copy(fechaMillis = millis, fechaError = null, errorMessage = null) }
    fun onDescripcionChange(value: String) = _formState.update { it.copy(descripcion = value, errorMessage = null) }
    fun onLitrosChange(value: String) = _formState.update { it.copy(litros = value, litrosError = null, errorMessage = null) }
    fun onKilometrajeChange(value: String) = _formState.update { it.copy(kilometraje = value, kilometrajeError = null, errorMessage = null) }
    fun onFotoChange(foto: String?) = _formState.update { it.copy(foto = foto, errorMessage = null) }

    fun saveExpense(vehiculoId: String) {
        val estado = _formState.value
        val categoriaError = ExpenseValidator.validateCategoria(estado.categoria)
        val montoError = ExpenseValidator.validateMonto(estado.monto)
        val fechaError = ExpenseValidator.validateFecha(estado.fechaMillis)
        val gasolina = esGasolina(estado.categoria)
        val litrosError = if (gasolina) ExpenseValidator.validateLitros(estado.litros) else null
        val kilometrajeError = if (gasolina) ExpenseValidator.validateKilometraje(estado.kilometraje) else null

        if (listOf(categoriaError, montoError, fechaError, litrosError, kilometrajeError).any { it != null }) {
            _formState.update {
                it.copy(
                    categoriaError = categoriaError,
                    montoError = montoError,
                    fechaError = fechaError,
                    litrosError = litrosError,
                    kilometrajeError = kilometrajeError
                )
            }
            return
        }

        _formState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val fecha = estado.fechaMillis!!.aFechaUtc()
            val monto = estado.monto.toDouble()
            // Litros y km solo tienen sentido en cargas de gasolina.
            val litros = if (gasolina) estado.litros.toDoubleOrNull() else null
            val kilometraje = if (gasolina) estado.kilometraje.toDoubleOrNull() else null

            val resultado = if (estado.editingExpenseId == null) {
                expenseRepository.insertExpense(
                    vehiculoId, estado.categoria, monto, fecha, estado.descripcion, litros, kilometraje, estado.foto
                )
            } else {
                expenseRepository.updateExpense(
                    Expense(
                        id = estado.editingExpenseId,
                        vehiculoId = vehiculoId,
                        categoria = estado.categoria.trim(),
                        monto = monto,
                        fecha = fecha,
                        descripcion = estado.descripcion.trim(),
                        litros = litros,
                        kilometraje = kilometraje,
                        foto = estado.foto
                    )
                )
            }

            resultado
                .onSuccess { guardado ->
                    _formState.value = ExpenseFormUiState(savedSuccessfully = true, guardado = guardado)
                    loadExpenses(vehiculoId)
                }
                .onFailure { error -> _formState.update { it.copy(isSaving = false, errorMessage = error.message) } }
        }
    }

    /** Vuelve a crear un gasto eliminado (acción "Deshacer"). */
    fun restaurarExpense(g: Expense) {
        viewModelScope.launch {
            expenseRepository.insertExpense(g.vehiculoId, g.categoria, g.monto, g.fecha, g.descripcion, g.litros, g.kilometraje, g.foto)
                .onSuccess { loadExpenses(g.vehiculoId) }
        }
    }

    fun deleteExpense(vehiculoId: String, id: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(id).onSuccess { loadExpenses(vehiculoId) }
        }
    }
}
