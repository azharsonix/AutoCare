package com.haydra.autocare.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.haydra.autocare.R
import com.haydra.autocare.data.local.AlmacenFotos
import com.haydra.autocare.data.model.Documento
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.ui.common.formatCantidad
import com.haydra.autocare.ui.common.formatFecha
import com.haydra.autocare.ui.common.formatKm
import com.haydra.autocare.ui.common.formatKmPorLitro
import com.haydra.autocare.ui.common.formatMoneda
import com.haydra.autocare.ui.documentos.vigencia
import com.haydra.autocare.ui.vehicles.CATEGORIA_MANTENIMIENTO
import com.haydra.autocare.ui.vehicles.EstadoRecordatorio
import com.haydra.autocare.ui.vehicles.calcularEstadisticas
import com.haydra.autocare.ui.vehicles.calcularRendimiento
import com.haydra.autocare.ui.vehicles.cifrasDeGasto
import com.haydra.autocare.ui.vehicles.estadoRecordatorio
import com.haydra.autocare.ui.vehicles.tramosRendimiento
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Todo lo que entra en el reporte de un vehículo. */
data class DatosReporte(
    val vehiculo: Vehicle,
    val propietario: String,
    val mantenimientos: List<Maintenance>,
    val gastos: List<Expense>,
    val recordatorios: List<Reminder>,
    val documentos: List<Documento> = emptyList(),
    /** Agrega al final un anexo con las fotos de documentos y comprobantes. */
    val incluirFotos: Boolean = false,
    val fecha: LocalDate = LocalDate.now()
)

/**
 * Genera el reporte PDF de un vehículo (tamaño carta/A4 en puntos) con
 * [PdfDocument] de Android, sin librerías externas: encabezado, datos del
 * vehículo, resumen de gastos, tablas de mantenimientos y gastos,
 * rendimiento de gasolina, recordatorios pendientes, documentos y, si se
 * pide, un anexo con las fotos. Las tablas continúan en la página siguiente
 * repitiendo su encabezado.
 */
class ReportePdf(private val context: Context) {

    private val almacen = AlmacenFotos(context)

    suspend fun generar(datos: DatosReporte): File = withContext(Dispatchers.IO) {
        val documento = PdfDocument()
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_autocare)
        try {
            Lienzo(documento, "${datos.vehiculo.marca} ${datos.vehiculo.modelo}", logo).apply {
                dibujarReporte(datos)
                if (datos.incluirFotos) anexoFotos(datos)
                terminar()
            }
            val carpeta = File(context.cacheDir, "reportes").apply { mkdirs() }
            // Solo se conserva el último reporte: son temporales, para compartir.
            carpeta.listFiles()?.forEach { it.delete() }
            val nombre = "AutoCare_${datos.vehiculo.marca}_${datos.vehiculo.modelo}_${datos.fecha}"
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
            File(carpeta, "$nombre.pdf").also { archivo ->
                archivo.outputStream().use { documento.writeTo(it) }
            }
        } finally {
            documento.close()
            logo.recycle()
        }
    }

    private fun Lienzo.dibujarReporte(datos: DatosReporte) {
        val v = datos.vehiculo
        val stats = calcularEstadisticas(v, datos.mantenimientos, datos.gastos, datos.recordatorios, datos.fecha)
        val gastosPropios = datos.gastos.filter { it.vehiculoId == v.id }
        val rendimiento = calcularRendimiento(gastosPropios)

        encabezado(formatFecha(datos.fecha))

        texto("${v.marca} ${v.modelo} ${v.anio}", MARGEN, y + 22f, Estilos.tituloVehiculo)
        texto(
            listOf("Placas ${v.placas}", v.color, "Propietario: ${datos.propietario}")
                .filter { it.isNotBlank() }
                .joinToString("  ·  "),
            MARGEN, y + 40f, Estilos.gris
        )
        y += 58f

        indicadores(
            buildList {
                add("Kilometraje" to "${formatKm(v.kilometraje)} km")
                cifrasDeGasto(stats, etiquetaAnio = "Gasto ${datos.fecha.year}", conMes = false).forEach { add(it.etiqueta to formatMoneda(it.monto)) }
                add("Costo por km" to (stats.costoPorKm?.let { formatMoneda(it) } ?: "—"))
                add("Rendimiento" to (rendimiento?.let { formatKmPorLitro(it.kmPorLitro) } ?: "—"))
            }
        )

        if (stats.categoriasAnio.size > 1) {
            seccion("¿En qué se va el dinero? (${datos.fecha.year})")
            val total = stats.categoriasAnio.sumOf { it.total }
            tabla(
                columnas = listOf(Columna("Categoría", 305f), Columna("Monto", 130f, true), Columna("%", 80f, true)),
                filas = stats.categoriasAnio.map {
                    listOf(it.nombre, formatMoneda(it.total), "${Math.round(it.total * 100 / total)} %")
                }
            )
        }

        val mantenimientos = datos.mantenimientos.filter { it.vehiculoId == v.id }.sortedByDescending { it.fecha }
        seccion("Mantenimientos (${mantenimientos.size})")
        tabla(
            columnas = listOf(
                Columna("Fecha", 75f),
                Columna("Tipo", 200f),
                Columna("Kilometraje", 110f, true),
                Columna("Costo", 130f, true)
            ),
            filas = mantenimientos.map {
                listOf(formatFecha(it.fecha), it.tipo, "${formatKm(it.kilometraje)} km", formatMoneda(it.costo))
            },
            total = listOf("", "Total", "", formatMoneda(mantenimientos.sumOf { it.costo }))
        )

        val gastos = gastosPropios.sortedByDescending { it.fecha }
        seccion("Gastos (${gastos.size})")
        tabla(
            columnas = listOf(
                Columna("Fecha", 75f),
                Columna("Categoría", 130f),
                Columna("Descripción", 180f),
                Columna("Monto", 130f, true)
            ),
            filas = gastos.map {
                val detalle = listOfNotNull(it.litros?.let { l -> "${formatCantidad(l)} L" }, it.descripcion.ifBlank { null })
                listOf(formatFecha(it.fecha), it.categoria, detalle.joinToString(" · ").ifBlank { "—" }, formatMoneda(it.monto))
            },
            total = listOf("", "Total", "", formatMoneda(gastos.sumOf { it.monto }))
        )

        // Solo si hay cargas con litros y km; si no, la sección no aporta nada.
        val tramos = tramosRendimiento(gastosPropios)
        if (tramos.isNotEmpty()) {
            seccion("Rendimiento de gasolina" + (rendimiento?.let { " · promedio ${formatKmPorLitro(it.kmPorLitro)}" } ?: ""))
            tabla(
                columnas = listOf(
                    Columna("Fecha", 75f),
                    Columna("Kilometraje", 100f, true),
                    Columna("Litros", 70f, true),
                    Columna("Recorrido", 95f, true),
                    Columna("Rendimiento", 85f, true),
                    Columna("Monto", 90f, true)
                ),
                filas = tramos.sortedByDescending { it.carga.kilometraje }.map { t ->
                    listOf(
                        formatFecha(t.carga.fecha),
                        "${formatKm(t.carga.kilometraje!!)} km",
                        "${formatCantidad(t.carga.litros!!)} L",
                        t.kmRecorridos?.let { "${formatKm(it)} km" } ?: "—",
                        t.kmPorLitro?.let(::formatKmPorLitro) ?: "—",
                        formatMoneda(t.carga.monto)
                    )
                }
            )
            rendimiento?.caida?.let { caida ->
                nota(
                    "Atención: la última carga rindió ${Math.round(caida * 100)} % menos que las anteriores.",
                    "Conviene revisar la presión de las llantas, el filtro de aire y las bujías."
                )
            }
        }

        val pendientes = datos.recordatorios
            .filter { it.vehiculoId == v.id && !it.completado }
            .sortedBy { it.fecha }
        seccion("Próximos servicios (${pendientes.size})")
        tabla(
            columnas = listOf(
                Columna("Recordatorio", 175f),
                Columna("Fecha", 75f),
                Columna("Kilometraje", 95f, true),
                Columna("Estado", 170f)
            ),
            filas = pendientes.map {
                listOf(
                    it.titulo,
                    formatFecha(it.fecha),
                    it.kilometraje?.let { km -> "${formatKm(km)} km" } ?: "—",
                    estadoCorto(estadoRecordatorio(it, v.kilometraje, datos.fecha))
                )
            }
        )

        val documentos = datos.documentos.filter { it.vehiculoId == v.id }
        if (documentos.isNotEmpty()) {
            seccion("Documentos (${documentos.size})")
            tabla(
                columnas = listOf(
                    Columna("Documento", 150f),
                    Columna("Vence", 75f),
                    Columna("Estado", 95f),
                    Columna("Notas", 195f)
                ),
                filas = documentos.map { d ->
                    val estado = vigencia(d.vence, datos.fecha)
                    listOf(
                        d.tipo,
                        d.vence?.let(::formatFecha) ?: "—",
                        when {
                            estado.vencido -> "Vencido"
                            estado.urgente -> estado.texto
                            d.vence == null -> "Sin vencimiento"
                            else -> "Vigente"
                        },
                        d.notas.ifBlank { "—" }
                    )
                }
            )
        }

        nota(
            "Los mantenimientos cuentan como gasto (categoría \"$CATEGORIA_MANTENIMIENTO\").",
            "El costo por km se calcula desde el primer kilometraje registrado en un mantenimiento.",
            "El rendimiento supone tanque lleno en cada carga: km recorridos desde la carga anterior entre los litros cargados."
        )
    }

    /**
     * Anexo con las fotos guardadas: primero los documentos, después los
     * comprobantes de servicios y gastos (los más recientes primero). Dos por
     * página, cada una con su descripción. Si no hay fotos no se agrega nada.
     */
    private suspend fun Lienzo.anexoFotos(datos: DatosReporte) {
        val v = datos.vehiculo
        val fotos = buildList {
            datos.documentos.filter { it.vehiculoId == v.id && it.foto != null }.forEach { d ->
                add(d.foto!! to listOfNotNull(d.tipo, d.vence?.let { "vence el ${formatFecha(it)}" }).joinToString(" · "))
            }
            datos.mantenimientos.filter { it.vehiculoId == v.id && it.foto != null }.sortedByDescending { it.fecha }.forEach { m ->
                add(m.foto!! to "Servicio · ${m.tipo} · ${formatFecha(m.fecha)} · ${formatMoneda(m.costo)}")
            }
            datos.gastos.filter { it.vehiculoId == v.id && it.foto != null }.sortedByDescending { it.fecha }.forEach { g ->
                add(g.foto!! to "Gasto · ${g.categoria} · ${formatFecha(g.fecha)} · ${formatMoneda(g.monto)}")
            }
        }
        if (fotos.isEmpty()) return

        nuevaHoja()
        seccion("Anexo: fotos (${fotos.size})")
        fotos.forEach { (nombre, descripcion) ->
            val imagen = almacen.cargar(nombre, LADO_FOTO_PDF) ?: return@forEach
            try {
                foto(imagen, descripcion)
            } finally {
                imagen.recycle()
            }
        }
    }
}

private const val ANCHO = 595f
private const val ALTO = 842f
private const val MARGEN = 40f
private const val ANCHO_UTIL = ANCHO - 2 * MARGEN
private const val LIMITE_INFERIOR = ALTO - 60f
/** Resolución de las fotos del anexo: se leen bien sin que el PDF pese demasiado. */
private const val LADO_FOTO_PDF = 1000

private val AZUL = Color.rgb(0x1F, 0x5F, 0xAE)
private val GRAFITO = Color.rgb(0x2B, 0x2D, 0x42)
private val TEXTO = Color.rgb(0x19, 0x1C, 0x20)
private val GRIS = Color.rgb(0x5D, 0x61, 0x69)
private val FONDO_SUAVE = Color.rgb(0xF3, 0xF3, 0xFA)
private val LINEA = Color.rgb(0xD9, 0xDC, 0xE4)

private object Estilos {
    fun texto(tamano: Float, color: Int = TEXTO, negrita: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tamano
        this.color = color
        typeface = Typeface.create(Typeface.DEFAULT, if (negrita) Typeface.BOLD else Typeface.NORMAL)
    }

    val subtituloBlanco = texto(10f, Color.WHITE)
    val tituloVehiculo = texto(18f, negrita = true)
    val seccion = texto(13f, GRAFITO, negrita = true)
    val normal = texto(9.5f)
    val negrita = texto(9.5f, negrita = true)
    val cabeceraTabla = texto(9f, Color.WHITE, negrita = true)
    val gris = texto(9.5f, GRIS)
    val pie = texto(8f, GRIS)
    val indicadorEtiqueta = texto(8.5f, GRIS)
    val indicadorValor = texto(13f, negrita = true)
}

private data class Columna(val titulo: String, val ancho: Float, val derecha: Boolean = false)

/** Página actual y posición vertical; se encarga de los saltos de página y del pie. */
private class Lienzo(
    private val documento: PdfDocument,
    private val nombreVehiculo: String,
    private val logo: Bitmap
) {
    private var numeroPagina = 0
    private var pagina: PdfDocument.Page = nuevaPagina()
    private val canvas: Canvas get() = pagina.canvas
    var y = MARGEN

    private fun nuevaPagina(): PdfDocument.Page {
        numeroPagina++
        val info = PdfDocument.PageInfo.Builder(ANCHO.toInt(), ALTO.toInt(), numeroPagina).create()
        return documento.startPage(info)
    }

    private fun cerrarPagina() {
        val pieY = ALTO - 28f
        canvas.drawLine(MARGEN, pieY - 12f, ANCHO - MARGEN, pieY - 12f, Paint().apply { color = LINEA })
        canvas.drawText("AutoCare · Reporte de $nombreVehiculo", MARGEN, pieY, Estilos.pie)
        val textoPagina = "Página $numeroPagina"
        canvas.drawText(textoPagina, ANCHO - MARGEN - Estilos.pie.measureText(textoPagina), pieY, Estilos.pie)
        documento.finishPage(pagina)
    }

    /** Pasa a otra página si no caben [alto] puntos más. Devuelve true si saltó. */
    fun asegurar(alto: Float): Boolean {
        if (y + alto <= LIMITE_INFERIOR) return false
        cerrarPagina()
        pagina = nuevaPagina()
        y = MARGEN
        return true
    }

    fun terminar() = cerrarPagina()

    /** Empieza una página nueva aunque quede sitio en la actual. */
    fun nuevaHoja() {
        cerrarPagina()
        pagina = nuevaPagina()
        y = MARGEN
    }

    /** Una foto con su descripción arriba, ajustada a media página sin deformarla. */
    fun foto(imagen: Bitmap, descripcion: String) {
        asegurar(ALTO_DESCRIPCION + ALTO_FOTO + 16f)
        y += 14f
        texto(descripcion, MARGEN, y, Estilos.negrita)
        y += 8f
        val escala = minOf(ANCHO_UTIL / imagen.width, ALTO_FOTO / imagen.height)
        val ancho = imagen.width * escala
        val alto = imagen.height * escala
        val x = MARGEN + (ANCHO_UTIL - ancho) / 2
        val marco = RectF(MARGEN, y, MARGEN + ANCHO_UTIL, y + ALTO_FOTO)
        canvas.drawRoundRect(marco, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FONDO_SUAVE })
        val destino = RectF(x, y + (ALTO_FOTO - alto) / 2, x + ancho, y + (ALTO_FOTO + alto) / 2)
        canvas.drawBitmap(imagen, null, destino, Paint(Paint.FILTER_BITMAP_FLAG))
        y += ALTO_FOTO + 16f
    }

    fun texto(valor: String, x: Float, base: Float, estilo: TextPaint, anchoMax: Float = ANCHO_UTIL) {
        val recortado = TextUtils.ellipsize(valor, estilo, anchoMax, TextUtils.TruncateAt.END).toString()
        canvas.drawText(recortado, x, base, estilo)
    }

    fun encabezado(fecha: String) {
        canvas.drawRect(0f, 0f, ANCHO, 92f, Paint().apply { color = AZUL })
        // Logotipo (blanco sobre la franja) a la izquierda; el texto, alineado a la derecha.
        val altoLogo = 64f
        val anchoLogo = altoLogo * logo.width / logo.height
        canvas.drawBitmap(logo, null, RectF(MARGEN, 14f, MARGEN + anchoLogo, 14f + altoLogo), Paint(Paint.FILTER_BITMAP_FLAG))
        val titulo = "Reporte del vehículo"
        canvas.drawText(titulo, ANCHO - MARGEN - Estilos.subtituloBlanco.measureText(titulo), 46f, Estilos.subtituloBlanco)
        val generado = "Generado el $fecha"
        canvas.drawText(generado, ANCHO - MARGEN - Estilos.subtituloBlanco.measureText(generado), 62f, Estilos.subtituloBlanco)
        y = 104f
    }

    fun indicadores(valores: List<Pair<String, String>>) {
        val separacion = 10f
        val ancho = (ANCHO_UTIL - separacion * (valores.size - 1)) / valores.size
        val alto = 50f
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FONDO_SUAVE }
        valores.forEachIndexed { i, (etiqueta, valor) ->
            val x = MARGEN + i * (ancho + separacion)
            canvas.drawRoundRect(RectF(x, y, x + ancho, y + alto), 8f, 8f, fondo)
            texto(etiqueta, x + 10f, y + 18f, Estilos.indicadorEtiqueta, ancho - 20f)
            texto(valor, x + 10f, y + 38f, Estilos.indicadorValor, ancho - 20f)
        }
        y += alto + 8f
    }

    fun seccion(titulo: String) {
        // El título nunca queda solo al final de la página: pide sitio para él y dos filas.
        asegurar(24f + ALTO_CABECERA + 2 * ALTO_FILA)
        y += 18f
        canvas.drawText(titulo, MARGEN, y, Estilos.seccion)
        y += 8f
    }

    fun tabla(columnas: List<Columna>, filas: List<List<String>>, total: List<String>? = null) {
        if (filas.isEmpty()) {
            canvas.drawText("Sin registros.", MARGEN, y + 14f, Estilos.gris)
            y += 22f
            return
        }
        cabecera(columnas)
        filas.forEachIndexed { indice, fila ->
            if (asegurar(ALTO_FILA)) cabecera(columnas)
            if (indice % 2 == 1) {
                canvas.drawRect(MARGEN, y, MARGEN + ANCHO_UTIL, y + ALTO_FILA, Paint().apply { color = FONDO_SUAVE })
            }
            celdas(columnas, fila, Estilos.normal)
            y += ALTO_FILA
        }
        total?.let {
            if (asegurar(ALTO_FILA + 2f)) cabecera(columnas)
            canvas.drawLine(MARGEN, y, MARGEN + ANCHO_UTIL, y, Paint().apply { color = GRAFITO; strokeWidth = 1f })
            celdas(columnas, it, Estilos.negrita)
            y += ALTO_FILA
        }
        y += 4f
    }

    private fun cabecera(columnas: List<Columna>) {
        canvas.drawRect(MARGEN, y, MARGEN + ANCHO_UTIL, y + ALTO_CABECERA, Paint().apply { color = GRAFITO })
        celdas(columnas, columnas.map { it.titulo }, Estilos.cabeceraTabla, ALTO_CABECERA)
        y += ALTO_CABECERA
    }

    private fun celdas(columnas: List<Columna>, valores: List<String>, estilo: TextPaint, alto: Float = ALTO_FILA) {
        var x = MARGEN
        val base = y + alto / 2 - (estilo.descent() + estilo.ascent()) / 2
        columnas.forEachIndexed { i, columna ->
            val anchoTexto = columna.ancho - 2 * RELLENO
            val valor = TextUtils.ellipsize(valores.getOrElse(i) { "" }, estilo, anchoTexto, TextUtils.TruncateAt.END).toString()
            val xTexto = if (columna.derecha) x + columna.ancho - RELLENO - estilo.measureText(valor) else x + RELLENO
            canvas.drawText(valor, xTexto, base, estilo)
            x += columna.ancho
        }
    }

    fun nota(vararg lineas: String) {
        asegurar(16f + lineas.size * 12f)
        y += 6f
        lineas.forEach { linea ->
            y += 12f
            texto(linea, MARGEN, y, Estilos.pie)
        }
    }

    private companion object {
        const val ALTO_FILA = 18f
        const val ALTO_CABECERA = 20f
        const val ALTO_DESCRIPCION = 22f
        const val ALTO_FOTO = 300f
        const val RELLENO = 6f
    }
}

/** En la tabla ya hay columnas de fecha y km: el estado solo dice si está vencido. */
private fun estadoCorto(estado: EstadoRecordatorio): String = when (estado) {
    is EstadoRecordatorio.Vencido -> if (estado.porKilometraje && !estado.porFecha) "Vencido (km)" else "Vencido"
    is EstadoRecordatorio.Pendiente -> "Pendiente"
    EstadoRecordatorio.Completado -> "Hecho"
}
