package com.haydra.autocare.ui.documentos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haydra.autocare.data.model.Documento
import com.haydra.autocare.ui.components.CargandoContenido
import com.haydra.autocare.ui.components.DeslizarParaBorrar
import com.haydra.autocare.ui.components.EmptyState
import com.haydra.autocare.ui.components.FilaEnGrupo
import com.haydra.autocare.ui.components.MiniaturaFoto
import com.haydra.autocare.ui.components.PosicionEnGrupo
import com.haydra.autocare.ui.components.VisorFoto

/**
 * Pestaña "Docs" del vehículo: póliza, tarjeta de circulación, verificación…
 * Los que vencen antes aparecen primero. Tocar la foto la abre en grande
 * (para mostrarla, p. ej., a tránsito); tocar la fila abre su edición y
 * deslizarla a la izquierda lo elimina.
 */
@Composable
fun DocumentosTabContent(
    vehiculoId: String,
    viewModel: DocumentoViewModel,
    onEditDocumento: (Documento) -> Unit,
    onDeleteDocumento: (Documento) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.listState.collectAsState()
    var fotoAbierta by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vehiculoId) {
        viewModel.loadDocumentos(vehiculoId)
    }

    val documentos = state.documentos.filter { it.vehiculoId == vehiculoId }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && documentos.isEmpty() -> CargandoContenido()
            documentos.isEmpty() -> EmptyState(
                icon = Icons.Filled.Description,
                message = "Guarda tu póliza, tarjeta de circulación y verificación.\n" +
                    "Los tendrás a la mano y te avisamos antes de que venzan.",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)) {
                itemsIndexed(documentos, key = { _, documento -> documento.id }) { indice, documento ->
                    val posicion = PosicionEnGrupo.de(indice, documentos.size)
                    DeslizarParaBorrar(
                        posicion = posicion,
                        onBorrar = { onDeleteDocumento(documento) },
                        modifier = Modifier.animateItem()
                    ) {
                        FilaDocumento(
                            documento = documento,
                            posicion = posicion,
                            onVerFoto = { documento.foto?.let { fotoAbierta = it } },
                            onClick = { onEditDocumento(documento) }
                        )
                    }
                }
            }
        }
    }

    fotoAbierta?.let { VisorFoto(foto = it, onCerrar = { fotoAbierta = null }) }
}

@Composable
private fun FilaDocumento(
    documento: Documento,
    posicion: PosicionEnGrupo,
    onVerFoto: () -> Unit,
    onClick: () -> Unit
) {
    val estado = vigencia(documento.vence)
    FilaEnGrupo(posicion = posicion, onClick = onClick, inicioDivisor = 88.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val forma = MaterialTheme.shapes.medium
            if (documento.foto != null) {
                MiniaturaFoto(
                    foto = documento.foto,
                    lado = 200,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(forma)
                        .clickable(onClickLabel = "Ver foto", onClick = onVerFoto)
                )
            } else {
                Surface(shape = forma, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = documento.tipo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = estado.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (estado.urgente) FontWeight.SemiBold else null,
                    color = if (estado.urgente) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (documento.notas.isNotBlank()) {
                    Text(
                        text = documento.notas,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
