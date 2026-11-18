package com.haydra.autocare

import com.haydra.autocare.data.repository.MensajesFirebase
import org.junit.Assert.assertEquals
import org.junit.Test

/** Los errores de Firebase Authentication llegan al usuario como mensajes en español. */
class MensajesFirebaseTest {

    @Test
    fun `credenciales invalidas no revelan si el correo existe`() {
        val mensajes = MensajesFirebase.CREDENCIALES_INVALIDAS.map(MensajesFirebase::porCodigo).toSet()
        assertEquals(setOf("El correo o la contraseña son incorrectos"), mensajes)
    }

    @Test
    fun `fallo de red pide revisar la conexion`() {
        assertEquals(MensajesFirebase.SIN_CONEXION, MensajesFirebase.porCodigo("ERROR_NETWORK_REQUEST_FAILED"))
    }
}
