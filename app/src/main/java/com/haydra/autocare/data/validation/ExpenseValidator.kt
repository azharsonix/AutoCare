package com.haydra.autocare.data.validation

/**
 * Reglas de validación del gasto descritas en AUTOCARE.txt, sección 37
 * (categoría obligatoria, monto mayor o igual a cero, fecha válida).
 */
object ExpenseValidator {

    fun validateCategoria(categoria: String): String? = when {
        categoria.isBlank() -> "La categoría es obligatoria"
        else -> null
    }

    fun validateMonto(montoTexto: String): String? {
        val monto = montoTexto.toDoubleOrNull() ?: return "El monto no es válido"
        return if (monto < 0) "El monto no puede ser negativo" else null
    }

    fun validateFecha(fechaMillis: Long?): String? =
        if (fechaMillis == null) "Selecciona una fecha" else null

    /** Litros de una carga de gasolina: opcionales, pero si se escriben deben ser más de cero. */
    fun validateLitros(litrosTexto: String): String? {
        if (litrosTexto.isBlank()) return null
        val litros = litrosTexto.toDoubleOrNull() ?: return "Los litros no son válidos"
        return if (litros <= 0) "Los litros deben ser más de cero" else null
    }

    /** Kilometraje al cargar gasolina: opcional. */
    fun validateKilometraje(kilometrajeTexto: String): String? {
        if (kilometrajeTexto.isBlank()) return null
        val kilometraje = kilometrajeTexto.toDoubleOrNull() ?: return "El kilometraje no es válido"
        return if (kilometraje < 0) "El kilometraje no puede ser negativo" else null
    }
}
