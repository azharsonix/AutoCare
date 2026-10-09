package com.haydra.autocare.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Fila de lista que se elimina deslizándola hacia la izquierda. Al pasar el
 * umbral el fondo se vuelve rojo, el bote crece y el teléfono vibra; al
 * soltar se llama [onBorrar] (que muestra "Deshacer"). Para lectores de
 * pantalla, la acción "Eliminar" hace lo mismo sin el gesto.
 */
@Composable
fun DeslizarParaBorrar(
    posicion: PosicionEnGrupo,
    onBorrar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    // Basta con deslizar algo menos de la mitad de la fila.
    val estado = rememberSwipeToDismissBoxState(positionalThreshold = { distancia -> distancia * 0.4f })
    val haptica = LocalHapticFeedback.current
    val armado = estado.targetValue == SwipeToDismissBoxValue.EndToStart

    LaunchedEffect(armado) {
        if (armado) haptica.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    SwipeToDismissBox(
        state = estado,
        enableDismissFromStartToEnd = false,
        onDismiss = { onBorrar() },
        modifier = modifier.semantics {
            customActions = listOf(CustomAccessibilityAction("Eliminar") { onBorrar(); true })
        },
        backgroundContent = {
            val colores = MaterialTheme.colorScheme
            val fondo by animateColorAsState(if (armado) colores.errorContainer else colores.surfaceContainerHighest)
            val tinte by animateColorAsState(if (armado) colores.onErrorContainer else colores.onSurfaceVariant)
            val escala by animateFloatAsState(if (armado) 1.25f else 0.9f, MaterialTheme.motionScheme.fastSpatialSpec())
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(formaEnGrupo(posicion))
                    .background(fondo)
                    .padding(end = 24.dp)
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = tinte, modifier = Modifier.scale(escala))
            }
        },
        content = { contenido() }
    )
}
