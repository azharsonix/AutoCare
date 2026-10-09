package com.haydra.autocare

import com.haydra.autocare.data.validation.ReminderValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias de las validaciones de recordatorio (HU08, RF15-RF16),
 * entregadas como parte del Sprint 5.
 */
class ReminderValidatorTest {

    @Test
    fun `titulo vacio es invalido`() {
        assertNotNull(ReminderValidator.validateTitulo(""))
        assertNull(ReminderValidator.validateTitulo("Cambio de aceite"))
    }

    @Test
    fun `fecha nula es invalida`() {
        assertNotNull(ReminderValidator.validateFecha(null))
        assertNull(ReminderValidator.validateFecha(1_700_000_000_000L))
    }

    @Test
    fun `kilometraje es opcional pero si se indica debe ser valido`() {
        assertNull(ReminderValidator.validateKilometraje(""))
        assertNotNull(ReminderValidator.validateKilometraje("abc"))
        assertNotNull(ReminderValidator.validateKilometraje("-10"))
        assertNull(ReminderValidator.validateKilometraje("50000"))
    }
}
