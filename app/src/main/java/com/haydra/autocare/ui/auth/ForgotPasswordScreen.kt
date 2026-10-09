package com.haydra.autocare.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.haydra.autocare.ui.components.AutoCarePrimaryButton
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.MensajeError
import com.haydra.autocare.ui.components.MensajeExito

/**
 * Restablecer contraseña (HU02). Firebase Authentication envía al correo un enlace
 * donde el usuario elige su contraseña nueva; después vuelve a iniciar sesión.
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val estado by viewModel.forgotPasswordState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.clearForgotPassword() }
    }

    PantallaAcceso(
        titulo = "Recupera tu acceso",
        subtitulo = "Escribe el correo de tu cuenta y te enviaremos un enlace para elegir una contraseña nueva.",
        onBack = onBack
    ) {
        val enviadoA = estado.enviadoA
        if (enviadoA == null) {
            AutoCareTextField(
                value = estado.correo,
                onValueChange = viewModel::onForgotCorreoChange,
                label = "Correo electrónico",
                errorText = estado.correoError,
                keyboardType = KeyboardType.Email,
                leadingIcon = Icons.Filled.Email,
                imeAction = ImeAction.Done,
                onImeAction = viewModel::sendPasswordReset
            )

            MensajeError(estado.errorMessage)

            AutoCarePrimaryButton(
                text = "Enviar enlace",
                onClick = viewModel::sendPasswordReset,
                loading = estado.isSaving
            )
        } else {
            MensajeExito("Si $enviadoA tiene una cuenta, te llegará un enlace en unos minutos. Revisa también la carpeta de spam.")

            AutoCarePrimaryButton(
                text = "Volver a iniciar sesión",
                onClick = onBack
            )
        }
    }
}
