package com.haydra.autocare.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM usuarios WHERE correo = :correo")
    suspend fun getByCorreo(correo: String): UserEntity?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun count(): Int

    @Insert
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehiculos WHERE usuario_id = :usuarioId ORDER BY marca")
    suspend fun getByUsuario(usuarioId: String): List<VehicleEntity>

    @Query("SELECT * FROM vehiculos WHERE id = :id")
    suspend fun getById(id: String): VehicleEntity?

    /** Todos los vehículos del teléfono (para el aviso de actualizar el kilometraje). */
    @Query("SELECT * FROM vehiculos")
    suspend fun getAll(): List<VehicleEntity>

    @Insert
    suspend fun insert(vehicle: VehicleEntity)

    /** Devuelve el número de filas actualizadas (0 si el vehículo no existe). */
    @Update
    suspend fun update(vehicle: VehicleEntity): Int

    @Query("DELETE FROM vehiculos WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM mantenimientos WHERE vehiculo_id = :vehiculoId ORDER BY fecha DESC")
    suspend fun getByVehiculo(vehiculoId: String): List<MaintenanceEntity>

    @Insert
    suspend fun insert(maintenance: MaintenanceEntity)

    @Update
    suspend fun update(maintenance: MaintenanceEntity): Int

    @Query("DELETE FROM mantenimientos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT foto FROM mantenimientos WHERE foto IS NOT NULL")
    suspend fun fotos(): List<String>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM gastos WHERE vehiculo_id = :vehiculoId ORDER BY fecha DESC")
    suspend fun getByVehiculo(vehiculoId: String): List<ExpenseEntity>

    @Insert
    suspend fun insert(expense: ExpenseEntity)

    @Update
    suspend fun update(expense: ExpenseEntity): Int

    @Query("DELETE FROM gastos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT foto FROM gastos WHERE foto IS NOT NULL")
    suspend fun fotos(): List<String>
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM recordatorios WHERE vehiculo_id = :vehiculoId ORDER BY completado, fecha")
    suspend fun getByVehiculo(vehiculoId: String): List<ReminderEntity>

    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Insert
    suspend fun insert(reminder: ReminderEntity)

    @Update
    suspend fun update(reminder: ReminderEntity): Int

    @Query("UPDATE recordatorios SET completado = :completado WHERE id = :id")
    suspend fun setCompletado(id: String, completado: Boolean): Int

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface DocumentoDao {
    /** Primero los que vencen antes; los que no vencen, al final. */
    @Query("SELECT * FROM documentos WHERE vehiculo_id = :vehiculoId ORDER BY vence IS NULL, vence, tipo")
    suspend fun getByVehiculo(vehiculoId: String): List<DocumentoEntity>

    @Query("SELECT * FROM documentos WHERE id = :id")
    suspend fun getById(id: String): DocumentoEntity?

    @Insert
    suspend fun insert(documento: DocumentoEntity)

    @Update
    suspend fun update(documento: DocumentoEntity): Int

    @Query("DELETE FROM documentos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT foto FROM documentos WHERE foto IS NOT NULL")
    suspend fun fotos(): List<String>
}
