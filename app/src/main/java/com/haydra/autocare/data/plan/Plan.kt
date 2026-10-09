package com.haydra.autocare.data.plan

import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Reglas del modelo freemium de AutoCare. El plan Gratis tiene todo lo
 * demás; Pro solo quita estos dos límites. Nunca se bloquean datos ya
 * guardados: quien tiene más vehículos que el límite los sigue usando, solo
 * no puede registrar otro.
 */
object Limites {
    /** Vehículos que se pueden registrar en el plan Gratis. */
    const val VEHICULOS_GRATIS = 1

    /** Meses hacia atrás que muestra el historial en el plan Gratis. */
    const val MESES_HISTORIAL_GRATIS = 12L

    /** Precio que se muestra en la pantalla Pro (pago único). */
    const val PRECIO_PRO = "$49 MXN"

    fun puedeAgregarVehiculo(esPro: Boolean, vehiculosActuales: Int): Boolean =
        esPro || vehiculosActuales < VEHICULOS_GRATIS

    /** Primer día que se ve en el historial del plan Gratis (inclusive). */
    fun inicioHistorialGratis(hoy: LocalDate = LocalDate.now()): LocalDate =
        hoy.minusMonths(MESES_HISTORIAL_GRATIS)
}

/** Guarda qué cuentas tienen Pro. */
interface PlanRepository {
    fun esPro(usuarioId: String): Flow<Boolean>
    suspend fun activarPro(usuarioId: String)
    suspend fun quitarPro(usuarioId: String)
}

/**
 * Cobro de Pro. Hoy se usa [PagosDemo]; para Google Play Billing basta con
 * otra implementación de esta interfaz en [com.haydra.autocare.AppContainer].
 */
interface ProveedorPagos {
    /** true cuando no se hace ningún cobro real (se avisa en pantalla). */
    val esDemostracion: Boolean

    suspend fun comprarPro(): Result<Unit>

    /** true si la cuenta de la tienda ya había comprado Pro. */
    suspend fun restaurarCompra(): Result<Boolean>
}

/** Compra simulada: tarda un momento y siempre se aprueba. No cobra nada. */
class PagosDemo : ProveedorPagos {
    override val esDemostracion = true

    override suspend fun comprarPro(): Result<Unit> {
        delay(900)
        return Result.success(Unit)
    }

    override suspend fun restaurarCompra(): Result<Boolean> = Result.success(false)
}
