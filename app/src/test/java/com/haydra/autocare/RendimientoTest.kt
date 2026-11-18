package com.haydra.autocare

import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.ui.vehicles.calcularRendimiento
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RendimientoTest {

    private val hoy = LocalDate.of(2026, 9, 25)

    private fun carga(km: Double?, litros: Double?, categoria: String = "Gasolina") =
        Expense("g$km", "v1", categoria, 900.0, hoy, "", litros = litros, kilometraje = km)

    @Test
    fun `con una sola carga no hay rendimiento`() {
        assertNull(calcularRendimiento(listOf(carga(10_000.0, 40.0))))
    }

    @Test
    fun `los km entre cargas se recorren con los litros de la siguiente`() {
        val r = calcularRendimiento(listOf(carga(10_000.0, 40.0), carga(10_500.0, 40.0)))!!
        assertEquals(12.5, r.kmPorLitro, 0.001)
        assertEquals(12.5, r.ultimo, 0.001)
    }

    @Test
    fun `avisa cuando el ultimo tramo cae mas de 15 por ciento`() {
        val r = calcularRendimiento(
            listOf(carga(10_000.0, 40.0), carga(10_500.0, 40.0), carga(11_000.0, 40.0), carga(11_350.0, 40.0))
        )!!
        // Antes 12.5 km/l, ahora 8.75: bajó 30 %.
        assertNotNull(r.caida)
        assertEquals(0.30, r.caida!!, 0.001)
    }
}
