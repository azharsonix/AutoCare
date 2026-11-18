package com.haydra.autocare

import com.haydra.autocare.data.local.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun verify_rechazaUnaContrasenaIncorrecta() {
        val guardado = PasswordHasher.hash("secreta123")
        assertFalse(PasswordHasher.verify("otra", guardado))
    }

    @Test
    fun hash_noGuardaTextoPlanoYUsaSalDistinta() {
        val a = PasswordHasher.hash("secreta123")
        val b = PasswordHasher.hash("secreta123")
        assertFalse(a.contains("secreta123"))
        assertNotEquals(a, b)
    }
}
