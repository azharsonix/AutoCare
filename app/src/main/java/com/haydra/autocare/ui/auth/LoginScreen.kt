package com.haydra.autocare.ui.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.haydra.autocare.ui.components.AutoCarePrimaryButton
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.MensajeError

/** Inicio de sesión (RF01). Al entrar se abre "Mis vehículos". */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val state by viewModel.loginState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) onLoginSuccess()
    }
    if (currentUser != null) return

    PantallaAcceso(
        titulo = "Te damos la bienvenida",
        subtitulo = "Lleva el control de los servicios, gastos y avisos de tus vehículos.",
        pie = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "¿No tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToRegister) { Text("Regístrate") }
            }
        }
    ) {
        AutoCareTextField(
            value = state.correo,
            onValueChange = viewModel::onLoginCorreoChange,
            label = "Correo electrónico",
            errorText = state.correoError,
            keyboardType = KeyboardType.Email,
            leadingIcon = Icons.Filled.Email
        )
        AutoCareTextField(
            value = state.password,
            onValueChange = viewModel::onLoginPasswordChange,
            label = "Contraseña",
            errorText = state.passwordError,
            isPassword = true,
            leadingIcon = Icons.Filled.Lock,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::login
        )
        TextButton(
            onClick = onNavigateToForgotPassword,
            modifier = Modifier.align(Alignment.End)
        ) { Text("¿Olvidaste tu contraseña?") }

        MensajeError(state.errorMessage)

        AutoCarePrimaryButton(
            text = "Iniciar sesión",
            onClick = viewModel::login,
            loading = state.isLoading
        )
    }
}
