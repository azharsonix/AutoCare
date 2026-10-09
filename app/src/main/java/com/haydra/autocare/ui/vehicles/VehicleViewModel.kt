package com.haydra.autocare.ui.vehicles

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.data.repository.FakeVehicleRepository
import com.haydra.autocare.data.repository.VehicleRepository
import com.haydra.autocare.data.validation.VehicleValidator
import com.haydra.autocare.ui.common.formatCantidad
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class VehicleListUiState(
    val vehiculos: List<Vehicle> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class VehicleFormUiState(
    val editingVehicleId: String? = null,
    val marca: String = "",
    val modelo: String = "",
    val anio: String = "",
    val color: String = "",
    val placas: String = "",
    val kilometraje: String = "",
    val marcaError: String? = null,
    val modeloError: String? = null,
    val anioError: String? = null,
    val colorError: String? = null,
    val placasError: String? = null,
    val kilometrajeError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
    /** Id del vehículo recién guardado (para abrirlo tras registrarlo). */
    val guardadoId: String? = null
)

/**
 * Administra el estado de "Mis vehículos" (RF04-RF07): listado, formulario de
 * alta/edición y detalle, siguiendo el mismo patrón MVVM + Repository que
 * [com.haydra.autocare.ui.auth.AuthViewModel].
 */
class VehicleViewModel(
    private val vehicleRepository: VehicleRepository = FakeVehicleRepository()
) : ViewModel() {

    private val _listState = MutableStateFlow(VehicleListUiState())
    val listState: StateFlow<VehicleListUiState> = _listState.asStateFlow()

    private val _formState = MutableStateFlow(VehicleFormUiState())
    val formState: StateFlow<VehicleFormUiState> = _formState.asStateFlow()

    /** Máximo de vehículos que puede registrar el usuario (null = sin límite, plan Pro). */
    var limiteVehiculos: Int? = null

    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()

    fun loadVehicles(usuarioId: String) {
        _listState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            vehicleRepository.getVehicles(usuarioId)
                .onSuccess { vehiculos -> _listState.value = VehicleListUiState(vehiculos = vehiculos) }
                .onFailure { error -> _listState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    fun loadVehicle(id: String) {
        viewModelScope.launch {
            vehicleRepository.getVehicle(id).onSuccess { _selectedVehicle.value = it }
        }
    }

    fun startNewVehicle() {
        _formState.value = VehicleFormUiState()
    }

    fun startEditingVehicle(vehicle: Vehicle) {
        _formState.value = VehicleFormUiState(
            editingVehicleId = vehicle.id,
            marca = vehicle.marca,
            modelo = vehicle.modelo,
            anio = vehicle.anio.toString(),
            color = vehicle.color,
            placas = vehicle.placas,
            kilometraje = formatCantidad(vehicle.kilometraje)
        )
    }

    fun onMarcaChange(value: String) = _formState.update { it.copy(marca = value, marcaError = null, errorMessage = null) }
    fun onModeloChange(value: String) = _formState.update { it.copy(modelo = value, modeloError = null, errorMessage = null) }
    fun onAnioChange(value: String) = _formState.update { it.copy(anio = value, anioError = null, errorMessage = null) }
    fun onColorChange(value: String) = _formState.update { it.copy(color = value, colorError = null, errorMessage = null) }
    fun onPlacasChange(value: String) = _formState.update { it.copy(placas = value, placasError = null, errorMessage = null) }
    fun onKilometrajeChange(value: String) = _formState.update { it.copy(kilometraje = value, kilometrajeError = null, errorMessage = null) }

    fun saveVehicle(usuarioId: String) {
        val estado = _formState.value
        val marcaError = VehicleValidator.validateMarca(estado.marca)
        val modeloError = VehicleValidator.validateModelo(estado.modelo)
        val anioError = VehicleValidator.validateAnio(estado.anio)
        val colorError = VehicleValidator.validateColor(estado.color)
        val placasError = VehicleValidator.validatePlacas(estado.placas)
        val kilometrajeError = VehicleValidator.validateKilometraje(estado.kilometraje)

        if (listOf(marcaError, modeloError, anioError, colorError, placasError, kilometrajeError).any { it != null }) {
            _formState.update {
                it.copy(
                    marcaError = marcaError,
                    modeloError = modeloError,
                    anioError = anioError,
                    colorError = colorError,
                    placasError = placasError,
                    kilometrajeError = kilometrajeError
                )
            }
            return
        }

        _formState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            // Límite del plan Gratis: solo al registrar, nunca al editar.
            val limite = limiteVehiculos
            if (estado.editingVehicleId == null && limite != null) {
                val actuales = vehicleRepository.getVehicles(usuarioId).getOrNull()?.size ?: 0
                if (actuales >= limite) {
                    _formState.update {
                        it.copy(isSaving = false, errorMessage = "Tu plan Gratis incluye $limite vehículo. Mejora a Pro para registrar más.")
                    }
                    return@launch
                }
            }
            val anio = estado.anio.toInt()
            val kilometraje = estado.kilometraje.toDouble()

            val resultado = if (estado.editingVehicleId == null) {
                vehicleRepository.insertVehicle(usuarioId, estado.marca, estado.modelo, anio, estado.color, estado.placas, kilometraje)
            } else {
                vehicleRepository.updateVehicle(
                    Vehicle(
                        id = estado.editingVehicleId,
                        usuarioId = usuarioId,
                        marca = estado.marca.trim(),
                        modelo = estado.modelo.trim(),
                        anio = anio,
                        color = estado.color.trim(),
                        placas = estado.placas.trim().uppercase(),
                        kilometraje = kilometraje,
                        kmActualizado = kmActualizadoAlEditar(estado.editingVehicleId, kilometraje)
                    )
                )
            }

            resultado
                .onSuccess { guardado ->
                    _formState.value = VehicleFormUiState(savedSuccessfully = true, guardadoId = guardado.id)
                    loadVehicles(usuarioId)
                }
                .onFailure { error -> _formState.update { it.copy(isSaving = false, errorMessage = error.message) } }
        }
    }

    /**
     * Cambia solo el kilometraje (acceso rápido desde "Resumen" y sincronización
     * al registrar un mantenimiento con más km). [onUpdated] recibe el km anterior.
     */
    /** Confirmar el mismo kilometraje también cuenta como "puesto al día". */
    fun updateKilometraje(vehicle: Vehicle, kilometraje: Double, onUpdated: (Double) -> Unit = {}) {
        val hoy = LocalDate.now()
        if (kilometraje == vehicle.kilometraje && vehicle.kmActualizado == hoy) return
        viewModelScope.launch {
            vehicleRepository.updateVehicle(vehicle.copy(kilometraje = kilometraje, kmActualizado = hoy))
                .onSuccess { actualizado ->
                    _selectedVehicle.value = actualizado
                    loadVehicles(vehicle.usuarioId)
                    if (kilometraje != vehicle.kilometraje) onUpdated(vehicle.kilometraje)
                }
        }
    }

    /** Al editar el vehículo se conserva la fecha del último km, salvo que cambie el km. */
    private fun kmActualizadoAlEditar(id: String, kilometraje: Double): LocalDate? {
        val anterior = _selectedVehicle.value?.takeIf { it.id == id }
            ?: _listState.value.vehiculos.find { it.id == id }
        return if (anterior == null || anterior.kilometraje != kilometraje) LocalDate.now() else anterior.kmActualizado
    }

    fun deleteVehicle(usuarioId: String, id: String) {
        viewModelScope.launch {
            vehicleRepository.deleteVehicle(id).onSuccess { loadVehicles(usuarioId) }
        }
    }
}
