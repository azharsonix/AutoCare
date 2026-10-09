package com.haydra.autocare.data.validation

/**
 * Reglas de validación del mantenimiento descritas en AUTOCARE.txt, sección 37
 * (tipo obligatorio, fecha válida, costo no negativo, kilometraje válido).
 */
object MaintenanceValidator {

    fun validateTipo(tipo: String): String? = when {
        tipo.isBlank() -> "El tipo de mantenimiento es obligatorio"
        else -> null
    }

    fun validateFecha(fechaMillis: Long?): String? =
        if (fechaMillis == null) "Selecciona una fecha" else null

    fun validateKilometraje(kilometrajeTexto: String): String? {
        val kilometraje = kilometrajeTexto.toDoubleOrNull() ?: return "El kilometraje no es válido"
        return if (kilometraje < 0) "El kilometraje no puede ser negativo" else null
    }

    fun validateCosto(costoTexto: String): String? {
        val costo = costoTexto.toDoubleOrNull() ?: return "El costo no es válido"
        return if (costo < 0) "El costo no puede ser negativo" else null
    }
}
