package com.haydra.autocare.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.haydra.autocare.ui.common.formatFechaMillis

/**
 * Campo de fecha de los formularios: se ve como un campo de texto y al
 * tocarlo abre el calendario. [fechaMillis] usa el formato del selector de
 * Material (medianoche UTC, ver `aMillisUtc`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    fechaMillis: Long?,
    onFechaChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Fecha",
    errorText: String? = null,
    /** Si se indica, con fecha elegida aparece una ✕ para dejarla vacía. */
    onQuitar: (() -> Unit)? = null
) {
    var mostrarCalendario by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) mostrarCalendario = true
        }
    }

    OutlinedTextField(
        value = fechaMillis?.let(::formatFechaMillis).orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.Event, contentDescription = null) },
        trailingIcon = if (onQuitar != null && fechaMillis != null) {
            { IconButton(onClick = onQuitar) { Icon(Icons.Filled.Close, contentDescription = "Quitar fecha") } }
        } else {
            null
        },
        isError = errorText != null,
        // Sin error no se reserva espacio debajo del campo.
        supportingText = errorText?.let { { Text(it) } },
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    )

    if (mostrarCalendario) {
        val estadoCalendario = rememberDatePickerState(initialSelectedDateMillis = fechaMillis)
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    onFechaChange(estadoCalendario.selectedDateMillis)
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoCalendario)
        }
    }
}
