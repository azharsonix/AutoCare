package com.haydra.autocare

import com.haydra.autocare.data.validation.VehicleValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias de las validaciones de vehículo (AUTOCARE.txt, sección 37
 * y RF04-RF06), entregadas como parte del Sprint 2.
 */
class VehicleValidatorTest {

    @Test
    fun `marca y modelo vacios son invalidos`() {
        assertNotNull(VehicleValidator.validateMarca(""))
        assertNull(VehicleValidator.validateMarca("Toyota"))
        assertNotNull(VehicleValidator.validateModelo(""))
        assertNull(VehicleValidator.validateModelo("Corolla"))
    }

    @Test
    fun `placas con formato invalido son rechazadas`() {
        assertNotNull(VehicleValidator.validatePlacas(""))
        assertNotNull(VehicleValidator.validatePlacas("AB"))
        assertNotNull(VehicleValidator.validatePlacas("###-###"))
        assertNull(VehicleValidator.validatePlacas("ABC-123"))
    }
}
