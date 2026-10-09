package com.haydra.autocare

import com.haydra.autocare.data.model.Documento
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.model.Vehicle
import com.haydra.autocare.ui.vehicles.DatosVehiculo
import com.haydra.autocare.ui.vehicles.resumirInicio
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Resumen de la pantalla de inicio con varios vehículos, sin datos repetidos. */
class InicioTest {

    private val hoy = LocalDate.of(2026, 9, 25)
    private val honda = Vehicle("h", "u1", "Honda", "CR-V", 2020, "Gris", "XYZ", 45_000.0)
    private val toyota = Vehicle("t", "u1", "Toyota", "Corolla", 2022, "Rojo", "ABC", 15_000.0)

    private val datos = mapOf(
        "h" to DatosVehiculo(
            mantenimientos = listOf(Maintenance("m1", "h", "Frenos", hoy, 45_000.0, 1_000.0, "")),
            gastos = listOf(Expense("g1", "h", "Gasolina", 500.0, hoy.minusMonths(1), "")),
            recordatorios = listOf(
                Reminder("r1", "h", "Verificación", hoy.minusDays(3), null, false),
                Reminder("r2", "h", "Cambio de aceite", hoy.plusDays(30), 50_000.0, false),
                Reminder("r5", "h", "Seguro", hoy.plusDays(90), null, false),
                Reminder("r3", "h", "Tenencia", hoy.minusDays(40), null, true)
            )
        ),
        "t" to DatosVehiculo(
            mantenimientos = emptyList(),
            gastos = listOf(Expense("g2", "t", "Casetas", 250.0, hoy, "")),
            recordatorios = listOf(Reminder("r4", "t", "Afinación", hoy.plusDays(60), 14_000.0, false))
        )
    )

    @Test
    fun `suma el gasto del mes de todos los vehiculos`() {
        val inicio = resumirInicio(listOf(honda, toyota), datos, hoy)

        assertTrue(inicio.cargado)
        assertEquals(1_250.0, inicio.gastoMes, 0.001)
    }

    @Test
    fun `los vencidos solo van en alertas y cada vehiculo muestra su proximo aviso no vencido`() {
        val inicio = resumirInicio(listOf(honda, toyota), datos, hoy)

        // r1 vence por fecha (Honda) y r4 por km (el Toyota ya pasó los 14,000); r3 está completado.
        assertEquals(listOf("r1", "r4"), inicio.alertas.map { it.recordatorio.id })
        // Honda: su próximo es r2 (el más cercano sin vencer), no r1 que ya está en alertas.
        assertEquals("r2", inicio.proximos["h"]?.first?.id)
        // Toyota: su único aviso está vencido, así que no se repite en su tarjeta.
        assertFalse("t" in inicio.proximos)
    }

    @Test
    fun `sin datos de un vehiculo no suma nada`() {
        val inicio = resumirInicio(listOf(honda), emptyMap(), hoy)
        assertEquals(0.0, inicio.gastoMes, 0.001)
        assertTrue(inicio.alertas.isEmpty())
        assertTrue(inicio.proximos.isEmpty())
    }

    @Test
    fun `solo los documentos vencidos o por vencer piden atencion, los mas proximos primero`() {
        fun doc(id: String, vence: LocalDate?) = Documento(id, "h", "Doc $id", vence, null, "")
        val conDocumentos = datos + ("h" to datos.getValue("h").copy(
            documentos = listOf(
                doc("lejos", hoy.plusDays(90)),
                doc("pronto", hoy.plusDays(10)),
                doc("vencido", hoy.minusDays(2)),
                doc("sinFecha", null)
            )
        ))
        val inicio = resumirInicio(listOf(honda, toyota), conDocumentos, hoy)

        assertEquals(listOf("vencido", "pronto"), inicio.documentos.map { it.documento.id })
        assertTrue(inicio.documentos.first().vigencia.vencido)
        assertFalse(inicio.documentos.last().vigencia.vencido)
    }
}
