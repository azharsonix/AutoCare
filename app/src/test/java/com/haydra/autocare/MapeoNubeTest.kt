package com.haydra.autocare

import com.haydra.autocare.data.local.db.ExpenseEntity
import com.haydra.autocare.data.local.db.VehicleEntity
import com.haydra.autocare.data.nube.MapeoNube
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Conversión de las filas de Room a documentos de Firestore y de regreso. */
class MapeoNubeTest {

    @Test
    fun `un vehiculo sube y baja igual`() {
        val vehiculo = VehicleEntity("v1", "u1", "Nissan", "Versa", 2019, "Gris", "PRT-482-B", 58200.0, LocalDate.of(2026, 10, 18))
        assertEquals(vehiculo, MapeoNube.vehiculo("v1", "u1", MapeoNube.aMapa(vehiculo)))
    }

    @Test
    fun `la foto no se sube y al bajar se conserva la del telefono`() {
        val gasto = ExpenseEntity("g1", "v1", "Gasolina", 850.0, LocalDate.of(2026, 10, 1), "", 35.5, 58000.0, "foto.jpg")
        val mapa = MapeoNube.aMapa(gasto)
        assertFalse(mapa.containsKey("foto"))
        assertEquals(gasto, MapeoNube.gasto("g1", mapa, "foto.jpg"))
    }
}
