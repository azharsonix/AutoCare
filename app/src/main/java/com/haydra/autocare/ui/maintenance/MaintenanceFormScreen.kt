package com.haydra.autocare.ui.maintenance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import com.haydra.autocare.ui.common.Plantillas
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.CampoFecha
import com.haydra.autocare.ui.components.CampoFoto
import com.haydra.autocare.ui.components.EliminarAccion
import com.haydra.autocare.ui.components.PantallaFormulario
import com.haydra.autocare.ui.components.SeccionFormulario
import com.haydra.autocare.ui.components.SugerenciasRow

/** Alta y edición de un mantenimiento (RF08-RF10). */
@Composable
fun MaintenanceFormScreen(
    vehiculoId: String,
    viewModel: MaintenanceViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    /** Se llama justo antes de eliminar (para ofrecer "Deshacer"). */
    onDeleted: () -> Unit = onSaved
) {
    val state by viewModel.formState.collectAsState()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }

    val esNuevo = state.editingMaintenanceId == null

    PantallaFormulario(
        titulo = if (esNuevo) "Registrar mantenimiento" else "Editar mantenimiento",
        textoGuardar = if (esNuevo) "Guardar" else "Guardar cambios",
        onGuardar = { viewModel.saveMaintenance(vehiculoId) },
        onBack = onBack,
        valores = with(state) { listOf(tipo, fechaMillis, kilometraje, costo, descripcion, foto) },
        guardando = state.isSaving,
        guardado = state.savedSuccessfully,
        error = state.errorMessage,
        acciones = {
            state.editingMaintenanceId?.let { id ->
                EliminarAccion(
                    titulo = "Eliminar mantenimiento",
                    mensaje = "Se eliminará este registro de ${state.tipo}.",
                    onConfirm = {
                        onDeleted()
                        viewModel.deleteMaintenance(vehiculoId, id)
                    }
                )
            }
        }
    ) {
        SeccionFormulario("Servicio") {
            AutoCareTextField(
                value = state.tipo,
                onValueChange = viewModel::onTipoChange,
                label = "Tipo de mantenimiento",
                errorText = state.tipoError,
                leadingIcon = Icons.Filled.Build
            )
            SugerenciasRow(
                sugerencias = Plantillas.tiposMantenimiento,
                seleccionada = state.tipo,
                onSelect = viewModel::onTipoChange
            )
        }

        SeccionFormulario("Detalles") {
            CampoFecha(
                fechaMillis = state.fechaMillis,
                onFechaChange = viewModel::onFechaChange,
                errorText = state.fechaError
            )
            AutoCareTextField(
                value = state.kilometraje,
                onValueChange = viewModel::onKilometrajeChange,
                label = "Kilometraje",
                errorText = state.kilometrajeError,
                keyboardType = KeyboardType.Number,
                suffix = "km",
                leadingIcon = Icons.Filled.Speed
            )
            AutoCareTextField(
                value = state.costo,
                onValueChange = viewModel::onCostoChange,
                label = "Costo",
                errorText = state.costoError,
                keyboardType = KeyboardType.Decimal,
                prefix = "$ ",
                leadingIcon = Icons.Filled.Payments
            )
            AutoCareTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = "Notas (opcional)",
                singleLine = false,
                leadingIcon = Icons.AutoMirrored.Filled.Notes
            )
        }

        SeccionFormulario("Comprobante") {
            CampoFoto(
                foto = state.foto,
                onFotoChange = viewModel::onFotoChange,
                textoVacio = "Guarda la foto de la factura del taller: sirve para la garantía (opcional)."
            )
        }
    }
}
