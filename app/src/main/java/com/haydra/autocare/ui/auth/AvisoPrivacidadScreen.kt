package com.haydra.autocare.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haydra.autocare.data.privacidad.AvisoPrivacidad
import com.haydra.autocare.ui.components.AutoCareTopBar
import com.haydra.autocare.ui.components.TarjetaFormulario

/** Aviso de privacidad: se abre desde el registro (antes de aceptarlo) y desde Mi perfil. */
@Composable
fun AvisoPrivacidadScreen(onBack: () -> Unit) {
    Scaffold(topBar = { AutoCareTopBar(title = "Aviso de privacidad", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Última actualización: ${AvisoPrivacidad.ACTUALIZADO}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TarjetaFormulario {
                AvisoPrivacidad.apartados.forEach { apartado ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            apartado.titulo,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            apartado.texto,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
