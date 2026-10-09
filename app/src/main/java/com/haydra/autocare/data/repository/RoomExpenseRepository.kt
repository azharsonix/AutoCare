package com.haydra.autocare.data.repository

import com.haydra.autocare.data.local.db.ExpenseDao
import com.haydra.autocare.data.local.db.toEntity
import com.haydra.autocare.data.local.db.toModel
import com.haydra.autocare.data.model.Expense
import java.time.LocalDate
import java.util.UUID

/** Implementación de [ExpenseRepository] sobre Room. */
class RoomExpenseRepository(private val expenseDao: ExpenseDao) : ExpenseRepository {

    override suspend fun getExpenses(vehiculoId: String): Result<List<Expense>> = runCatching {
        expenseDao.getByVehiculo(vehiculoId).map { it.toModel() }
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
    ): Result<Expense> = runCatching {
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
        expenseDao.insert(gasto.toEntity())
        gasto
    }

    override suspend fun updateExpense(expense: Expense): Result<Expense> = runCatching {
        if (expenseDao.update(expense.toEntity()) == 0) throw NoSuchElementException("El gasto no existe")
        expense
    }

    override suspend fun deleteExpense(id: String): Result<Unit> = runCatching {
        expenseDao.deleteById(id)
    }
}
