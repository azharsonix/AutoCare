package com.haydra.autocare.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Avance del brillo (0 → 1) que comparten todos los huesos de un mismo esqueleto. */
private val LocalBrillo = staticCompositionLocalOf<State<Float>?> { null }

/**
 * Contenedor de un esqueleto de carga: dibuja la forma de la pantalla que viene
 * (bloques grises con un brillo que los recorre) en lugar de un indicador
 * girando, para que al llegar los datos nada salte de lugar. Para lectores de
 * pantalla es un solo elemento "Cargando".
 */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val brillo = rememberInfiniteTransition(label = "esqueleto").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "brillo"
    )
    CompositionLocalProvider(LocalBrillo provides brillo) {
        Box(modifier = modifier.clearAndSetSemantics { contentDescription = "Cargando" }) { contenido() }
    }
}

/**
 * Un bloque del esqueleto (texto, icono, tarjeta). El brillo se lee al dibujar,
 * así que la animación no recompone la pantalla.
 */
@Composable
fun Hueso(modifier: Modifier = Modifier, forma: Shape = MaterialTheme.shapes.small) {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    // En tema claro el brillo es blanco; en oscuro, un velo claro muy tenue.
    val luz = if (base.luminance() > 0.5f) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.07f)
    val brillo = LocalBrillo.current
    Box(
        modifier = modifier
            .clip(forma)
            .background(base)
            .drawWithContent {
                val avance = brillo?.value ?: return@drawWithContent
                val centro = (avance * 2.4f - 0.7f) * size.width
                val ancho = size.width * 0.35f
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, luz, Color.Transparent),
                        startX = centro - ancho,
                        endX = centro + ancho
                    )
                )
            }
    )
}

/** Línea de texto del esqueleto. */
@Composable
private fun LineaHueso(ancho: Float, alto: Dp = 12.dp) {
    Hueso(modifier = Modifier.fillMaxWidth(ancho).height(alto), forma = MaterialTheme.shapes.extraSmall)
}

/** Fila con la misma forma que [RegistroItem]: icono en círculo, título, detalle y monto. */
@Composable
private fun FilaEsqueleto(posicion: PosicionEnGrupo, anchoTitulo: Float) {
    FilaEnGrupo(posicion = posicion) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Hueso(modifier = Modifier.size(42.dp), forma = CircleShape)
            Column(
                modifier = Modifier.weight(1f).padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LineaHueso(ancho = anchoTitulo, alto = 14.dp)
                LineaHueso(ancho = 0.4f)
            }
            Hueso(modifier = Modifier.padding(start = 12.dp).width(56.dp).height(14.dp), forma = MaterialTheme.shapes.extraSmall)
        }
    }
}

/** Anchos distintos por fila para que el esqueleto no parezca una rejilla. */
private val ANCHOS_TITULO = listOf(0.7f, 0.5f, 0.62f, 0.45f, 0.58f)

/** Grupo de filas como el de las listas por mes. */
@Composable
private fun GrupoEsqueleto(filas: Int) {
    Column {
        repeat(filas) { i ->
            FilaEsqueleto(posicion = PosicionEnGrupo.de(i, filas), anchoTitulo = ANCHOS_TITULO[i % ANCHOS_TITULO.size])
        }
    }
}

/** Esqueleto de una lista de registros (servicios, gastos, avisos, documentos). */
@Composable
fun ListaEsqueleto(modifier: Modifier = Modifier, filas: Int = 5) {
    Esqueleto(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LineaHueso(ancho = 0.35f, alto = 14.dp)
            GrupoEsqueleto(filas)
        }
    }
}

/** Esqueleto de la pantalla de inicio: tarjeta del gasto del mes y tarjetas de vehículos. */
@Composable
fun InicioEsqueleto(modifier: Modifier = Modifier) {
    Esqueleto(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Hueso(modifier = Modifier.fillMaxWidth().height(112.dp), forma = MaterialTheme.shapes.large)
            Box(modifier = Modifier.padding(top = 8.dp)) { LineaHueso(ancho = 0.4f, alto = 16.dp) }
            repeat(2) {
                Hueso(modifier = Modifier.fillMaxWidth().height(96.dp), forma = MaterialTheme.shapes.large)
            }
        }
    }
}

/** Esqueleto del espacio de trabajo del vehículo: tarjeta azul del Resumen y actividad reciente. */
@Composable
fun ResumenEsqueleto(modifier: Modifier = Modifier) {
    Esqueleto(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Hueso(modifier = Modifier.fillMaxWidth().height(176.dp), forma = MaterialTheme.shapes.large)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Hueso(modifier = Modifier.weight(1f).height(72.dp), forma = MaterialTheme.shapes.medium)
                Hueso(modifier = Modifier.weight(1f).height(72.dp), forma = MaterialTheme.shapes.medium)
            }
            Box(modifier = Modifier.padding(top = 8.dp)) { LineaHueso(ancho = 0.45f, alto = 16.dp) }
            GrupoEsqueleto(filas = 3)
        }
    }
}
