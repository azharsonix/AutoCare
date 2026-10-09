package com.haydra.autocare.data.model

import java.time.LocalDate

/**
 * Documento del vehículo (póliza de seguro, tarjeta de circulación,
 * verificación, tenencia...) con su foto y, si aplica, su fecha de vencimiento.
 */
data class Documento(
    val id: String,
    val vehiculoId: String,
    val tipo: String,
    val vence: LocalDate?,
    val foto: String?,
    val notas: String
)
