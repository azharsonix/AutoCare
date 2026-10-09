package com.haydra.autocare.data.model

import java.time.LocalDate

data class Maintenance(
    val id: String,
    val vehiculoId: String,
    val tipo: String,
    val fecha: LocalDate,
    val kilometraje: Double,
    val costo: Double,
    val descripcion: String,
    /** Foto del ticket o factura (ruta interna, ver [com.haydra.autocare.data.local.AlmacenFotos]). */
    val foto: String? = null
)
