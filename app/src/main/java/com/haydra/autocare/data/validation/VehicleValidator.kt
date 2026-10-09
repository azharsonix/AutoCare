package com.haydra.autocare.data.validation

import java.time.Year

/**
 * Reglas de validación del vehículo descritas en AUTOCARE.txt, sección 37
 * (marca, modelo y año obligatorios; kilometraje válido; formato de placas permitido).
 */
object VehicleValidator {

    private val PLACAS_REGEX = Regex("^[A-Za-z0-9-]{5,10}$")

    fun validateMarca(marca: String): String? = when {
        marca.isBlank() -> "La marca es obligatoria"
        else -> null
    }

    fun validateModelo(modelo: String): String? = when {
        modelo.isBlank() -> "El modelo es obligatorio"
        else -> null
    }

    fun validateColor(color: String): String? = when {
        color.isBlank() -> "El color es obligatorio"
        else -> null
    }

    fun validateAnio(anioTexto: String): String? {
        val anio = anioTexto.toIntOrNull() ?: return "El año no es válido"
        val anioMaximo = Year.now().value + 1
        return if (anio < 1900 || anio > anioMaximo) "El año debe estar entre 1900 y $anioMaximo" else null
    }

    fun validateKilometraje(kilometrajeTexto: String): String? {
        val kilometraje = kilometrajeTexto.toDoubleOrNull() ?: return "El kilometraje no es válido"
        return if (kilometraje < 0) "El kilometraje no puede ser negativo" else null
    }

    fun validatePlacas(placas: String): String? = when {
        placas.isBlank() -> "Las placas son obligatorias"
        !PLACAS_REGEX.matches(placas) -> "Formato de placas no válido"
        else -> null
    }
}
