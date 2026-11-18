package com.haydra.autocare.ui.inicio

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.haydra.autocare.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Proporción de las capas intro_*.png (todas comparten el mismo lienzo). */
private const val PROPORCION_LOGO = 900f / 504f

/** Centro de cada letra de "Care" dentro del lienzo, para que crezca desde su sitio. */
private val CENTROS_LETRAS = listOf(
    R.drawable.intro_c to TransformOrigin(0.149f, 0.617f),
    R.drawable.intro_a to TransformOrigin(0.408f, 0.644f),
    R.drawable.intro_r to TransformOrigin(0.647f, 0.644f),
    R.drawable.intro_e to TransformOrigin(0.869f, 0.644f)
)

private val SalidaRapida = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** Tiempo máximo que el logotipo espera a la app antes de abrirse de todos modos. */
private const val ESPERA_MAXIMA_MS = 1500L

/**
 * Animación de arranque con el logotipo. Tiene un propósito: cubrir el tiempo
 * en que se abre la base de datos y se revisa la sesión, para que la primera
 * pantalla aparezca ya lista y no en blanco o a medio cargar.
 *
 * El coche entra con estela, las letras de "Care" saltan una tras otra, la
 * barra de la "a" cae y las franjas se dibujan (menos de un segundo). En
 * cuanto [appLista] es verdadero el logotipo se abre hacia la app; no hay
 * pausa fija. Continúa la pantalla de inicio del sistema (mismo fondo), así
 * que no hay salto. Un toque la salta.
 *
 * [onLogoCompleto] avisa cuando termina el movimiento: es el momento de
 * componer la app debajo sin que la animación se trabe.
 */
@Composable
fun IntroAnimada(appLista: Boolean, onLogoCompleto: () -> Unit, onTerminar: () -> Unit) {
    val coche = remember { Animatable(0f) }
    val letras = remember { List(CENTROS_LETRAS.size) { Animatable(0f) } }
    val barra = remember { Animatable(0f) }
    val franjas = remember { Animatable(0f) }
    val salida = remember { Animatable(0f) }
    val tinte = ColorFilter.tint(colorResource(R.color.logo_inicio))
    val lista by rememberUpdatedState(appLista)

    LaunchedEffect(Unit) {
        launch { coche.animateTo(1f, tween(550, easing = SalidaRapida)) }
        delay(200)
        letras.forEachIndexed { i, letra ->
            launch {
                delay(i * 60L)
                letra.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 600f))
            }
        }
        delay(letras.size * 60L + 60)
        launch { barra.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 700f)) }
        franjas.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
        onLogoCompleto()
        // Se abre en cuanto la sesión y los datos están listos; si algo tarda de
        // más, no deja a la persona mirando el logotipo.
        withTimeoutOrNull(ESPERA_MAXIMA_MS) { snapshotFlow { lista }.first { it } }
        salida.animateTo(1f, tween(320, easing = FastOutLinearInEasing))
        onTerminar()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - salida.value
                // Sin búfer intermedio a pantalla completa: el fundido se aplica directo al dibujar.
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .background(colorResource(R.color.fondo_inicio))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTerminar)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .widthIn(max = 420.dp)
                .aspectRatio(PROPORCION_LOGO)
                .graphicsLayer {
                    val escala = 1f + 0.25f * salida.value
                    scaleX = escala
                    scaleY = escala
                }
        ) {
            // Estela: dos copias del coche que se quedan atrás y se desvanecen.
            listOf(0.55f to 0.25f, 0.3f to 0.45f).forEach { (atraso, opacidad) ->
                Capa(R.drawable.intro_coche, tinte) {
                    val v = coche.value
                    translationX = (v - 1f - atraso * (1f - v)) * size.width * 1.3f
                    alpha = opacidad * (1f - v)
                }
            }
            Capa(R.drawable.intro_coche, tinte) {
                val v = coche.value
                translationX = (v - 1f) * size.width * 1.3f
                alpha = v.coerceIn(0f, 1f)
            }

            CENTROS_LETRAS.forEachIndexed { i, (imagen, centro) ->
                Capa(imagen, tinte) {
                    val v = letras[i].value
                    transformOrigin = centro
                    scaleX = 0.4f + 0.6f * v
                    scaleY = 0.4f + 0.6f * v
                    translationY = (1f - v) * 40.dp.toPx()
                    alpha = v.coerceIn(0f, 1f)
                }
            }

            Capa(R.drawable.intro_barra, tinte) {
                val v = barra.value
                translationY = (v - 1f) * 120.dp.toPx()
                alpha = v.coerceIn(0f, 1f)
            }

            Image(
                painter = painterResource(R.drawable.intro_franjas),
                contentDescription = null,
                colorFilter = tinte,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        clipRect(right = size.width * franjas.value) { this@drawWithContent.drawContent() }
                    }
            )
        }
    }
}

@Composable
private fun Capa(@DrawableRes imagen: Int, tinte: ColorFilter, animacion: GraphicsLayerScope.() -> Unit) {
    Image(
        painter = painterResource(imagen),
        contentDescription = null,
        colorFilter = tinte,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Cada capa es una sola imagen: la transparencia no necesita búfer intermedio en la GPU.
                compositingStrategy = CompositingStrategy.ModulateAlpha
                animacion()
            }
    )
}
