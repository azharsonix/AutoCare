package com.haydra.autocare.ui.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import com.haydra.autocare.ui.common.Plantillas
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.CampoFecha
import com.haydra.autocare.ui.components.EliminarAccion
import com.haydra.autocare.ui.components.PantallaFormulario
import com.haydra.autocare.ui.components.SeccionFormulario
import com.haydra.autocare.ui.components.SugerenciasRow

/** Alta y edición de un recordatorio (RF15-RF16, HU08). */
@Composable
fun ReminderFormScreen(
    vehiculoId: String,
    viewModel: ReminderViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    /** Se llama justo antes de eliminar (para ofrecer "Deshacer"). */
    onDeleted: () -> Unit = onSaved,
    /** Km actual del vehículo, para sugerir el kilometraje del aviso. */
    kilometrajeActual: Double? = null
) {
    val state by viewModel.formState.collectAsState()
    val context = LocalContext.current
    // El permiso de notificaciones se pide aquí, cuando el usuario crea un aviso,
    // y no al abrir la app: así entiende para qué es.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }

    val esNuevo = state.editingReminderId == null

    PantallaFormulario(
        titulo = if (esNuevo) "Crear recordatorio" else "Editar recordatorio",
        textoGuardar = if (esNuevo) "Guardar" else "Guardar cambios",
        onGuardar = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            viewModel.saveReminder(vehiculoId)
        },
        onBack = onBack,
        valores = with(state) { listOf(titulo, fechaMillis, kilometraje) },
        guardando = state.isSaving,
        guardado = state.savedSuccessfully,
        error = state.errorMessage,
        acciones = {
            state.editingReminderId?.let { id ->
                EliminarAccion(
                    titulo = "Eliminar recordatorio",
                    mensaje = "Se eliminará el recordatorio \"${state.titulo}\" y su aviso.",
                    onConfirm = {
                        onDeleted()
                        viewModel.deleteReminder(vehiculoId, id)
                    }
                )
            }
        }
    ) {
        SeccionFormulario("Recordatorio") {
            AutoCareTextField(
                value = state.titulo,
                onValueChange = viewModel::onTituloChange,
                label = "Título",
                errorText = state.tituloError,
                leadingIcon = Icons.Filled.Notifications
            )
            SugerenciasRow(
                sugerencias = Plantillas.titulosRecordatorio,
                seleccionada = state.titulo,
                onSelect = { viewModel.aplicarPlantilla(it, kilometrajeActual) }
            )
        }

        SeccionFormulario("Cuándo avisar") {
            CampoFecha(
                fechaMillis = state.fechaMillis,
                onFechaChange = viewModel::onFechaChange,
                errorText = state.fechaError
            )
            AutoCareTextField(
                value = state.kilometraje,
                onValueChange = viewModel::onKilometrajeChange,
                label = "Al llegar a (opcional)",
                errorText = state.kilometrajeError,
                keyboardType = KeyboardType.Number,
                suffix = "km",
                leadingIcon = Icons.Filled.Speed
            )
            Text(
                text = "Te avisamos en la fecha o al llegar al kilometraje, lo que ocurra primero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
