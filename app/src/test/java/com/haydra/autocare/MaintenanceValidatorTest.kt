package com.haydra.autocare

import com.haydra.autocare.data.validation.MaintenanceValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias de las validaciones de mantenimiento (AUTOCARE.txt, sección 37
 * y RF08-RF10), entregadas como parte del Sprint 3.
 */
class MaintenanceValidatorTest {

    @Test
    fun `tipo vacio es invalido`() {
        assertNotNull(MaintenanceValidator.validateTipo(""))
        assertNull(MaintenanceValidator.validateTipo("Cambio de aceite"))
    }

    @Test
    fun `fecha nula es invalida`() {
        assertNotNull(MaintenanceValidator.validateFecha(null))
        assertNull(MaintenanceValidator.validateFecha(1_700_000_000_000L))
    }

    @Test
    fun `kilometraje negativo o no numerico es invalido`() {
        assertNotNull(MaintenanceValidator.validateKilometraje(""))
        assertNotNull(MaintenanceValidator.validateKilometraje("abc"))
        assertNotNull(MaintenanceValidator.validateKilometraje("-1"))
        assertNull(MaintenanceValidator.validateKilometraje("45000"))
    }

    @Test
    fun `costo negativo o no numerico es invalido`() {
        assertNotNull(MaintenanceValidator.validateCosto(""))
        assertNotNull(MaintenanceValidator.validateCosto("abc"))
        assertNotNull(MaintenanceValidator.validateCosto("-5"))
        assertNull(MaintenanceValidator.validateCosto("0"))
        assertNull(MaintenanceValidator.validateCosto("850.50"))
    }
}
