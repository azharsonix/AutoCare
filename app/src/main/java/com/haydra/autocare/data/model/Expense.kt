package com.haydra.autocare.data.model

import java.time.LocalDate

data class Expense(
    val id: String,
    val vehiculoId: String,
    val categoria: String,
    val monto: Double,
    val fecha: LocalDate,
    val descripcion: String,
    /** Solo en cargas de gasolina: litros cargados y kilometraje (odómetro) al cargar. */
    val litros: Double? = null,
    val kilometraje: Double? = null,
    /** Foto del ticket o factura (ruta interna, ver [com.haydra.autocare.data.local.AlmacenFotos]). */
    val foto: String? = null
)
