package com.haydra.autocare.ui.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.haydra.autocare.ui.components.AutoCarePrimaryButton
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.MensajeError

/** Registro de cuenta (RF01). Al crearla se inicia sesión directamente. */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.registerState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) onRegisterSuccess()
    }
    if (currentUser != null) return

    PantallaAcceso(
        titulo = "Regístrate",
        subtitulo = "Solo necesitas tu nombre, un correo y una contraseña.",
        onBack = onNavigateToLogin,
        pie = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "¿Ya tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToLogin) { Text("Inicia sesión") }
            }
        }
    ) {
        AutoCareTextField(
            value = state.nombre,
            onValueChange = viewModel::onRegisterNombreChange,
            label = "Nombre",
            errorText = state.nombreError,
            leadingIcon = Icons.Filled.Person
        )
        AutoCareTextField(
            value = state.correo,
            onValueChange = viewModel::onRegisterCorreoChange,
            label = "Correo electrónico",
            errorText = state.correoError,
            keyboardType = KeyboardType.Email,
            leadingIcon = Icons.Filled.Email
        )
        AutoCareTextField(
            value = state.password,
            onValueChange = viewModel::onRegisterPasswordChange,
            label = "Contraseña",
            errorText = state.passwordError,
            isPassword = true,
            leadingIcon = Icons.Filled.Lock
        )
        AutoCareTextField(
            value = state.confirmacion,
            onValueChange = viewModel::onRegisterConfirmacionChange,
            label = "Repite la contraseña",
            errorText = state.confirmacionError,
            isPassword = true,
            leadingIcon = Icons.Filled.Lock,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::register
        )

        MensajeError(state.errorMessage)

        AutoCarePrimaryButton(
            text = "Crear cuenta",
            onClick = viewModel::register,
            loading = state.isLoading
        )
    }
}
