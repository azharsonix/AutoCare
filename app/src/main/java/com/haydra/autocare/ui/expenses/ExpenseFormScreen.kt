package com.haydra.autocare.ui.expenses

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import com.haydra.autocare.ui.common.Plantillas
import com.haydra.autocare.ui.common.formatMoneda
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.CampoFecha
import com.haydra.autocare.ui.components.CampoFoto
import com.haydra.autocare.ui.components.EliminarAccion
import com.haydra.autocare.ui.components.PantallaFormulario
import com.haydra.autocare.ui.components.SeccionFormulario
import com.haydra.autocare.ui.components.SugerenciasRow
import com.haydra.autocare.ui.vehicles.esGasolina

/** Alta y edición de un gasto (RF11-RF13). */
@Composable
fun ExpenseFormScreen(
    vehiculoId: String,
    viewModel: ExpenseViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    /** Se llama justo antes de eliminar (para ofrecer "Deshacer"). */
    onDeleted: () -> Unit = onSaved
) {
    val state by viewModel.formState.collectAsState()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }

    val esNuevo = state.editingExpenseId == null

    PantallaFormulario(
        titulo = if (esNuevo) "Registrar gasto" else "Editar gasto",
        textoGuardar = if (esNuevo) "Guardar" else "Guardar cambios",
        onGuardar = { viewModel.saveExpense(vehiculoId) },
        onBack = onBack,
        valores = with(state) { listOf(categoria, monto, fechaMillis, descripcion, litros, kilometraje, foto) },
        guardando = state.isSaving,
        guardado = state.savedSuccessfully,
        error = state.errorMessage,
        acciones = {
            state.editingExpenseId?.let { id ->
                EliminarAccion(
                    titulo = "Eliminar gasto",
                    mensaje = "Se eliminará este gasto de ${state.categoria}.",
                    onConfirm = {
                        onDeleted()
                        viewModel.deleteExpense(vehiculoId, id)
                    }
                )
            }
        }
    ) {
        SeccionFormulario("Gasto") {
            AutoCareTextField(
                value = state.categoria,
                onValueChange = viewModel::onCategoriaChange,
                label = "Categoría",
                errorText = state.categoriaError,
                leadingIcon = Icons.Filled.Category
            )
            SugerenciasRow(
                sugerencias = Plantillas.categoriasGasto,
                seleccionada = state.categoria,
                onSelect = viewModel::onCategoriaChange
            )
        }

        SeccionFormulario("Detalles") {
            AutoCareTextField(
                value = state.monto,
                onValueChange = viewModel::onMontoChange,
                label = "Monto",
                errorText = state.montoError,
                keyboardType = KeyboardType.Decimal,
                prefix = "$ ",
                leadingIcon = Icons.Filled.Payments
            )
            CampoFecha(
                fechaMillis = state.fechaMillis,
                onFechaChange = viewModel::onFechaChange,
                errorText = state.fechaError
            )
            AutoCareTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = "Notas (opcional)",
                singleLine = false,
                leadingIcon = Icons.AutoMirrored.Filled.Notes
            )
        }

        // Con litros y km de cada carga la app calcula el rendimiento (km/l).
        if (esGasolina(state.categoria)) {
            SeccionFormulario("Carga de gasolina") {
                AutoCareTextField(
                    value = state.litros,
                    onValueChange = viewModel::onLitrosChange,
                    label = "Litros (opcional)",
                    errorText = state.litrosError,
                    keyboardType = KeyboardType.Decimal,
                    suffix = "L",
                    leadingIcon = Icons.Filled.LocalGasStation
                )
                AutoCareTextField(
                    value = state.kilometraje,
                    onValueChange = viewModel::onKilometrajeChange,
                    label = "Kilometraje al cargar (opcional)",
                    errorText = state.kilometrajeError,
                    keyboardType = KeyboardType.Decimal,
                    suffix = "km",
                    leadingIcon = Icons.Filled.Speed
                )
                val precioLitro = state.monto.toDoubleOrNull()?.let { monto ->
                    state.litros.toDoubleOrNull()?.takeIf { it > 0 }?.let { monto / it }
                }
                Text(
                    text = buildString {
                        precioLitro?.let { append("${formatMoneda(it)} por litro. ") }
                        append("Llena el tanque en cada carga para que el rendimiento salga exacto.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SeccionFormulario("Comprobante") {
            CampoFoto(
                foto = state.foto,
                onFotoChange = viewModel::onFotoChange,
                textoVacio = "Guarda la foto del ticket o la factura (opcional)."
            )
        }
    }
}
