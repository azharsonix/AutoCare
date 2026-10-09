package com.haydra.autocare.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AutoCareTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    prefix: String? = null,
    suffix: String? = null,
    imeAction: ImeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
    leadingIcon: ImageVector? = null,
    /** Qué hacer al pulsar "Listo" en el teclado (p. ej. enviar el formulario). */
    onImeAction: (() -> Unit)? = null
) {
    var contrasenaVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        // En campos numéricos se ignoran separadores de miles y espacios ("1,500" → "1500").
        onValueChange = { nuevo ->
            val esNumero = keyboardType == KeyboardType.Number || keyboardType == KeyboardType.Decimal
            onValueChange(if (esNumero) nuevo.filter { it.isDigit() || it == '.' } else nuevo)
        },
        label = { Text(label) },
        isError = errorText != null,
        // Sin error no se reserva espacio debajo del campo.
        supportingText = errorText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = if (onImeAction != null) KeyboardActions(onDone = { onImeAction() }) else KeyboardActions.Default,
        leadingIcon = leadingIcon?.let { icono -> { Icon(icono, contentDescription = null) } },
        prefix = prefix?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        visualTransformation = if (isPassword && !contrasenaVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                    Icon(
                        imageVector = if (contrasenaVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (contrasenaVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            }
        } else {
            null
        },
        singleLine = singleLine,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    )
}

/** Botón principal: al presionarlo pasa de píldora a esquinas más rectas (Expressive). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AutoCarePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (loading) {
            LoadingIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCareTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        actionLabel?.let {
            Button(onClick = onAction, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(top = 20.dp)) { Text(it) }
        }
    }
}

@Composable
fun AccountMenuButton(
    nombre: String,
    correo: String,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    esPro: Boolean = false,
    onOpenPro: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.AccountCircle, contentDescription = "Cuenta")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    correo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            onOpenPro?.let { abrirPro ->
                DropdownMenuItem(
                    text = { Text(if (esPro) "AutoCare Pro (activo)" else "Mejorar a Pro") },
                    leadingIcon = { Icon(Icons.Filled.WorkspacePremium, contentDescription = null) },
                    onClick = {
                        expanded = false
                        abrirPro()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Mi perfil") },
                leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEditProfile()
                }
            )
            DropdownMenuItem(
                text = { Text("Cerrar sesión") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                onClick = {
                    expanded = false
                    mostrarConfirmacion = true
                }
            )
        }
    }

    if (mostrarConfirmacion) {
        ConfirmDialog(
            title = "Cerrar sesión",
            message = "¿Seguro que quieres cerrar sesión?",
            confirmLabel = "Cerrar sesión",
            onConfirm = {
                mostrarConfirmacion = false
                onLogout()
            },
            onDismiss = { mostrarConfirmacion = false }
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Eliminar"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Fila deslizable de atajos: al tocar uno se rellena el campo. El que
 * coincide con [seleccionada] aparece marcado.
 */
@Composable
fun SugerenciasRow(
    sugerencias: List<String>,
    seleccionada: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(sugerencias) { sugerencia ->
            FilterChip(
                selected = sugerencia.equals(seleccionada.trim(), ignoreCase = true),
                onClick = { onSelect(sugerencia) },
                label = { Text(sugerencia) }
            )
        }
    }
}

/**
 * Salida de un formulario sin perder lo escrito por accidente: si
 * [hayCambios], el botón atrás (del sistema o de la barra) pregunta antes de
 * descartar. Devuelve la acción para el botón atrás de la barra superior.
 */
@Composable
fun rememberSalidaSegura(hayCambios: Boolean, onSalir: () -> Unit): () -> Unit {
    var preguntar by remember { mutableStateOf(false) }
    BackHandler(enabled = hayCambios) { preguntar = true }
    if (preguntar) {
        ConfirmDialog(
            title = "¿Descartar cambios?",
            message = "Lo que escribiste no se guardará.",
            confirmLabel = "Descartar",
            onConfirm = {
                preguntar = false
                onSalir()
            },
            onDismiss = { preguntar = false }
        )
    }
    return { if (hayCambios) preguntar = true else onSalir() }
}

/** Error general de un formulario (p. ej. "El correo ya está registrado"); no ocupa espacio si es null. */
@Composable
fun MensajeError(mensaje: String?, modifier: Modifier = Modifier) {
    mensaje?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(top = 8.dp)
        )
    }
}

/** Confirmación dentro de un formulario (p. ej. "Perfil actualizado"). */
@Composable
fun MensajeExito(mensaje: String?, modifier: Modifier = Modifier) {
    mensaje?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(top = 8.dp)
        )
    }
}

/** Título de una sección de pantalla, con un enlace opcional a la derecha (p. ej. "Ver historial"). */
@Composable
fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        accion?.let { TextButton(onClick = onAccion) { Text(it) } }
    }
}

/** Tarjeta que agrupa los campos de un formulario (acceso, perfil). */
@Composable
fun TarjetaFormulario(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido
        )
    }
}

/** Indicador de carga Expressive (forma que cambia), centrado en su contenedor. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CargandoContenido(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}
