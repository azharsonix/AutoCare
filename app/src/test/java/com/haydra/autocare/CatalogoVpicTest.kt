package com.haydra.autocare

import com.haydra.autocare.data.catalogo.CatalogoVpic
import com.haydra.autocare.data.catalogo.DatosVin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Lectura de las respuestas JSON de la API vPIC (respuestas recortadas de las reales). */
class CatalogoVpicTest {

    @Test
    fun `los modelos salen sin repetir y ordenados`() {
        val json = """
            {"Count":4,"Results":[
              {"Make_Name":"NISSAN","Model_Name":"Versa"},
              {"Make_Name":"NISSAN","Model_Name":"Altima"},
              {"Make_Name":"NISSAN","Model_Name":"Versa"},
              {"Make_Name":"NISSAN","Model_Name":" "}
            ]}
        """.trimIndent()
        assertEquals(listOf("Altima", "Versa"), CatalogoVpic.leerModelos(json))
    }

    @Test
    fun `sin resultados devuelve lista vacia`() {
        assertEquals(emptyList<String>(), CatalogoVpic.leerModelos("""{"Count":0,"Results":[]}"""))
    }

    @Test
    fun `el VIN trae marca con mayuscula inicial, modelo y anio`() {
        val json = """{"Results":[{"Make":"NISSAN","Model":"Versa","ModelYear":"2019"}]}"""
        assertEquals(DatosVin("Nissan", "Versa", 2019), CatalogoVpic.leerVin(json))
    }

    @Test
    fun `un VIN que no existe no trae datos`() {
        val json = """{"Results":[{"Make":"","Model":"","ModelYear":""}]}"""
        assertNull(CatalogoVpic.leerVin(json))
    }
}
