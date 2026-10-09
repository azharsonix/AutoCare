package com.haydra.autocare.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.haydra.autocare.ui.components.AutoCarePrimaryButton
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.MensajeError

/**
 * Restablecer contraseña (HU02). Como todavía no existe un proyecto de
 * Supabase que envíe un correo de restablecimiento, se pide directamente el
 * correo y la nueva contraseña; cuando se conecte Supabase Auth esto se
 * reemplazará por el flujo real de enlace por correo.
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onResetSuccess: () -> Unit
) {
    val estado by viewModel.forgotPasswordState.collectAsState()

    LaunchedEffect(estado.savedSuccessfully) {
        if (estado.savedSuccessfully) onResetSuccess()
    }

    PantallaAcceso(
        titulo = "Recupera tu acceso",
        subtitulo = "Escribe el correo de tu cuenta y elige una contraseña nueva.",
        onBack = onBack
    ) {
        AutoCareTextField(
            value = estado.correo,
            onValueChange = viewModel::onForgotCorreoChange,
            label = "Correo electrónico",
            errorText = estado.correoError,
            keyboardType = KeyboardType.Email,
            leadingIcon = Icons.Filled.Email
        )
        AutoCareTextField(
            value = estado.nueva,
            onValueChange = viewModel::onForgotNuevaChange,
            label = "Nueva contraseña",
            errorText = estado.nuevaError,
            isPassword = true,
            leadingIcon = Icons.Filled.Lock
        )
        AutoCareTextField(
            value = estado.confirmacion,
            onValueChange = viewModel::onForgotConfirmacionChange,
            label = "Repite la contraseña",
            errorText = estado.confirmacionError,
            isPassword = true,
            leadingIcon = Icons.Filled.Lock,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::resetPassword
        )

        MensajeError(estado.errorMessage)

        AutoCarePrimaryButton(
            text = "Restablecer contraseña",
            onClick = viewModel::resetPassword,
            loading = estado.isSaving
        )
    }
}
