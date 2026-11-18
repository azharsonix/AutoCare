package com.haydra.autocare.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fotos de tickets, facturas y documentos. Se guardan dentro de la app (no en
 * la galería), reducidas a [LADO_MAXIMO] px: suficiente para leerlas sin
 * llenar el teléfono. En la base solo se guarda el nombre del archivo.
 */
class AlmacenFotos(private val context: Context) {

    private val carpeta: File get() = File(context.filesDir, CARPETA).apply { mkdirs() }

    fun archivo(nombre: String): File = File(carpeta, nombre)

    /** Nombre y Uri de un archivo nuevo donde la cámara escribe la foto. */
    fun nuevoParaCamara(): Pair<String, Uri> {
        val nombre = "${UUID.randomUUID()}.jpg"
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo(nombre))
        return nombre to uri
    }

    /** Reduce y endereza la foto que dejó la cámara en [nombre]; null si no se llegó a tomar. */
    suspend fun terminarFotoDeCamara(nombre: String): String? = withContext(Dispatchers.IO) {
        val archivo = archivo(nombre)
        if (!archivo.exists() || archivo.length() == 0L) {
            archivo.delete()
            return@withContext null
        }
        val rotacion = runCatching { ExifInterface(archivo).rotacion() }.getOrDefault(0)
        val reducida = decodificarReducida({ archivo.inputStream() }, rotacion) ?: return@withContext null
        archivo.outputStream().use { reducida.compress(Bitmap.CompressFormat.JPEG, CALIDAD, it) }
        nombre
    }

    /** Copia, reducida, una imagen elegida de la galería. */
    suspend fun importar(uri: Uri): String? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val rotacion = runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it).rotacion() } }.getOrNull() ?: 0
        val reducida = decodificarReducida({ resolver.openInputStream(uri) ?: error("Sin imagen") }, rotacion)
            ?: return@withContext null
        val nombre = "${UUID.randomUUID()}.jpg"
        archivo(nombre).outputStream().use { reducida.compress(Bitmap.CompressFormat.JPEG, CALIDAD, it) }
        nombre
    }

    /** Imagen reducida a [lado] px para listas, formularios o el visor. */
    suspend fun cargar(nombre: String, lado: Int): Bitmap? = withContext(Dispatchers.IO) {
        val archivo = archivo(nombre)
        if (!archivo.exists()) return@withContext null
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.path, opciones)
        opciones.inSampleSize = muestreo(opciones.outWidth, opciones.outHeight, lado)
        opciones.inJustDecodeBounds = false
        BitmapFactory.decodeFile(archivo.path, opciones)
    }

    /**
     * Borra las fotos que ya no usa ningún registro (p. ej. las de un gasto
     * eliminado cuyo "Deshacer" ya pasó). Las del último día se conservan por
     * si son de un formulario que todavía no se guarda.
     */
    suspend fun limpiar(enUso: Set<String>) = withContext(Dispatchers.IO) {
        val limite = System.currentTimeMillis() - UN_DIA_MS
        carpeta.listFiles()
            ?.filter { it.name !in enUso && it.lastModified() < limite }
            ?.forEach { it.delete() }
    }

    /** Borra en ese momento las fotos indicadas (al borrar una cuenta no se espera a la limpieza diaria). */
    suspend fun borrar(nombres: List<String>) = withContext(Dispatchers.IO) {
        nombres.forEach { archivo(it).delete() }
    }

    private fun ExifInterface.rotacion(): Int = when (
        getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    private fun decodificarReducida(abrir: () -> InputStream, rotacion: Int): Bitmap? {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        abrir().use { BitmapFactory.decodeStream(it, null, opciones) }
        if (opciones.outWidth <= 0) return null
        opciones.inSampleSize = muestreo(opciones.outWidth, opciones.outHeight, LADO_MAXIMO)
        opciones.inJustDecodeBounds = false
        var bitmap = abrir().use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null
        val mayor = maxOf(bitmap.width, bitmap.height)
        if (mayor > LADO_MAXIMO) {
            val escala = LADO_MAXIMO.toFloat() / mayor
            bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true)
        }
        if (rotacion != 0) {
            val giro = Matrix().apply { postRotate(rotacion.toFloat()) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, giro, true)
        }
        return bitmap
    }

    private fun muestreo(ancho: Int, alto: Int, lado: Int): Int {
        var muestra = 1
        while (maxOf(ancho, alto) / (muestra * 2) >= lado) muestra *= 2
        return muestra
    }

    companion object {
        private const val CARPETA = "fotos"
        private const val LADO_MAXIMO = 1600
        private const val CALIDAD = 85
        private const val UN_DIA_MS = 24 * 60 * 60 * 1000L
    }
}
