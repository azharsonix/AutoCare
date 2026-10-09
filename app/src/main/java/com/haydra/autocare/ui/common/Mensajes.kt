package com.haydra.autocare.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull

/** Aviso breve para el usuario, con una acción opcional (p. ej. "Deshacer"). */
data class Mensaje(
    val texto: String,
    val accion: String? = null,
    val alAccion: () -> Unit = {}
)

/**
 * Buzón de avisos de la app. Una pantalla deja el aviso al terminar una
 * acción (guardar, eliminar) y la pantalla a la que se vuelve lo muestra.
 * Guarda el último aviso hasta que alguien lo muestra, así no se pierde
 * durante la transición entre pantallas.
 */
class Mensajes {
    private val _pendiente = MutableStateFlow<Mensaje?>(null)
    val pendiente: StateFlow<Mensaje?> = _pendiente.asStateFlow()

    fun mostrar(mensaje: Mensaje) {
        _pendiente.value = mensaje
    }

    fun mostrar(texto: String) = mostrar(Mensaje(texto))

    fun consumir(mensaje: Mensaje) {
        _pendiente.compareAndSet(mensaje, null)
    }
}

/** Muestra en [snackbarHostState] los avisos pendientes de [mensajes]. */
@Composable
fun MostrarMensajes(mensajes: Mensajes, snackbarHostState: SnackbarHostState) {
    // Se recoge el flujo (y no se usa el aviso como clave del efecto): marcarlo
    // como mostrado no debe cancelar el snackbar que se está enseñando.
    LaunchedEffect(mensajes, snackbarHostState) {
        mensajes.pendiente.filterNotNull().collect { actual ->
            mensajes.consumir(actual)
            val resultado = snackbarHostState.showSnackbar(
                message = actual.texto,
                actionLabel = actual.accion,
                withDismissAction = actual.accion == null,
                duration = if (actual.accion != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (resultado == SnackbarResult.ActionPerformed) actual.alAccion()
        }
    }
}
