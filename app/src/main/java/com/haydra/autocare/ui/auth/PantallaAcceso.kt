package com.haydra.autocare.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.haydra.autocare.R
import com.haydra.autocare.ui.components.TarjetaFormulario

/**
 * Diseño común de las pantallas de acceso (iniciar sesión, registro y
 * restablecer contraseña): logotipo, título, subtítulo, el formulario en una
 * tarjeta y, debajo, los enlaces a las otras pantallas. [onBack] muestra la
 * flecha para volver (el inicio de sesión no la tiene).
 */
@Composable
fun PantallaAcceso(
    titulo: String,
    subtitulo: String,
    onBack: (() -> Unit)? = null,
    pie: @Composable ColumnScope.() -> Unit = {},
    formulario: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Espacio arriba para que el contenido no quede pegado a la flecha de volver.
            Spacer(Modifier.height(if (onBack != null) 48.dp else 40.dp))

            // El logotipo ya lleva el nombre de la app; se tiñe con el color de marca.
            Image(
                painter = painterResource(R.drawable.logo_autocare),
                contentDescription = "AutoCare",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.width(200.dp)
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            // En tabletas el formulario no se estira a todo el ancho.
            TarjetaFormulario(modifier = Modifier.widthIn(max = 480.dp), contenido = formulario)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp),
                content = pie
            )
        }

        onBack?.let {
            IconButton(onClick = it, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
    }
}
