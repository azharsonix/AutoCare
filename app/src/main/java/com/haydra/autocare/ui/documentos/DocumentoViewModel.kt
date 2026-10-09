package com.haydra.autocare.ui.documentos

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haydra.autocare.data.model.Documento
import com.haydra.autocare.data.repository.DocumentoRepository
import com.haydra.autocare.notifications.DocumentoScheduler
import com.haydra.autocare.ui.common.aFechaUtc
import com.haydra.autocare.ui.common.aMillisUtc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class DocumentoListUiState(
    val documentos: List<Documento> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class DocumentoFormUiState(
    val editingDocumentoId: String? = null,
    val tipo: String = "",
    val venceMillis: Long? = null,
    val foto: String? = null,
    val notas: String = "",
    val tipoError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
    val guardado: Documento? = null
)

/**
 * Documentos de un vehículo: alta, edición y eliminación, y los avisos antes
 * de su vencimiento ([DocumentoScheduler]). Mismo patrón MVVM + Repository que
 * los demás módulos.
 */
class DocumentoViewModel(
    private val documentoRepository: DocumentoRepository,
    private val scheduler: DocumentoScheduler? = null
) : ViewModel() {

    private val _listState = MutableStateFlow(DocumentoListUiState())
    val listState: StateFlow<DocumentoListUiState> = _listState.asStateFlow()

    private val _formState = MutableStateFlow(DocumentoFormUiState())
    val formState: StateFlow<DocumentoFormUiState> = _formState.asStateFlow()

    fun loadDocumentos(vehiculoId: String) {
        _listState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            documentoRepository.getDocumentos(vehiculoId)
                .onSuccess { documentos -> _listState.value = DocumentoListUiState(documentos = documentos) }
                .onFailure { error -> _listState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    fun startNewDocumento() {
        _formState.value = DocumentoFormUiState()
    }

    fun startEditingDocumento(documento: Documento) {
        _formState.value = DocumentoFormUiState(
            editingDocumentoId = documento.id,
            tipo = documento.tipo,
            venceMillis = documento.vence?.aMillisUtc(),
            foto = documento.foto,
            notas = documento.notas
        )
    }

    fun onTipoChange(value: String) = _formState.update { it.copy(tipo = value, tipoError = null, errorMessage = null) }
    fun onVenceChange(millis: Long?) = _formState.update { it.copy(venceMillis = millis, errorMessage = null) }
    fun onFotoChange(foto: String?) = _formState.update { it.copy(foto = foto, errorMessage = null) }
    fun onNotasChange(value: String) = _formState.update { it.copy(notas = value, errorMessage = null) }

    /** [nombreVehiculo] va en el texto del aviso ("…de tu Toyota Corolla vence…"). */
    fun saveDocumento(vehiculoId: String, nombreVehiculo: String) {
        val estado = _formState.value
        if (estado.tipo.isBlank()) {
            _formState.update { it.copy(tipoError = "Indica qué documento es") }
            return
        }

        _formState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val vence = estado.venceMillis?.aFechaUtc()
            val resultado = if (estado.editingDocumentoId == null) {
                documentoRepository.insertDocumento(vehiculoId, estado.tipo, vence, estado.foto, estado.notas)
            } else {
                documentoRepository.updateDocumento(
                    Documento(
                        id = estado.editingDocumentoId,
                        vehiculoId = vehiculoId,
                        tipo = estado.tipo.trim(),
                        vence = vence,
                        foto = estado.foto,
                        notas = estado.notas.trim()
                    )
                )
            }

            resultado
                .onSuccess { guardado ->
                    scheduler?.schedule(guardado, nombreVehiculo)
                    _formState.value = DocumentoFormUiState(savedSuccessfully = true, guardado = guardado)
                    loadDocumentos(vehiculoId)
                }
                .onFailure { error -> _formState.update { it.copy(isSaving = false, errorMessage = error.message) } }
        }
    }

    /** Vuelve a crear un documento eliminado (acción "Deshacer"), con sus avisos. */
    fun restaurarDocumento(d: Documento, nombreVehiculo: String) {
        viewModelScope.launch {
            documentoRepository.insertDocumento(d.vehiculoId, d.tipo, d.vence, d.foto, d.notas).onSuccess { nuevo ->
                scheduler?.schedule(nuevo, nombreVehiculo)
                loadDocumentos(d.vehiculoId)
            }
        }
    }

    fun deleteDocumento(vehiculoId: String, id: String) {
        viewModelScope.launch {
            documentoRepository.deleteDocumento(id).onSuccess {
                scheduler?.cancel(id)
                loadDocumentos(vehiculoId)
            }
        }
    }
}
