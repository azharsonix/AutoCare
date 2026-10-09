package com.haydra.autocare.data.repository

import com.haydra.autocare.data.model.Expense
import java.time.LocalDate

/**
 * Contrato de administración de gastos (RF11-RF13, RF18). La app usa
 * [RoomExpenseRepository] y las pruebas [FakeExpenseRepository]; en el Sprint 6 se añadirá
 * una implementación respaldada por la tabla "gastos" de Supabase (AUTOCARE.txt, sección 32).
 */
interface ExpenseRepository {
    suspend fun getExpenses(vehiculoId: String): Result<List<Expense>>
    suspend fun insertExpense(
        vehiculoId: String,
        categoria: String,
        monto: Double,
        fecha: LocalDate,
        descripcion: String,
        litros: Double? = null,
        kilometraje: Double? = null,
        foto: String? = null
    ): Result<Expense>
    suspend fun updateExpense(expense: Expense): Result<Expense>
    suspend fun deleteExpense(id: String): Result<Unit>
}
