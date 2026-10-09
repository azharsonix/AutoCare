package com.haydra.autocare.data.repository

import com.haydra.autocare.data.DemoData
import com.haydra.autocare.data.model.Expense
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Implementación en memoria de [ExpenseRepository], usada por las pruebas unitarias (la app
 * usa la versión Room, ver AppContainer). Antes de Room era la única y servía
 * mientras no existiera un
 * proyecto de Supabase. Guarda los gastos en memoria, asociados al vehículo
 * al que pertenecen. Se precargan un par de ejemplos para el primer vehículo
 * de la cuenta demo (ver [DemoData]).
 */
class FakeExpenseRepository : ExpenseRepository {

    private val gastosPorId = mutableMapOf<String, Expense>()

    init {
        val hoy = LocalDate.now()
        val ejemplo1 = Expense(
            id = UUID.randomUUID().toString(),
            vehiculoId = DemoData.VEHICLE_1_ID,
            categoria = "Gasolina",
            monto = 650.0,
            fecha = hoy.minusDays(5),
            descripcion = "Tanque lleno"
        )
        val ejemplo2 = Expense(
            id = UUID.randomUUID().toString(),
            vehiculoId = DemoData.VEHICLE_1_ID,
            categoria = "Verificación vehicular",
            monto = 450.0,
            fecha = hoy.minusMonths(3),
            descripcion = ""
        )
        gastosPorId[ejemplo1.id] = ejemplo1
        gastosPorId[ejemplo2.id] = ejemplo2
    }

    override suspend fun getExpenses(vehiculoId: String): Result<List<Expense>> {
        delay(200)
        val gastos = gastosPorId.values
            .filter { it.vehiculoId == vehiculoId }
            .sortedByDescending { it.fecha }
        return Result.success(gastos)
    }

    override suspend fun insertExpense(
        vehiculoId: String,
        categoria: String,
        monto: Double,
        fecha: LocalDate,
        descripcion: String,
        litros: Double?,
        kilometraje: Double?,
        foto: String?
    ): Result<Expense> {
        delay(300)
        val gasto = Expense(
            id = UUID.randomUUID().toString(),
            vehiculoId = vehiculoId,
            categoria = categoria.trim(),
            monto = monto,
            fecha = fecha,
            descripcion = descripcion.trim(),
            litros = litros,
            kilometraje = kilometraje,
            foto = foto
        )
        gastosPorId[gasto.id] = gasto
        return Result.success(gasto)
    }

    override suspend fun updateExpense(expense: Expense): Result<Expense> {
        delay(300)
        if (!gastosPorId.containsKey(expense.id)) {
            return Result.failure(NoSuchElementException("El gasto no existe"))
        }
        gastosPorId[expense.id] = expense
        return Result.success(expense)
    }

    override suspend fun deleteExpense(id: String): Result<Unit> {
        delay(200)
        gastosPorId.remove(id)
        return Result.success(Unit)
    }
}
