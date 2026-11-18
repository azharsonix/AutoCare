package com.haydra.autocare.ui.vehicles

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haydra.autocare.data.catalogo.CatalogoVehiculos
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.data.repository.FakeVehicleRepository
import com.haydra.autocare.data.repository.VehicleRepository
import com.haydra.autocare.data.validation.VehicleValidator
import com.haydra.autocare.ui.common.Plantillas
import com.haydra.autocare.ui.common.formatCantidad
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val guardadoId: String? = null,
    /** Modelos de la marca y año escritos, según el catálogo vPIC (API de terceros). */
    val modelosCatalogo: List<String> = emptyList(),
    val vin: String = "",
    val buscandoVin: Boolean = false,
    val vinMensaje: String? = null
)

/**
 * Administra el estado de "Mis vehículos" (RF04-RF07): listado, formulario de
 * alta/edición y detalle, siguiendo el mismo patrón MVVM + Repository que
 * [com.haydra.autocare.ui.auth.AuthViewModel].
 */
class VehicleViewModel(
    private val vehicleRepository: VehicleRepository = FakeVehicleRepository(),
    private val catalogo: CatalogoVehiculos? = null
) : ViewModel() {

    private var busquedaModelos: Job? = null

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
        buscarModelos()
    }

    fun onMarcaChange(value: String) {
        _formState.update { it.copy(marca = value, marcaError = null, errorMessage = null) }
        buscarModelos()
    }
    fun onModeloChange(value: String) = _formState.update { it.copy(modelo = value, modeloError = null, errorMessage = null) }
    fun onAnioChange(value: String) {
        _formState.update { it.copy(anio = value, anioError = null, errorMessage = null) }
        buscarModelos()
    }
    fun onColorChange(value: String) = _formState.update { it.copy(color = value, colorError = null, errorMessage = null) }
    fun onPlacasChange(value: String) = _formState.update { it.copy(placas = value, placasError = null, errorMessage = null) }
    fun onKilometrajeChange(value: String) = _formState.update { it.copy(kilometraje = value, kilometrajeError = null, errorMessage = null) }

    fun onVinChange(value: String) = _formState.update {
        it.copy(vin = value.filter(Char::isLetterOrDigit).uppercase().take(VIN_LARGO), vinMensaje = null)
    }

    /**
     * Pide al catálogo vPIC la marca, el modelo y el año del número de serie y llena el formulario.
     * Si no hay conexión o el VIN no existe, solo muestra un aviso: el registro manual sigue igual.
     */
    fun buscarPorVin() {
        val catalogo = catalogo ?: return
        val vin = _formState.value.vin
        if (vin.length != VIN_LARGO) {
            _formState.update { it.copy(vinMensaje = "El número de serie tiene $VIN_LARGO caracteres") }
            return
        }
        _formState.update { it.copy(buscandoVin = true, vinMensaje = null) }
        viewModelScope.launch {
            catalogo.decodificarVin(vin)
                .onSuccess { datos ->
                    _formState.update {
                        it.copy(
                            marca = marcaConocida(datos.marca.ifEmpty { it.marca }),
                            modelo = datos.modelo.ifEmpty { it.modelo },
                            anio = datos.anio?.toString() ?: it.anio,
                            marcaError = null, modeloError = null, anioError = null,
                            buscandoVin = false,
                            vinMensaje = "Datos llenados con el número de serie. Revísalos antes de guardar."
                        )
                    }
                    buscarModelos()
                }
                .onFailure { error ->
                    val mensaje = error.message?.takeIf { "número de serie" in it }
                        ?: "No pudimos consultar el catálogo. Revisa tu conexión."
                    _formState.update { it.copy(buscandoVin = false, vinMensaje = mensaje) }
                }
        }
    }

    /** Espera a que el usuario deje de escribir y trae los modelos de esa marca y año. */
    private fun buscarModelos() {
        val catalogo = catalogo ?: return
        busquedaModelos?.cancel()
        val estado = _formState.value
        val marca = estado.marca.trim()
        if (marca.length < 2) {
            _formState.update { it.copy(modelosCatalogo = emptyList()) }
            return
        }
        busquedaModelos = viewModelScope.launch {
            delay(ESPERA_ESCRITURA_MS)
            val modelos = catalogo.modelos(marca, estado.anio.toIntOrNull()?.takeIf { it in 1950..2100 })
                .getOrDefault(emptyList())
            _formState.update { if (it.marca.trim() == marca) it.copy(modelosCatalogo = modelos) else it }
        }
    }

    /** Usa la escritura de la lista de marcas de la app ("BMW" en vez de "Bmw"). */
    private fun marcaConocida(marca: String) =
        (Plantillas.marcasVehiculo + MARCAS_CON_SIGLAS).firstOrNull { it.equals(marca, ignoreCase = true) } ?: marca

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

    private companion object {
        const val VIN_LARGO = 17
        const val ESPERA_ESCRITURA_MS = 600L

        /** Marcas que el catálogo escribe en mayúsculas y que no van con mayúscula inicial. */
        val MARCAS_CON_SIGLAS = listOf("BMW", "GMC", "MG", "BYD", "SEAT", "MINI", "RAM", "JAC", "Mercedes-Benz")
    }
}
