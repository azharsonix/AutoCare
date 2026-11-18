package com.haydra.autocare

import com.haydra.autocare.data.validation.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias de las validaciones de registro/login (AUTOCARE.txt, sección 37
 * y RNF "Validación"), que forman parte de la entrega del Sprint 1.
 */
class AuthValidatorTest {

    @Test
    fun `nombre vacio es invalido`() {
        assertNotNull(AuthValidator.validateNombre(""))
        assertNotNull(AuthValidator.validateNombre("   "))
        assertNull(AuthValidator.validateNombre("Pablo"))
    }

    @Test
    fun `correo con formato invalido es rechazado`() {
        assertNotNull(AuthValidator.validateCorreo(""))
        assertNotNull(AuthValidator.validateCorreo("no-es-un-correo"))
        assertNotNull(AuthValidator.validateCorreo("usuario@"))
        assertNull(AuthValidator.validateCorreo("usuario@autocare.com"))
    }

    @Test
    fun `password corta o vacia es invalida`() {
        assertNotNull(AuthValidator.validatePassword(""))
        assertNotNull(AuthValidator.validatePassword("123"))
        assertNull(AuthValidator.validatePassword("123456"))
    }

    @Test
    fun `confirmacion debe coincidir con la contrasena`() {
        assertNotNull(AuthValidator.validateConfirmacion("123456", ""))
        assertNotNull(AuthValidator.validateConfirmacion("123456", "654321"))
        assertNull(AuthValidator.validateConfirmacion("123456", "123456"))
    }

    @Test
    fun `sin aceptar el aviso de privacidad no se puede registrar`() {
        assertNotNull(AuthValidator.validateAviso(false))
        assertNull(AuthValidator.validateAviso(true))
    }

    @Test
    fun `suma de control de sanidad del entorno de pruebas`() {
        assertEquals(4, 2 + 2)
    }
}
