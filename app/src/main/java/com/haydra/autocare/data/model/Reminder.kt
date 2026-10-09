package com.haydra.autocare.data.model

import java.time.LocalDate

data class Reminder(
    val id: String,
    val vehiculoId: String,
    val titulo: String,
    val fecha: LocalDate,
    val kilometraje: Double?,
    val completado: Boolean
)
