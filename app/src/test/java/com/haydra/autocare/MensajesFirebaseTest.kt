package com.haydra.autocare

import com.haydra.autocare.data.repository.MensajesFirebase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Los errores de Firebase Authentication llegan al usuario como mensajes en español. */
class MensajesFirebaseTest {

    @Test
    fun `credenciales invalidas no revelan si el correo existe`() {
        val mensajes = MensajesFirebase.CREDENCIALES_INVALIDAS.map(MensajesFirebase::porCodigo).toSet()
        assertEquals(setOf("El correo o la contraseña son incorrectos"), mensajes)
    }

    @Test
    fun `correo ya registrado`() {
        assertEquals("Ya existe una cuenta con ese correo", MensajesFirebase.porCodigo("ERROR_EMAIL_ALREADY_IN_USE"))
    }

    @Test
    fun `fallo de red pide revisar la conexion`() {
        assertEquals(MensajesFirebase.SIN_CONEXION, MensajesFirebase.porCodigo("ERROR_NETWORK_REQUEST_FAILED"))
    }

    @Test
    fun `codigo desconocido usa el mensaje generico`() {
        assertEquals(MensajesFirebase.GENERICO, MensajesFirebase.porCodigo("ERROR_ALGO_NUEVO"))
        assertNotEquals(MensajesFirebase.GENERICO, MensajesFirebase.porCodigo("ERROR_WEAK_PASSWORD"))
    }

    @Test
    fun `errores propios conservan su mensaje`() {
        assertEquals("Tu sesión expiró", MensajesFirebase.de(IllegalStateException("Tu sesión expiró")))
    }
}
