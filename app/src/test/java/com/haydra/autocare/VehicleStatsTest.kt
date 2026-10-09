package com.haydra.autocare

import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.ui.vehicles.CATEGORIA_MANTENIMIENTO
import com.haydra.autocare.ui.vehicles.EstadoRecordatorio
import com.haydra.autocare.ui.vehicles.calcularEstadisticas
import com.haydra.autocare.ui.vehicles.estadoRecordatorio
import com.haydra.autocare.ui.vehicles.textoAviso
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleStatsTest {

    private val hoy = LocalDate.of(2026, 9, 25)
    private val vehiculo = Vehicle("v1", "u1", "Nissan", "Versa", 2020, "Gris", "ABC123", 50_000.0)

    private fun gasto(monto: Double, fecha: LocalDate, categoria: String = "Gasolina", vehiculoId: String = "v1") =
        Expense("g${monto}${fecha}", vehiculoId, categoria, monto, fecha, "")

    private fun mantenimiento(costo: Double, fecha: LocalDate, km: Double) =
        Maintenance("m${costo}${fecha}", "v1", "Cambio de aceite", fecha, km, costo, "")

    private fun recordatorio(id: String, fecha: LocalDate, km: Double? = null, completado: Boolean = false) =
        Reminder(id, "v1", "Servicio $id", fecha, km, completado)

    @Test
    fun `suma gastos y mantenimientos del mes y del anio`() {
        val stats = calcularEstadisticas(
            vehiculo,
            mantenimientos = listOf(mantenimiento(1_000.0, hoy.withDayOfMonth(2), 45_000.0)),
            gastos = listOf(
                gasto(500.0, hoy),
                gasto(300.0, hoy.withMonth(1)),
                gasto(999.0, hoy.minusYears(1)),
                gasto(700.0, hoy, vehiculoId = "otro")
            ),
            recordatorios = emptyList(),
            hoy = hoy
        )

        assertEquals(1_500.0, stats.gastoMes, 0.001)
        assertEquals(1_800.0, stats.gastoAnio, 0.001)
        assertEquals(2_799.0, stats.gastoTotal, 0.001)
        assertEquals(listOf(CATEGORIA_MANTENIMIENTO, "Gasolina"), stats.categoriasAnio.map { it.nombre })
    }

    @Test
    fun `costo por km usa el primer kilometraje registrado`() {
        val stats = calcularEstadisticas(
            vehiculo,
            mantenimientos = listOf(mantenimiento(2_000.0, hoy, 40_000.0)),
            gastos = emptyList(),
            recordatorios = emptyList(),
            hoy = hoy
        )
        assertEquals(0.2, stats.costoPorKm!!, 0.0001)
    }

    @Test
    fun `sin kilometraje suficiente no hay costo por km`() {
        val stats = calcularEstadisticas(vehiculo, emptyList(), listOf(gasto(100.0, hoy)), emptyList(), hoy)
        assertNull(stats.costoPorKm)
    }

    @Test
    fun `recordatorio vence por fecha o por kilometraje`() {
        val porFecha = estadoRecordatorio(recordatorio("a", hoy.minusDays(1)), 50_000.0, hoy)
        val porKm = estadoRecordatorio(recordatorio("b", hoy.plusDays(30), km = 49_000.0), 50_000.0, hoy)
        val pendiente = estadoRecordatorio(recordatorio("c", hoy.plusDays(3), km = 50_800.0), 50_000.0, hoy)

        assertEquals(EstadoRecordatorio.Vencido(porFecha = true, porKilometraje = false), porFecha)
        assertEquals(EstadoRecordatorio.Vencido(porFecha = false, porKilometraje = true), porKm)
        assertEquals(EstadoRecordatorio.Pendiente(diasRestantes = 3, kmRestantes = 800.0), pendiente)
    }

    @Test
    fun `el proximo recordatorio prioriza los vencidos e ignora completados`() {
        val stats = calcularEstadisticas(
            vehiculo,
            mantenimientos = emptyList(),
            gastos = emptyList(),
            recordatorios = listOf(
                recordatorio("pronto", hoy.plusDays(2)),
                recordatorio("hecho", hoy.minusDays(10), completado = true),
                recordatorio("km", hoy.plusDays(60), km = 49_500.0)
            ),
            hoy = hoy
        )

        assertEquals("km", stats.proximoRecordatorio?.id)
        assertEquals(1, stats.recordatoriosVencidos)
        assertTrue(stats.estadoProximo is EstadoRecordatorio.Vencido)
    }

    @Test
    fun `el texto del aviso no repite fecha y km`() {
        val km = 50_000.0
        val pronto = recordatorio("a", hoy.plusDays(10), km = 50_800.0)
        val lejano = recordatorio("b", hoy.plusDays(200), km = 60_000.0)
        val porKm = recordatorio("c", hoy.plusDays(60), km = 49_000.0)
        val porFecha = recordatorio("d", hoy.minusDays(2))

        assertEquals("En 10 días · faltan 800 km", textoAviso(pronto, estadoRecordatorio(pronto, km, hoy)))
        assertEquals("El 13/04/2027 · faltan 10,000 km", textoAviso(lejano, estadoRecordatorio(lejano, km, hoy)))
        assertEquals("El 13/04/2027", textoAviso(lejano, estadoRecordatorio(lejano, km, hoy), conKm = false))
        assertEquals("Ya pasó los 49,000 km", textoAviso(porKm, estadoRecordatorio(porKm, km, hoy)))
        assertEquals("Venció el 23/09/2026", textoAviso(porFecha, estadoRecordatorio(porFecha, km, hoy)))
    }
}
