package com.haydra.autocare.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.haydra.autocare.ui.components.AutoCarePrimaryButton
import com.haydra.autocare.ui.components.AutoCareTextField
import com.haydra.autocare.ui.components.AutoCareTopBar
import com.haydra.autocare.ui.components.ConfirmDialog
import com.haydra.autocare.ui.components.MensajeError
import com.haydra.autocare.ui.components.MensajeExito
import com.haydra.autocare.ui.components.TarjetaFormulario
import com.haydra.autocare.ui.components.TituloSeccion

/**
 * Perfil de la cuenta: datos personales, cambio de contraseña y privacidad (RF01-RF03).
 * Desde aquí se ejercen los derechos ARCO: corregir los datos y borrar la cuenta.
 * El nombre y el correo solo aparecen en sus campos; el avatar muestra la
 * inicial para dar identidad sin repetir el dato.
 */
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onVerAviso: () -> Unit = {},
    onCuentaBorrada: () -> Unit = {}
) {
    val perfil by viewModel.profileState.collectAsState()
    val contrasena by viewModel.changePasswordState.collectAsState()
    val borrado by viewModel.deleteAccountState.collectAsState()
    val usuario by viewModel.currentUser.collectAsState()
    var confirmarBorrado by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startEditingProfile()
    }
    // Al borrar la cuenta la sesión queda cerrada y se regresa al inicio de sesión.
    LaunchedEffect(usuario) {
        if (usuario == null) onCuentaBorrada()
    }

    // Guardar solo tiene sentido si algo cambió respecto a la cuenta.
    val hayCambiosPerfil = usuario?.let { perfil.nombre.trim() != it.nombre || perfil.correo.trim() != it.correo } ?: false

    Scaffold(topBar = { AutoCareTopBar(title = "Mi perfil", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Avatar(nombre = perfil.nombre.ifBlank { usuario?.nombre.orEmpty() })

            TituloSeccion("Datos personales")
            TarjetaFormulario {
                AutoCareTextField(
                    value = perfil.nombre,
                    onValueChange = viewModel::onProfileNombreChange,
                    label = "Nombre",
                    errorText = perfil.nombreError,
                    leadingIcon = Icons.Filled.Person
                )
                AutoCareTextField(
                    value = perfil.correo,
                    onValueChange = viewModel::onProfileCorreoChange,
                    label = "Correo electrónico",
                    errorText = perfil.correoError,
                    keyboardType = KeyboardType.Email,
                    leadingIcon = Icons.Filled.Email,
                    imeAction = ImeAction.Done,
                    onImeAction = { if (hayCambiosPerfil) viewModel.saveProfile() }
                )
                MensajeError(perfil.errorMessage)
                MensajeExito(perfil.successMessage)
                AutoCarePrimaryButton(
                    text = "Guardar cambios",
                    onClick = viewModel::saveProfile,
                    enabled = hayCambiosPerfil,
                    loading = perfil.isSaving
                )
            }

            TituloSeccion("Seguridad")
            TarjetaFormulario {
                AutoCareTextField(
                    value = contrasena.actual,
                    onValueChange = viewModel::onChangePasswordActualChange,
                    label = "Contraseña actual",
                    errorText = contrasena.actualError,
                    isPassword = true,
                    leadingIcon = Icons.Filled.Lock
                )
                AutoCareTextField(
                    value = contrasena.nueva,
                    onValueChange = viewModel::onChangePasswordNuevaChange,
                    label = "Nueva contraseña",
                    errorText = contrasena.nuevaError,
                    isPassword = true,
                    leadingIcon = Icons.Filled.LockReset
                )
                AutoCareTextField(
                    value = contrasena.confirmacion,
                    onValueChange = viewModel::onChangePasswordConfirmacionChange,
                    label = "Repite la contraseña",
                    errorText = contrasena.confirmacionError,
                    isPassword = true,
                    leadingIcon = Icons.Filled.LockReset,
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::changePassword
                )
                MensajeError(contrasena.errorMessage)
                MensajeExito(contrasena.successMessage)
                AutoCarePrimaryButton(
                    text = "Cambiar contraseña",
                    onClick = viewModel::changePassword,
                    loading = contrasena.isSaving
                )
            }

            TituloSeccion("Privacidad", accion = "Ver aviso", onAccion = onVerAviso)
            TarjetaFormulario {
                Text(
                    "Borrar tu cuenta elimina tus vehículos, registros y fotos del teléfono y de la nube. " +
                        "No se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AutoCareTextField(
                    value = borrado.password,
                    onValueChange = viewModel::onDeleteAccountPasswordChange,
                    label = "Escribe tu contraseña",
                    errorText = borrado.passwordError,
                    isPassword = true,
                    leadingIcon = Icons.Filled.Lock,
                    imeAction = ImeAction.Done,
                    onImeAction = { if (viewModel.canDeleteAccount()) confirmarBorrado = true }
                )
                MensajeError(borrado.errorMessage)
                OutlinedButton(
                    onClick = { if (viewModel.canDeleteAccount()) confirmarBorrado = true },
                    enabled = !borrado.isDeleting,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (borrado.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.error,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null)
                        Text("Borrar mi cuenta", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (confirmarBorrado) {
        ConfirmDialog(
            title = "¿Borrar tu cuenta?",
            message = "Se eliminarán tu cuenta y todos tus datos del teléfono y de la nube. No se puede deshacer.",
            confirmLabel = "Borrar cuenta",
            onConfirm = {
                confirmarBorrado = false
                viewModel.deleteAccount()
            },
            onDismiss = { confirmarBorrado = false }
        )
    }
}

@Composable
private fun Avatar(nombre: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = nombre.trim().firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

