package com.haydra.autocare.data.validation

/**
 * Reglas de validación del recordatorio (HU08, RF15-RF16): título obligatorio,
 * fecha válida y, si se indica, kilometraje válido. El kilometraje es opcional
 * porque un recordatorio puede basarse solo en la fecha.
 */
object ReminderValidator {

    fun validateTitulo(titulo: String): String? = when {
        titulo.isBlank() -> "El título es obligatorio"
        else -> null
    }

    fun validateFecha(fechaMillis: Long?): String? =
        if (fechaMillis == null) "Selecciona una fecha" else null

    fun validateKilometraje(kilometrajeTexto: String): String? {
        if (kilometrajeTexto.isBlank()) return null
        val kilometraje = kilometrajeTexto.toDoubleOrNull() ?: return "El kilometraje no es válido"
        return if (kilometraje < 0) "El kilometraje no puede ser negativo" else null
    }
}
