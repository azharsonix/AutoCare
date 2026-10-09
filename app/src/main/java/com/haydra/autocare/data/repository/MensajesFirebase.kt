package com.haydra.autocare.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

/** Traduce los errores de Firebase a mensajes claros para el usuario. */
object MensajesFirebase {

    const val GENERICO = "No pudimos completar la operación. Inténtalo de nuevo."
    const val SIN_CONEXION = "Sin conexión a internet. Revisa tu red e inténtalo de nuevo."

    /** Códigos con los que Firebase responde a un correo o contraseña que no coinciden. */
    val CREDENCIALES_INVALIDAS = setOf("ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD")

    fun de(error: Throwable): String = when (error) {
        is FirebaseNetworkException -> SIN_CONEXION
        is FirebaseAuthException -> porCodigo(error.errorCode)
        is FirebaseFirestoreException -> GENERICO
        else -> error.message ?: GENERICO
    }

    fun porCodigo(codigo: String): String = when (codigo) {
        in CREDENCIALES_INVALIDAS -> "El correo o la contraseña son incorrectos"
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta con ese correo"
        "ERROR_INVALID_EMAIL" -> "El correo no es válido"
        "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil: usa al menos 6 caracteres"
        "ERROR_USER_DISABLED" -> "Esta cuenta está desactivada"
        "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera unos minutos e inténtalo de nuevo."
        "ERROR_REQUIRES_RECENT_LOGIN" -> "Por seguridad, cierra sesión y vuelve a entrar antes de este cambio"
        "ERROR_NETWORK_REQUEST_FAILED" -> SIN_CONEXION
        else -> GENERICO
    }
}
