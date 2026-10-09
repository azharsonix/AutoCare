package com.haydra.autocare.ui.common

/** Cada cuánto se repite un servicio: por km, por meses o ambos (lo que llegue antes). */
data class Intervalo(val km: Int?, val meses: Int?)

/**
 * Valores frecuentes que se ofrecen como atajo en los formularios para no
 * tener que escribirlos a mano. El usuario sigue pudiendo escribir otro.
 * Los títulos de recordatorio usan los mismos nombres que los tipos de
 * mantenimiento: así registrar un servicio completa su recordatorio.
 */
object Plantillas {
    private val intervalos: Map<String, Intervalo> = linkedMapOf(
        "Cambio de aceite" to Intervalo(km = 5_000, meses = 6),
        "Afinación" to Intervalo(km = 20_000, meses = 12),
        "Frenos" to Intervalo(km = 20_000, meses = 12),
        "Rotación y balanceo" to Intervalo(km = 10_000, meses = 6),
        "Alineación" to Intervalo(km = 10_000, meses = 6),
        "Llantas" to Intervalo(km = 40_000, meses = 36),
        "Filtro de aire" to Intervalo(km = 15_000, meses = 12),
        "Batería" to Intervalo(km = null, meses = 24),
        "Anticongelante" to Intervalo(km = 40_000, meses = 24),
        "Suspensión" to Intervalo(km = 50_000, meses = 24),
        "Verificación vehicular" to Intervalo(km = null, meses = 6),
        "Renovar seguro" to Intervalo(km = null, meses = 12),
        "Pagar tenencia" to Intervalo(km = null, meses = 12)
    )

    val marcasVehiculo = listOf(
        "Nissan",
        "Chevrolet",
        "Volkswagen",
        "Toyota",
        "Kia",
        "Mazda",
        "Honda",
        "Hyundai",
        "Ford"
    )

    val tiposMantenimiento = listOf(
        "Cambio de aceite",
        "Afinación",
        "Frenos",
        "Rotación y balanceo",
        "Alineación",
        "Llantas",
        "Filtro de aire",
        "Batería",
        "Anticongelante",
        "Suspensión"
    )

    val categoriasGasto = listOf(
        "Gasolina",
        "Seguro",
        "Verificación",
        "Tenencia",
        "Lavado",
        "Estacionamiento",
        "Casetas",
        "Refacciones",
        "Multas"
    )

    val tiposDocumento = listOf(
        "Póliza de seguro",
        "Tarjeta de circulación",
        "Verificación",
        "Tenencia",
        "Licencia de conducir",
        "Factura del auto"
    )

    val titulosRecordatorio = listOf(
        "Cambio de aceite",
        "Verificación vehicular",
        "Renovar seguro",
        "Pagar tenencia",
        "Afinación",
        "Frenos",
        "Rotación y balanceo"
    )

    fun intervaloDe(nombre: String): Intervalo? =
        intervalos.entries.firstOrNull { it.key.equals(nombre.trim(), ignoreCase = true) }?.value
}
