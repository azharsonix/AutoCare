package com.haydra.autocare.ui.reminders

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.repository.FakeReminderRepository
import com.haydra.autocare.data.repository.ReminderRepository
import com.haydra.autocare.data.validation.ReminderValidator
import com.haydra.autocare.notifications.ReminderScheduler
import com.haydra.autocare.ui.common.Plantillas
import com.haydra.autocare.ui.common.aFechaUtc
import com.haydra.autocare.ui.common.aMillisUtc
import com.haydra.autocare.ui.common.formatCantidad
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ReminderListUiState(
    val recordatorios: List<Reminder> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class ReminderFormUiState(
    val editingReminderId: String? = null,
    val titulo: String = "",
    val fechaMillis: Long? = null,
    val kilometraje: String = "",
    val completado: Boolean = false,
    val tituloError: String? = null,
    val fechaError: String? = null,
    val kilometrajeError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
    val guardado: Reminder? = null
)

/**
 * Administra el estado del listado, alta y edición de recordatorios (RF15-RF16,
 * HU08) de un vehículo, siguiendo el mismo patrón MVVM + Repository que
 * [com.haydra.autocare.ui.maintenance.MaintenanceViewModel]. Cuando se provee
 * [scheduler], programa o cancela la notificación local del recordatorio al
 * guardarlo, completarlo o eliminarlo.
 */
class ReminderViewModel(
    private val reminderRepository: ReminderRepository = FakeReminderRepository(),
    private val scheduler: ReminderScheduler? = null
) : ViewModel() {

    private val _listState = MutableStateFlow(ReminderListUiState())
    val listState: StateFlow<ReminderListUiState> = _listState.asStateFlow()

    private val _formState = MutableStateFlow(ReminderFormUiState())
    val formState: StateFlow<ReminderFormUiState> = _formState.asStateFlow()

    fun loadReminders(vehiculoId: String) {
        _listState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            reminderRepository.getReminders(vehiculoId)
                .onSuccess { recordatorios -> _listState.value = ReminderListUiState(recordatorios = recordatorios) }
                .onFailure { error -> _listState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    /** Los parámetros prellenan el formulario (p. ej. "programar el próximo servicio"). */
    fun startNewReminder(titulo: String = "", fechaMillis: Long? = null, kilometraje: Double? = null) {
        _formState.value = ReminderFormUiState(
            titulo = titulo,
            fechaMillis = fechaMillis,
            kilometraje = kilometraje?.let { formatCantidad(it) } ?: ""
        )
    }

    /**
     * Elige una plantilla: pone el título y, si están vacíos, sugiere fecha y
     * kilometraje según el intervalo típico del servicio.
     */
    fun aplicarPlantilla(titulo: String, kilometrajeActual: Double?) {
        val intervalo = Plantillas.intervaloDe(titulo)
        _formState.update { estado ->
            estado.copy(
                titulo = titulo,
                tituloError = null,
                fechaMillis = estado.fechaMillis ?: intervalo?.meses?.let { fechaEnMeses(it) },
                fechaError = null,
                kilometraje = estado.kilometraje.ifBlank {
                    val km = intervalo?.km
                    if (km != null && kilometrajeActual != null) formatCantidad(kilometrajeActual + km) else ""
                },
                errorMessage = null
            )
        }
    }

    fun startEditingReminder(reminder: Reminder) {
        _formState.value = ReminderFormUiState(
            editingReminderId = reminder.id,
            titulo = reminder.titulo,
            fechaMillis = reminder.fecha.aMillisUtc(),
            kilometraje = reminder.kilometraje?.let { formatCantidad(it) } ?: "",
            completado = reminder.completado
        )
    }

    fun onTituloChange(value: String) = _formState.update { it.copy(titulo = value, tituloError = null, errorMessage = null) }
    fun onFechaChange(millis: Long?) = _formState.update { it.copy(fechaMillis = millis, fechaError = null, errorMessage = null) }
    fun onKilometrajeChange(value: String) = _formState.update { it.copy(kilometraje = value, kilometrajeError = null, errorMessage = null) }

    fun saveReminder(vehiculoId: String) {
        val estado = _formState.value
        val tituloError = ReminderValidator.validateTitulo(estado.titulo)
        val fechaError = ReminderValidator.validateFecha(estado.fechaMillis)
        val kilometrajeError = ReminderValidator.validateKilometraje(estado.kilometraje)

        if (listOf(tituloError, fechaError, kilometrajeError).any { it != null }) {
            _formState.update { it.copy(tituloError = tituloError, fechaError = fechaError, kilometrajeError = kilometrajeError) }
            return
        }

        _formState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val fecha = estado.fechaMillis!!.aFechaUtc()
            val kilometraje = estado.kilometraje.toDoubleOrNull()

            val resultado = if (estado.editingReminderId == null) {
                reminderRepository.insertReminder(vehiculoId, estado.titulo, fecha, kilometraje)
            } else {
                reminderRepository.updateReminder(
                    Reminder(
                        id = estado.editingReminderId,
                        vehiculoId = vehiculoId,
                        titulo = estado.titulo.trim(),
                        fecha = fecha,
                        kilometraje = kilometraje,
                        completado = estado.completado
                    )
                )
            }

            resultado
                .onSuccess { recordatorio ->
                    scheduler?.schedule(recordatorio)
                    _formState.value = ReminderFormUiState(savedSuccessfully = true, guardado = recordatorio)
                    loadReminders(vehiculoId)
                }
                .onFailure { error -> _formState.update { it.copy(isSaving = false, errorMessage = error.message) } }
        }
    }

    fun toggleCompletado(vehiculoId: String, reminder: Reminder) {
        viewModelScope.launch {
            val nuevoCompletado = !reminder.completado
            reminderRepository.setCompletado(reminder.id, nuevoCompletado)
                .onSuccess {
                    scheduler?.schedule(reminder.copy(completado = nuevoCompletado))
                    loadReminders(vehiculoId)
                }
        }
    }

    /**
     * Avisa de inmediato de los recordatorios cuyo kilometraje objetivo se
     * alcanzó al pasar el vehículo de [kmAnterior] a [kmNuevo].
     */
    fun avisarPorKilometraje(vehiculoId: String, kmAnterior: Double, kmNuevo: Double) {
        if (kmNuevo <= kmAnterior) return
        viewModelScope.launch {
            reminderRepository.getReminders(vehiculoId).onSuccess { recordatorios ->
                recordatorios
                    .filter { !it.completado && it.kilometraje != null && it.kilometraje > kmAnterior && it.kilometraje <= kmNuevo }
                    .forEach { scheduler?.notifyNow(it) }
            }
        }
    }

    /**
     * Marca como hechos los recordatorios pendientes llamados como [titulo]
     * (p. ej. al registrar ese mantenimiento). [onHecho] recibe cuántos fueron.
     */
    fun completarPorTitulo(vehiculoId: String, titulo: String, onHecho: (Int) -> Unit = {}) {
        viewModelScope.launch {
            reminderRepository.getReminders(vehiculoId).onSuccess { recordatorios ->
                val coinciden = recordatorios.filter { !it.completado && it.titulo.trim().equals(titulo.trim(), ignoreCase = true) }
                coinciden.forEach { recordatorio ->
                    reminderRepository.setCompletado(recordatorio.id, true)
                        .onSuccess { scheduler?.schedule(recordatorio.copy(completado = true)) }
                }
                if (coinciden.isNotEmpty()) loadReminders(vehiculoId)
                onHecho(coinciden.size)
            }
        }
    }

    /** Vuelve a crear un recordatorio eliminado (acción "Deshacer"), con su aviso. */
    fun restaurarReminder(r: Reminder) {
        viewModelScope.launch {
            reminderRepository.insertReminder(r.vehiculoId, r.titulo, r.fecha, r.kilometraje).onSuccess { nuevo ->
                if (r.completado) reminderRepository.setCompletado(nuevo.id, true)
                scheduler?.schedule(nuevo.copy(completado = r.completado))
                loadReminders(r.vehiculoId)
            }
        }
    }

    fun deleteReminder(vehiculoId: String, id: String) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(id).onSuccess {
                scheduler?.cancel(id)
                loadReminders(vehiculoId)
            }
        }
    }

    private fun fechaEnMeses(meses: Int): Long =
        LocalDate.now().plusMonths(meses.toLong()).aMillisUtc()
}
