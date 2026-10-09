package com.haydra.autocare.ui.components

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.haydra.autocare.AutoCareApplication
import com.haydra.autocare.data.local.AlmacenFotos
import kotlinx.coroutines.launch

@Composable
fun rememberAlmacenFotos(): AlmacenFotos {
    val context = LocalContext.current
    return remember { (context.applicationContext as AutoCareApplication).container.almacenFotos }
}

/**
 * Foto opcional de un registro (ticket, factura o documento): se toma con la
 * cámara o se elige de la galería. Con foto muestra la miniatura; tocarla la
 * abre en grande. [onFotoChange] recibe el nombre del archivo o null al quitarla.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CampoFoto(
    foto: String?,
    onFotoChange: (String?) -> Unit,
    textoVacio: String,
    modifier: Modifier = Modifier
) {
    val almacen = rememberAlmacenFotos()
    val scope = rememberCoroutineScope()
    var procesando by remember { mutableStateOf(false) }
    var verGrande by remember { mutableStateOf(false) }
    // Sobrevive si el sistema cierra la app mientras la cámara está abierta.
    var archivoCamara by rememberSaveable { mutableStateOf<String?>(null) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
        val nombre = archivoCamara ?: return@rememberLauncherForActivityResult
        archivoCamara = null
        procesando = true
        scope.launch {
            if (tomada) {
                almacen.terminarFotoDeCamara(nombre)?.let(onFotoChange)
            } else {
                almacen.archivo(nombre).delete()
            }
            procesando = false
        }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        procesando = true
        scope.launch {
            almacen.importar(uri)?.let(onFotoChange)
            procesando = false
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            procesando -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            foto != null -> Box {
                MiniaturaFoto(
                    foto = foto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable(onClickLabel = "Ver en grande") { verGrande = true }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    FilledTonalIconButton(onClick = { verGrande = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Filled.ZoomOutMap, contentDescription = "Ver en grande")
                    }
                    FilledTonalIconButton(onClick = { onFotoChange(null) }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar foto")
                    }
                }
            }
            else -> {
                Text(
                    text = textoVacio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            val (nombre, uri) = almacen.nuevoParaCamara()
                            archivoCamara = nombre
                            try {
                                camara.launch(uri)
                            } catch (_: ActivityNotFoundException) {
                                archivoCamara = null
                            }
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Tomar foto", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(
                        onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Galería", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (verGrande && foto != null) VisorFoto(foto = foto, onCerrar = { verGrande = false })
}

/** Miniatura recortada de una foto guardada; un fondo neutro mientras carga. */
@Composable
fun MiniaturaFoto(foto: String, modifier: Modifier = Modifier, lado: Int = 600) {
    val almacen = rememberAlmacenFotos()
    val imagen by produceState<Bitmap?>(initialValue = null, foto) { value = almacen.cargar(foto, lado) }
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        imagen?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** La foto a pantalla completa, con zoom de pellizco (para leer una póliza o mostrarla). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VisorFoto(foto: String, onCerrar: () -> Unit) {
    val almacen = rememberAlmacenFotos()
    val imagen by produceState<Bitmap?>(initialValue = null, foto) { value = almacen.cargar(foto, 2000) }
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }
    val gesto = rememberTransformableState { zoom, mover, _ ->
        escala = (escala * zoom).coerceIn(1f, 5f)
        desplazamiento = if (escala == 1f) Offset.Zero else desplazamiento + mover
    }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().transformable(gesto)) {
                imagen?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Foto",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = escala
                                scaleY = escala
                                translationX = desplazamiento.x
                                translationY = desplazamiento.y
                            }
                    )
                } ?: LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                IconButton(
                    onClick = onCerrar,
                    modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }
}
