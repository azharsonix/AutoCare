package com.haydra.autocare.ui.documentos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.haydra.autocare.notifications.DocumentoScheduler
import com.haydra.autocare.ui.common.Plantillas
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.CampoFecha
import com.haydra.autocare.ui.components.CampoFoto
import com.haydra.autocare.ui.components.EliminarAccion
import com.haydra.autocare.ui.components.PantallaFormulario
import com.haydra.autocare.ui.components.SeccionFormulario
import com.haydra.autocare.ui.components.SugerenciasRow

/** Alta y edición de un documento del vehículo. */
@Composable
fun DocumentoFormScreen(
    vehiculoId: String,
    nombreVehiculo: String,
    viewModel: DocumentoViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    /** Se llama justo antes de eliminar (para ofrecer "Deshacer"). */
    onDeleted: () -> Unit = onSaved
) {
    val state by viewModel.formState.collectAsState()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }

    val esNuevo = state.editingDocumentoId == null

    PantallaFormulario(
        titulo = if (esNuevo) "Agregar documento" else "Editar documento",
        textoGuardar = if (esNuevo) "Guardar" else "Guardar cambios",
        onGuardar = { viewModel.saveDocumento(vehiculoId, nombreVehiculo) },
        onBack = onBack,
        valores = with(state) { listOf(tipo, venceMillis, foto, notas) },
        guardando = state.isSaving,
        guardado = state.savedSuccessfully,
        error = state.errorMessage,
        acciones = {
            state.editingDocumentoId?.let { id ->
                EliminarAccion(
                    titulo = "Eliminar documento",
                    mensaje = "Se eliminará ${state.tipo} con su foto.",
                    onConfirm = {
                        onDeleted()
                        viewModel.deleteDocumento(vehiculoId, id)
                    }
                )
            }
        }
    ) {
        SeccionFormulario("Documento") {
            AutoCareTextField(
                value = state.tipo,
                onValueChange = viewModel::onTipoChange,
                label = "Qué documento es",
                errorText = state.tipoError,
                leadingIcon = Icons.Filled.Description
            )
            SugerenciasRow(
                sugerencias = Plantillas.tiposDocumento,
                seleccionada = state.tipo,
                onSelect = viewModel::onTipoChange
            )
            AutoCareTextField(
                value = state.notas,
                onValueChange = viewModel::onNotasChange,
                label = "Notas: n.º de póliza, aseguradora… (opcional)",
                singleLine = false,
                leadingIcon = Icons.AutoMirrored.Filled.Notes
            )
        }

        SeccionFormulario("Foto") {
            CampoFoto(
                foto = state.foto,
                onFotoChange = viewModel::onFotoChange,
                textoVacio = "Tómale foto al documento para tenerlo siempre a la mano."
            )
        }

        SeccionFormulario("Vencimiento") {
            CampoFecha(
                fechaMillis = state.venceMillis,
                onFechaChange = viewModel::onVenceChange,
                label = "Vence el (opcional)",
                onQuitar = { viewModel.onVenceChange(null) }
            )
            Text(
                text = "Te avisamos ${DocumentoScheduler.DIAS_ANTES} días antes y el mismo día.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
