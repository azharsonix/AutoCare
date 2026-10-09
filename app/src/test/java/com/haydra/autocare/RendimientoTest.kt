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
    fun `el promedio pondera por litros y no depende del orden de captura`() {
        val r = calcularRendimiento(
            listOf(carga(11_000.0, 50.0), carga(10_000.0, 40.0), carga(10_500.0, 40.0))
        )!!
        // 1,000 km con 90 L.
        assertEquals(1_000.0 / 90.0, r.kmPorLitro, 0.001)
        assertEquals(10.0, r.ultimo, 0.001)
    }

    @Test
    fun `ignora otras categorias y cargas sin litros o sin km`() {
        val r = calcularRendimiento(
            listOf(
                carga(10_000.0, 40.0),
                carga(10_200.0, null),
                carga(null, 30.0),
                carga(10_300.0, 20.0, categoria = "Lavado"),
                carga(10_500.0, 40.0)
            )
        )!!
        assertEquals(12.5, r.kmPorLitro, 0.001)
    }

    @Test
    fun `descarta tramos imposibles por km mal escritos`() {
        val r = calcularRendimiento(listOf(carga(10_000.0, 40.0), carga(100_000.0, 40.0), carga(100_500.0, 40.0)))!!
        assertEquals(12.5, r.kmPorLitro, 0.001)
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

    @Test
    fun `una variacion pequena no se avisa`() {
        val r = calcularRendimiento(
            listOf(carga(10_000.0, 40.0), carga(10_500.0, 40.0), carga(11_000.0, 40.0), carga(11_460.0, 40.0))
        )!!
        assertNull(r.caida)
    }
}
