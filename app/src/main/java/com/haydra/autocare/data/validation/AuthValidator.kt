package com.haydra.autocare.data.validation

/**
 * Reglas de validación descritas en AUTOCARE.txt, sección 37 (Validaciones > Usuario):
 * correo electrónico válido, contraseña obligatoria y campos requeridos.
 *
 * Se usa una expresión regular propia (en vez de android.util.Patterns) para que
 * esta lógica sea verificable con pruebas unitarias JVM sin depender del framework Android.
 */
object AuthValidator {

    private const val MIN_PASSWORD_LENGTH = 6
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateNombre(nombre: String): String? = when {
        nombre.isBlank() -> "El nombre es obligatorio"
        else -> null
    }

    fun validateCorreo(correo: String): String? = when {
        correo.isBlank() -> "El correo es obligatorio"
        !EMAIL_REGEX.matches(correo.trim()) -> "El correo no es válido"
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.isBlank() -> "La contraseña es obligatoria"
        password.length < MIN_PASSWORD_LENGTH -> "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
        else -> null
    }

    /** La ley pide el consentimiento antes de tratar los datos: sin aceptar el aviso no se crea la cuenta. */
    fun validateAviso(aceptado: Boolean): String? =
        if (aceptado) null else "Acepta el aviso de privacidad para crear tu cuenta"

    fun validateConfirmacion(password: String, confirmacion: String): String? = when {
        confirmacion.isBlank() -> "Confirma tu contraseña"
        confirmacion != password -> "Las contraseñas no coinciden"
        else -> null
    }
}
