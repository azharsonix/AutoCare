package com.haydra.autocare

import com.haydra.autocare.data.validation.ExpenseValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias de las validaciones de gasto (AUTOCARE.txt, sección 37
 * y RF11-RF13), entregadas como parte del Sprint 4.
 */
class ExpenseValidatorTest {

    @Test
    fun `categoria vacia es invalida`() {
        assertNotNull(ExpenseValidator.validateCategoria(""))
        assertNull(ExpenseValidator.validateCategoria("Gasolina"))
    }

    @Test
    fun `monto negativo o no numerico es invalido`() {
        assertNotNull(ExpenseValidator.validateMonto(""))
        assertNotNull(ExpenseValidator.validateMonto("abc"))
        assertNotNull(ExpenseValidator.validateMonto("-1"))
        assertNull(ExpenseValidator.validateMonto("0"))
        assertNull(ExpenseValidator.validateMonto("450.75"))
    }

    @Test
    fun `fecha nula es invalida`() {
        assertNotNull(ExpenseValidator.validateFecha(null))
        assertNull(ExpenseValidator.validateFecha(1_700_000_000_000L))
    }
}
