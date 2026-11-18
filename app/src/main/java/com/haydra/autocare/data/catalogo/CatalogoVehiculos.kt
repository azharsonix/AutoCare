package com.haydra.autocare.data.catalogo

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Datos que se obtienen al leer el número de serie (VIN) de un vehículo. */
data class DatosVin(val marca: String, val modelo: String, val anio: Int?)

/**
 * Catálogo de marcas y modelos de vehículos. La app usa [CatalogoVpic] y las pruebas
 * pueden usar una implementación falsa.
 */
interface CatalogoVehiculos {
    /** Modelos de [marca] para el año [anio] (si no hay datos de ese año, todos los de la marca). */
    suspend fun modelos(marca: String, anio: Int?): Result<List<String>>

    /** Marca, modelo y año a partir del número de serie de 17 caracteres. */
    suspend fun decodificarVin(vin: String): Result<DatosVin>
}

/**
 * API de terceros vPIC (Product Information Catalog) de la NHTSA, la agencia de seguridad vial de
 * Estados Unidos: https://vpic.nhtsa.dot.gov/api/ . Es pública, gratuita y no pide clave.
 * Las consultas van por HTTPS y con tiempo límite, para no colgar el formulario sin conexión.
 */
class CatalogoVpic(private val urlBase: String = URL_BASE) : CatalogoVehiculos {

    override suspend fun modelos(marca: String, anio: Int?): Result<List<String>> = runCatching {
        val marcaUrl = codificar(marca.trim())
        val delAnio = anio?.let { leerModelos(pedir("GetModelsForMakeYear/make/$marcaUrl/modelyear/$it")) }.orEmpty()
        delAnio.ifEmpty { leerModelos(pedir("GetModelsForMake/$marcaUrl")) }
    }

    override suspend fun decodificarVin(vin: String): Result<DatosVin> = runCatching {
        leerVin(pedir("DecodeVinValues/${codificar(vin.trim().uppercase())}"))
            ?: throw IllegalStateException("No encontramos datos para ese número de serie")
    }

    private suspend fun pedir(ruta: String): String = withContext(Dispatchers.IO) {
        val conexion = URL("$urlBase$ruta?format=json").openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = TIEMPO_MS
            conexion.readTimeout = TIEMPO_MS
            conexion.setRequestProperty("Accept", "application/json")
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("El catálogo no respondió (código ${conexion.responseCode})")
            }
            conexion.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }

    private fun codificar(texto: String) = URLEncoder.encode(texto, "UTF-8").replace("+", "%20")

    companion object {
        const val URL_BASE = "https://vpic.nhtsa.dot.gov/api/vehicles/"
        private const val TIEMPO_MS = 8_000

        /** Nombres de modelo sin repetir y ordenados, tal como los da el catálogo ("CR-V", "Versa"). */
        fun leerModelos(json: String): List<String> {
            val resultados = JSONObject(json).optJSONArray("Results") ?: return emptyList()
            return (0 until resultados.length())
                .mapNotNull { resultados.optJSONObject(it)?.optString("Model_Name")?.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }

        /** Lee la respuesta de DecodeVinValues; null si no trae marca ni modelo. */
        fun leerVin(json: String): DatosVin? {
            val fila = JSONObject(json).optJSONArray("Results")?.optJSONObject(0) ?: return null
            val marca = fila.optString("Make").trim()
            val modelo = fila.optString("Model").trim()
            if (marca.isEmpty() && modelo.isEmpty()) return null
            return DatosVin(
                marca = capitalizar(marca),
                modelo = modelo,
                anio = fila.optString("ModelYear").trim().toIntOrNull()
            )
        }

        /** Las marcas llegan en mayúsculas: "NISSAN" → "Nissan", "MERCEDES-BENZ" → "Mercedes-benz". */
        private fun capitalizar(texto: String): String = texto.lowercase().split(' ')
            .joinToString(" ") { p -> p.replaceFirstChar { it.uppercase() } }
    }
}
