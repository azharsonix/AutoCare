package com.haydra.autocare.ui.maintenance

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.repository.FakeMaintenanceRepository
import com.haydra.autocare.data.repository.MaintenanceRepository
import com.haydra.autocare.data.validation.MaintenanceValidator
import com.haydra.autocare.ui.common.aFechaUtc
import com.haydra.autocare.ui.common.aMillisUtc
import com.haydra.autocare.ui.common.formatCantidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class MaintenanceListUiState(
    val mantenimientos: List<Maintenance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class MaintenanceFormUiState(
    val editingMaintenanceId: String? = null,
    val tipo: String = "",
    val fechaMillis: Long? = null,
    val kilometraje: String = "",
    val costo: String = "",
    val descripcion: String = "",
    val foto: String? = null,
    val tipoError: String? = null,
    val fechaError: String? = null,
    val kilometrajeError: String? = null,
    val costoError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
    /** El registro recién guardado, para ofrecer acciones de seguimiento. */
    val guardado: Maintenance? = null
)

/**
 * Administra el estado del listado, alta y edición de mantenimientos (RF08-RF10)
 * de un vehículo, siguiendo el mismo patrón MVVM + Repository que
 * [com.haydra.autocare.ui.vehicles.VehicleViewModel].
 */
class MaintenanceViewModel(
    private val maintenanceRepository: MaintenanceRepository = FakeMaintenanceRepository()
) : ViewModel() {

    private val _listState = MutableStateFlow(MaintenanceListUiState())
    val listState: StateFlow<MaintenanceListUiState> = _listState.asStateFlow()

    private val _formState = MutableStateFlow(MaintenanceFormUiState())
    val formState: StateFlow<MaintenanceFormUiState> = _formState.asStateFlow()

    fun loadMaintenances(vehiculoId: String) {
        _listState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            maintenanceRepository.getMaintenances(vehiculoId)
                .onSuccess { mantenimientos -> _listState.value = MaintenanceListUiState(mantenimientos = mantenimientos) }
                .onFailure { error -> _listState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    /** [fechaMillis] y [kilometraje] prellenan el formulario (hoy y el km actual del vehículo). */
    fun startNewMaintenance(fechaMillis: Long? = null, kilometraje: Double? = null) {
        _formState.value = MaintenanceFormUiState(
            fechaMillis = fechaMillis,
            kilometraje = kilometraje?.let { formatCantidad(it) } ?: ""
        )
    }

    fun startEditingMaintenance(maintenance: Maintenance) {
        _formState.value = MaintenanceFormUiState(
            editingMaintenanceId = maintenance.id,
            tipo = maintenance.tipo,
            fechaMillis = maintenance.fecha.aMillisUtc(),
            kilometraje = formatCantidad(maintenance.kilometraje),
            costo = formatCantidad(maintenance.costo),
            descripcion = maintenance.descripcion,
            foto = maintenance.foto
        )
    }

    fun onTipoChange(value: String) = _formState.update { it.copy(tipo = value, tipoError = null, errorMessage = null) }
    fun onFechaChange(millis: Long?) = _formState.update { it.copy(fechaMillis = millis, fechaError = null, errorMessage = null) }
    fun onKilometrajeChange(value: String) = _formState.update { it.copy(kilometraje = value, kilometrajeError = null, errorMessage = null) }
    fun onCostoChange(value: String) = _formState.update { it.copy(costo = value, costoError = null, errorMessage = null) }
    fun onDescripcionChange(value: String) = _formState.update { it.copy(descripcion = value, errorMessage = null) }
    fun onFotoChange(foto: String?) = _formState.update { it.copy(foto = foto, errorMessage = null) }

    fun saveMaintenance(vehiculoId: String) {
        val estado = _formState.value
        val tipoError = MaintenanceValidator.validateTipo(estado.tipo)
        val fechaError = MaintenanceValidator.validateFecha(estado.fechaMillis)
        val kilometrajeError = MaintenanceValidator.validateKilometraje(estado.kilometraje)
        val costoError = MaintenanceValidator.validateCosto(estado.costo)

        if (listOf(tipoError, fechaError, kilometrajeError, costoError).any { it != null }) {
            _formState.update {
                it.copy(
                    tipoError = tipoError,
                    fechaError = fechaError,
                    kilometrajeError = kilometrajeError,
                    costoError = costoError
                )
            }
            return
        }

        _formState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val fecha = estado.fechaMillis!!.aFechaUtc()
            val kilometraje = estado.kilometraje.toDouble()
            val costo = estado.costo.toDouble()

            val resultado = if (estado.editingMaintenanceId == null) {
                maintenanceRepository.insertMaintenance(vehiculoId, estado.tipo, fecha, kilometraje, costo, estado.descripcion, estado.foto)
            } else {
                maintenanceRepository.updateMaintenance(
                    Maintenance(
                        id = estado.editingMaintenanceId,
                        vehiculoId = vehiculoId,
                        tipo = estado.tipo.trim(),
                        fecha = fecha,
                        kilometraje = kilometraje,
                        costo = costo,
                        descripcion = estado.descripcion.trim(),
                        foto = estado.foto
                    )
                )
            }

            resultado
                .onSuccess { guardado ->
                    _formState.value = MaintenanceFormUiState(savedSuccessfully = true, guardado = guardado)
                    loadMaintenances(vehiculoId)
                }
                .onFailure { error -> _formState.update { it.copy(isSaving = false, errorMessage = error.message) } }
        }
    }

    /** Vuelve a crear un mantenimiento eliminado (acción "Deshacer"). */
    fun restaurarMaintenance(m: Maintenance) {
        viewModelScope.launch {
            maintenanceRepository.insertMaintenance(m.vehiculoId, m.tipo, m.fecha, m.kilometraje, m.costo, m.descripcion, m.foto)
                .onSuccess { loadMaintenances(m.vehiculoId) }
        }
    }

    fun deleteMaintenance(vehiculoId: String, id: String) {
        viewModelScope.launch {
            maintenanceRepository.deleteMaintenance(id).onSuccess { loadMaintenances(vehiculoId) }
        }
    }
}
