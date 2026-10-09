package com.haydra.autocare.ui.plan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.haydra.autocare.data.plan.Limites
import com.haydra.autocare.ui.components.AutoCarePrimaryButton
import com.haydra.autocare.ui.components.AutoCareTopBar
import com.haydra.autocare.ui.components.MensajeError

private data class Beneficio(val nombre: String, val gratis: String, val pro: String)

private val beneficios = listOf(
    Beneficio("Vehículos", "${Limites.VEHICULOS_GRATIS}", "Ilimitados"),
    Beneficio("Historial", "${Limites.MESES_HISTORIAL_GRATIS} meses", "Completo"),
    Beneficio("Servicios, gastos y avisos", "✓", "✓"),
    Beneficio("Resumen y costo por km", "✓", "✓"),
    Beneficio("Reporte PDF", "✓", "✓")
)

/**
 * Pantalla de AutoCare Pro: qué incluye frente al plan Gratis y la compra.
 * [motivo] explica por qué se llegó aquí (p. ej. al intentar registrar un
 * segundo vehículo) para que el usuario entienda el límite.
 */
@Composable
fun ProScreen(
    state: PlanUiState,
    onBack: () -> Unit,
    onComprar: () -> Unit,
    onRestaurar: () -> Unit,
    onVolverAGratis: () -> Unit,
    motivo: String? = null,
    onAvisoMostrado: () -> Unit = {}
) {
    // Solo muestra sus propios avisos: si escuchara el buzón general se
    // quedaría con el "Ya tienes Pro" mientras la pantalla se cierra.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.aviso) {
        val aviso = state.aviso ?: return@LaunchedEffect
        // Se limpia después de mostrarlo: limpiarlo antes cambiaría la clave y cancelaría el snackbar.
        snackbarHostState.showSnackbar(aviso)
        onAvisoMostrado()
    }
    Scaffold(
        topBar = { AutoCareTopBar(title = "AutoCare Pro", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Encabezado(esPro = state.esPro)

            if (!state.esPro && motivo != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = motivo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            Comparativa()

            AnimatedContent(
                targetState = state.esPro,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "plan"
            ) { esPro ->
                if (esPro) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Tienes todas las funciones de Pro.",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (state.esDemostracion) {
                            OutlinedButton(onClick = onVolverAGratis, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Volver al plan Gratis (demostración)")
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Pago único · ${Limites.PRECIO_PRO}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tus datos siguen siendo tuyos en cualquier plan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        AutoCarePrimaryButton(
                            text = "Obtener Pro",
                            onClick = onComprar,
                            loading = state.procesando
                        )
                        MensajeError(state.errorMessage)
                        TextButton(onClick = onRestaurar, enabled = !state.procesando) {
                            Text("Restaurar compra")
                        }
                        if (state.esDemostracion) {
                            Text(
                                text = "Modo demostración: no se realiza ningún cobro.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Encabezado(esPro: Boolean) {
    val colores = MaterialTheme.colorScheme
    Card(
        shape = MaterialTheme.shapes.large,
        // Verde azulado (color "premium" de la paleta): el degradado azul es solo de la tarjeta del vehículo.
        colors = CardDefaults.cardColors(containerColor = colores.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(colores.onSecondaryContainer.copy(alpha = 0.1f), MaterialTheme.shapes.extraLarge)
            ) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = colores.onSecondaryContainer, modifier = Modifier.size(36.dp))
            }
            Text(
                text = if (esPro) "Eres Pro" else "Lleva todos tus autos",
                style = MaterialTheme.typography.headlineSmall,
                color = colores.onSecondaryContainer,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (esPro) {
                Text(
                    text = "Gracias por apoyar AutoCare.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun Comparativa() {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            FilaComparativa("", "Gratis", "Pro", encabezado = true)
            beneficios.forEach { beneficio ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                FilaComparativa(beneficio.nombre, beneficio.gratis, beneficio.pro)
            }
        }
    }
}

@Composable
private fun FilaComparativa(nombre: String, gratis: String, pro: String, encabezado: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nombre,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = gratis,
            style = if (encabezado) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = pro,
            style = if (encabezado) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(88.dp)
        )
    }
}
