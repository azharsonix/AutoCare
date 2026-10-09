package com.haydra.autocare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Estructura común de los formularios de registro y edición (vehículo,
 * mantenimiento, gasto y recordatorio): barra superior, secciones con
 * [SeccionFormulario] y el botón de guardar fijo abajo, siempre visible
 * aunque el teclado esté abierto.
 *
 * [valores] son los campos editables: si difieren de como estaban al abrir
 * la pantalla, salir pide confirmación para no perder lo escrito.
 */
@Composable
fun PantallaFormulario(
    titulo: String,
    textoGuardar: String,
    onGuardar: () -> Unit,
    onBack: () -> Unit,
    valores: List<Any?>,
    guardando: Boolean,
    guardado: Boolean,
    error: String?,
    acciones: @Composable RowScope.() -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit
) {
    val iniciales = remember { valores }
    val salir = rememberSalidaSegura(hayCambios = !guardado && valores != iniciales, onSalir = onBack)

    Scaffold(
        topBar = { AutoCareTopBar(title = titulo, onBack = salir, actions = acciones) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        // Con teclado abierto manda su margen; si no, el de la barra de navegación (sin sumarlos).
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    MensajeError(error, modifier = Modifier.padding(bottom = 8.dp))
                    AutoCarePrimaryButton(text = textoGuardar, onClick = onGuardar, loading = guardando)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido
        )
    }
}

/** Un grupo de campos del formulario: título y tarjeta. */
@Composable
fun SeccionFormulario(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    TituloSeccion(titulo)
    TarjetaFormulario(contenido = contenido)
}
