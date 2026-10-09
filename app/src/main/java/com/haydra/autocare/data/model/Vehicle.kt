package com.haydra.autocare.data.model

import java.time.LocalDate

data class Vehicle(
    val id: String,
    val usuarioId: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val color: String,
    val placas: String,
    val kilometraje: Double,
    /** Último día en que se puso al día el kilometraje (para recordar actualizarlo). */
    val kmActualizado: LocalDate? = null
)
