package com.haydra.autocare.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Lugar de una fila dentro de un grupo: decide qué esquinas se redondean. */
enum class PosicionEnGrupo {
    UNICA, PRIMERA, MEDIA, ULTIMA;

    companion object {
        fun de(indice: Int, total: Int): PosicionEnGrupo = when {
            total <= 1 -> UNICA
            indice == 0 -> PRIMERA
            indice == total - 1 -> ULTIMA
            else -> MEDIA
        }
    }
}

private val RADIO_GRUPO = 20.dp

/** Esquinas de una fila según su lugar en el bloque: solo las de los extremos se redondean. */
fun formaEnGrupo(posicion: PosicionEnGrupo): RoundedCornerShape {
    val arriba = if (posicion == PosicionEnGrupo.UNICA || posicion == PosicionEnGrupo.PRIMERA) RADIO_GRUPO else 0.dp
    val abajo = if (posicion == PosicionEnGrupo.UNICA || posicion == PosicionEnGrupo.ULTIMA) RADIO_GRUPO else 0.dp
    return RoundedCornerShape(topStart = arriba, topEnd = arriba, bottomStart = abajo, bottomEnd = abajo)
}

/**
 * Contenedor de una fila de lista agrupada: las filas consecutivas forman un
 * solo bloque redondeado, separadas por una línea fina que empieza en
 * [inicioDivisor] (alineada con el texto, no con el icono).
 */
@Composable
fun FilaEnGrupo(
    posicion: PosicionEnGrupo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colorFondo: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    inicioDivisor: Dp = 72.dp,
    contenido: @Composable () -> Unit
) {
    val forma = formaEnGrupo(posicion)
    val cuerpo: @Composable () -> Unit = {
        Column {
            if (posicion == PosicionEnGrupo.MEDIA || posicion == PosicionEnGrupo.ULTIMA) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = inicioDivisor)
                )
            }
            contenido()
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = forma, color = colorFondo, modifier = modifier.fillMaxWidth(), content = cuerpo)
    } else {
        Surface(shape = forma, color = colorFondo, modifier = modifier.fillMaxWidth(), content = cuerpo)
    }
}

/**
 * Fila estándar de un registro (mantenimiento, gasto, actividad): icono en
 * círculo, título, detalle y, a la derecha, el monto. Todas las listas usan
 * esta misma fila para que se lean igual; [posicion] la une a sus vecinas.
 */
@Composable
fun RegistroItem(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    modifier: Modifier = Modifier,
    monto: String? = null,
    colorIcono: Color = MaterialTheme.colorScheme.primaryContainer,
    colorSobreIcono: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    posicion: PosicionEnGrupo = PosicionEnGrupo.UNICA,
    /** Muestra un clip junto al título: el registro tiene foto del comprobante. */
    conFoto: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    FilaEnGrupo(posicion = posicion, modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = colorIcono, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = colorSobreIcono, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (conFoto) {
                        Icon(
                            Icons.Filled.AttachFile,
                            contentDescription = "Con comprobante",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp).size(16.dp)
                        )
                    }
                }
                if (detalle.isNotBlank()) {
                    Text(
                        text = detalle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            monto?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/** Título de un grupo de la lista (p. ej. "Septiembre 2026", "Vencidos") con un total opcional. */
@Composable
fun EncabezadoGrupo(titulo: String, modifier: Modifier = Modifier, total: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        total?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Encabezado de una lista: cuántos registros hay y, si aplica, el total. */
@Composable
fun ListaEncabezado(texto: String, total: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        total?.let {
            Text(
                "Total $it",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Botón de eliminar para la barra superior de una pantalla de edición, con
 * su diálogo de confirmación. Sustituye a los iconos de basura en cada fila.
 */
@Composable
fun EliminarAccion(titulo: String, mensaje: String, onConfirm: () -> Unit) {
    var confirmar by remember { mutableStateOf(false) }
    IconButton(onClick = { confirmar = true }) {
        Icon(Icons.Filled.DeleteOutline, contentDescription = titulo)
    }
    if (confirmar) {
        ConfirmDialog(
            title = titulo,
            message = mensaje,
            onConfirm = {
                confirmar = false
                onConfirm()
            },
            onDismiss = { confirmar = false }
        )
    }
}

/**
 * Botón "Exportar PDF" de la barra superior. Antes de generar pregunta si se
 * incluyen las fotos (documentos y comprobantes): el reporte suele
 * compartirse y no siempre conviene enviar, p. ej., la tarjeta de circulación.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BotonExportarPdf(exportando: Boolean, onExportar: (incluirFotos: Boolean) -> Unit) {
    var preguntar by remember { mutableStateOf(false) }
    var incluirFotos by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { preguntar = true }, enabled = !exportando) {
        if (exportando) {
            LoadingIndicator(modifier = Modifier.size(28.dp))
        } else {
            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exportar PDF")
        }
    }

    if (preguntar) {
        AlertDialog(
            onDismissRequest = { preguntar = false },
            icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
            title = { Text("Exportar reporte") },
            text = {
                Column {
                    Text("Genera un PDF con el historial del vehículo para guardarlo o compartirlo.")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .toggleable(value = incluirFotos, role = Role.Checkbox, onValueChange = { incluirFotos = it })
                    ) {
                        Checkbox(checked = incluirFotos, onCheckedChange = null)
                        Text("Incluir fotos de documentos y comprobantes", modifier = Modifier.padding(start = 12.dp))
                    }
                    AnimatedVisibility(visible = incluirFotos) {
                        Text(
                            text = "Las fotos pueden llevar datos personales (placas, dirección, póliza). " +
                                "Revisa con quién compartes el reporte.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    preguntar = false
                    onExportar(incluirFotos)
                }) { Text("Exportar") }
            },
            dismissButton = {
                TextButton(onClick = { preguntar = false }) { Text("Cancelar") }
            }
        )
    }
}
