package com.haydra.autocare

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.haydra.autocare.data.export.DatosReporte
import com.haydra.autocare.data.export.ReportePdf
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.model.Vehicle
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportePdfTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val hoy = LocalDate.of(2026, 9, 25)
    private val vehiculo = Vehicle("v1", "u1", "Nissan", "Versa", 2020, "Gris", "ABC-123", 80_000.0)

    private fun paginasDe(datos: DatosReporte): Int = runBlocking {
        val archivo = ReportePdf(contexto).generar(datos)
        assertTrue(archivo.length() > 0)
        ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { it.pageCount }
        }
    }

    @Test
    fun reporte_sin_registros_cabe_en_una_pagina() {
        val paginas = paginasDe(DatosReporte(vehiculo, "Ana", emptyList(), emptyList(), emptyList(), fecha = hoy))
        assertTrue("páginas = $paginas", paginas == 1)
    }

    @Test
    fun reporte_largo_continua_en_varias_paginas() {
        val mantenimientos = (1..60).map { i ->
            Maintenance("m$i", "v1", "Servicio número $i con una descripción larga", hoy.minusDays(i * 7L), 20_000.0 + i * 1_000, 500.0 + i, "")
        }
        val gastos = (1..30).map { i ->
            Expense("g$i", "v1", if (i % 2 == 0) "Gasolina" else "Casetas", 300.0 + i, hoy.minusDays(i.toLong()), "Viaje $i")
        }
        val recordatorios = listOf(Reminder("r1", "v1", "Cambio de aceite", hoy.plusMonths(3), 85_000.0, false))

        val paginas = paginasDe(DatosReporte(vehiculo, "Ana", mantenimientos, gastos, recordatorios, fecha = hoy))
        assertTrue("páginas = $paginas", paginas >= 3)
    }
}
