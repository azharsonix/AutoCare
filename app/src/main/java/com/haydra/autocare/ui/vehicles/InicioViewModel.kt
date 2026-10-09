package com.haydra.autocare.ui.vehicles

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haydra.autocare.data.model.Documento
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.data.repository.DocumentoRepository
import com.haydra.autocare.data.repository.ExpenseRepository
import com.haydra.autocare.data.repository.MaintenanceRepository
import com.haydra.autocare.data.repository.ReminderRepository
import com.haydra.autocare.ui.documentos.Vigencia
import com.haydra.autocare.ui.documentos.vigencia
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Un recordatorio vencido de cualquier vehículo, para "Requiere atención". */
@Immutable
data class Alerta(val vehiculo: Vehicle, val recordatorio: Reminder, val estado: EstadoRecordatorio)

/** Un documento vencido o por vencer de cualquier vehículo, también para "Requiere atención". */
@Immutable
data class AlertaDocumento(val vehiculo: Vehicle, val documento: Documento, val vigencia: Vigencia)

@Immutable
data class InicioUiState(
    val gastoMes: Double = 0.0,
    /** Próximo aviso aún no vencido por vehículo (los vencidos van en [alertas], no se repiten). */
    val proximos: Map<String, Pair<Reminder, EstadoRecordatorio.Pendiente>> = emptyMap(),
    val alertas: List<Alerta> = emptyList(),
    /** Los que vencen antes, primero. */
    val documentos: List<AlertaDocumento> = emptyList(),
    val cargado: Boolean = false
)

/** Datos crudos de un vehículo, tal como salen de los repositorios. */
data class DatosVehiculo(
    val mantenimientos: List<Maintenance>,
    val gastos: List<Expense>,
    val recordatorios: List<Reminder>,
    val documentos: List<Documento> = emptyList()
)

/**
 * Resume todos los vehículos para la pantalla de inicio. Cada dato sale una
 * sola vez: el gasto del mes entre todos, los avisos vencidos y los
 * documentos vencidos o por vencer (en "Requiere atención") y, por
 * vehículo, solo su próximo aviso que aún no vence.
 */
fun resumirInicio(
    vehiculos: List<Vehicle>,
    datos: Map<String, DatosVehiculo>,
    hoy: LocalDate = LocalDate.now()
): InicioUiState {
    var gastoMes = 0.0
    val alertas = mutableListOf<Alerta>()
    val proximos = mutableMapOf<String, Pair<Reminder, EstadoRecordatorio.Pendiente>>()
    val documentos = mutableListOf<AlertaDocumento>()
    vehiculos.forEach { vehiculo ->
        val d = datos[vehiculo.id] ?: return@forEach
        gastoMes += calcularEstadisticas(vehiculo, d.mantenimientos, d.gastos, d.recordatorios, hoy).gastoMes
        d.recordatorios.filter { !it.completado }.sortedBy { it.fecha }.forEach { recordatorio ->
            when (val estado = estadoRecordatorio(recordatorio, vehiculo.kilometraje, hoy)) {
                is EstadoRecordatorio.Vencido -> alertas += Alerta(vehiculo, recordatorio, estado)
                is EstadoRecordatorio.Pendiente -> proximos.putIfAbsent(vehiculo.id, recordatorio to estado)
                EstadoRecordatorio.Completado -> Unit
            }
        }
        d.documentos.forEach { documento ->
            val estado = vigencia(documento.vence, hoy)
            if (estado.urgente) documentos += AlertaDocumento(vehiculo, documento, estado)
        }
    }
    return InicioUiState(
        gastoMes = gastoMes,
        proximos = proximos,
        alertas = alertas.sortedBy { it.recordatorio.fecha },
        documentos = documentos.sortedBy { it.documento.vence },
        cargado = true
    )
}

/** Carga los datos de todos los vehículos del usuario para el inicio. */
class InicioViewModel(
    private val maintenanceRepository: MaintenanceRepository,
    private val expenseRepository: ExpenseRepository,
    private val reminderRepository: ReminderRepository,
    private val documentoRepository: DocumentoRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow(InicioUiState())
    val state: StateFlow<InicioUiState> = _state.asStateFlow()

    private var carga: Job? = null

    fun cargar(vehiculos: List<Vehicle>) {
        carga?.cancel()
        carga = viewModelScope.launch {
            val datos = vehiculos.associate { vehiculo ->
                vehiculo.id to DatosVehiculo(
                    mantenimientos = maintenanceRepository.getMaintenances(vehiculo.id).getOrDefault(emptyList()),
                    gastos = expenseRepository.getExpenses(vehiculo.id).getOrDefault(emptyList()),
                    recordatorios = reminderRepository.getReminders(vehiculo.id).getOrDefault(emptyList()),
                    documentos = documentoRepository?.getDocumentos(vehiculo.id)?.getOrNull().orEmpty()
                )
            }
            _state.value = resumirInicio(vehiculos, datos)
        }
    }
}
