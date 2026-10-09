package com.haydra.autocare.ui.plan

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haydra.autocare.data.plan.PlanRepository
import com.haydra.autocare.data.plan.ProveedorPagos
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class PlanUiState(
    val esPro: Boolean = false,
    val procesando: Boolean = false,
    val errorMessage: String? = null,
    /** Compra simulada: la pantalla lo avisa y permite volver al plan Gratis. */
    val esDemostracion: Boolean = true,
    /** Aviso para la propia pantalla Pro (p. ej. resultado de "Restaurar compra"). */
    val aviso: String? = null
)

/**
 * Plan de la cuenta con sesión (Gratis o Pro): lo observa en
 * [PlanRepository] y gestiona la compra a través de [ProveedorPagos].
 */
class PlanViewModel(
    private val planRepository: PlanRepository,
    private val pagos: ProveedorPagos
) : ViewModel() {

    private val _state = MutableStateFlow(PlanUiState(esDemostracion = pagos.esDemostracion))
    val state: StateFlow<PlanUiState> = _state.asStateFlow()

    private var usuarioId: String? = null
    private var observacion: Job? = null

    /** Empieza a seguir el plan de [usuarioId] (null al cerrar sesión). */
    fun observar(usuarioId: String?) {
        if (usuarioId == this.usuarioId) return
        this.usuarioId = usuarioId
        observacion?.cancel()
        _state.update { it.copy(esPro = false, errorMessage = null) }
        if (usuarioId == null) return
        observacion = viewModelScope.launch {
            planRepository.esPro(usuarioId).collect { pro -> _state.update { it.copy(esPro = pro) } }
        }
    }

    fun comprar(onExito: () -> Unit = {}) {
        val id = usuarioId ?: return
        if (_state.value.procesando) return
        _state.update { it.copy(procesando = true, errorMessage = null) }
        viewModelScope.launch {
            pagos.comprarPro()
                .onSuccess {
                    planRepository.activarPro(id)
                    _state.update { it.copy(procesando = false, esPro = true) }
                    onExito()
                }
                .onFailure { error ->
                    _state.update { it.copy(procesando = false, errorMessage = error.message ?: "No se pudo completar la compra") }
                }
        }
    }

    fun limpiarAviso() = _state.update { it.copy(aviso = null) }

    fun restaurar(onResultado: (Boolean) -> Unit = {}) {
        val id = usuarioId ?: return
        _state.update { it.copy(procesando = true, errorMessage = null) }
        viewModelScope.launch {
            pagos.restaurarCompra()
                .onSuccess { tenia ->
                    if (tenia) planRepository.activarPro(id)
                    _state.update {
                        it.copy(
                            procesando = false,
                            esPro = it.esPro || tenia,
                            aviso = if (tenia) "Compra restaurada: ya tienes Pro" else "No encontramos una compra anterior de Pro"
                        )
                    }
                    onResultado(tenia)
                }
                .onFailure { error ->
                    _state.update { it.copy(procesando = false, errorMessage = error.message ?: "No se pudo restaurar la compra") }
                }
        }
    }

    /** Solo en modo demostración: vuelve al plan Gratis para mostrar ambos estados. */
    fun volverAGratis() {
        val id = usuarioId ?: return
        if (!pagos.esDemostracion) return
        viewModelScope.launch { planRepository.quitarPro(id) }
    }
}
