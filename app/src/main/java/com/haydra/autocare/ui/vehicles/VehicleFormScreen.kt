package com.haydra.autocare.ui.vehicles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.haydra.autocare.ui.common.Plantillas
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.PantallaFormulario
import com.haydra.autocare.ui.components.SeccionFormulario
import com.haydra.autocare.ui.components.SugerenciasRow

/** Alta y edición de un vehículo (RF04, RF06). */
@Composable
fun VehicleFormScreen(
    usuarioId: String,
    viewModel: VehicleViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.formState.collectAsState()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }

    val esNuevo = state.editingVehicleId == null
    val guardar = { viewModel.saveVehicle(usuarioId) }

    PantallaFormulario(
        titulo = if (esNuevo) "Registrar vehículo" else "Editar vehículo",
        textoGuardar = if (esNuevo) "Guardar" else "Guardar cambios",
        onGuardar = guardar,
        onBack = onBack,
        valores = with(state) { listOf(marca, modelo, anio, color, placas, kilometraje) },
        guardando = state.isSaving,
        guardado = state.savedSuccessfully,
        error = state.errorMessage
    ) {
        SeccionFormulario("Vehículo") {
            AutoCareTextField(
                value = state.marca,
                onValueChange = viewModel::onMarcaChange,
                label = "Marca",
                errorText = state.marcaError,
                leadingIcon = Icons.Filled.DirectionsCar
            )
            SugerenciasRow(
                sugerencias = Plantillas.marcasVehiculo,
                seleccionada = state.marca,
                onSelect = viewModel::onMarcaChange
            )
            AutoCareTextField(
                value = state.modelo,
                onValueChange = viewModel::onModeloChange,
                label = "Modelo",
                errorText = state.modeloError,
                leadingIcon = Icons.Filled.Tag
            )
            AutoCareTextField(
                value = state.anio,
                onValueChange = viewModel::onAnioChange,
                label = "Año",
                errorText = state.anioError,
                keyboardType = KeyboardType.Number,
                leadingIcon = Icons.Filled.CalendarMonth
            )
            AutoCareTextField(
                value = state.color,
                onValueChange = viewModel::onColorChange,
                label = "Color",
                errorText = state.colorError,
                leadingIcon = Icons.Filled.Palette
            )
        }

        SeccionFormulario("Placas y kilometraje") {
            AutoCareTextField(
                value = state.placas,
                onValueChange = viewModel::onPlacasChange,
                label = "Placas",
                errorText = state.placasError,
                leadingIcon = Icons.Filled.ConfirmationNumber
            )
            AutoCareTextField(
                value = state.kilometraje,
                onValueChange = viewModel::onKilometrajeChange,
                label = "Kilometraje actual",
                errorText = state.kilometrajeError,
                keyboardType = KeyboardType.Number,
                suffix = "km",
                leadingIcon = Icons.Filled.Speed,
                imeAction = ImeAction.Done,
                onImeAction = guardar
            )
        }
    }
}
