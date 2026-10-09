package com.haydra.autocare.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.haydra.autocare.data.model.Documento
import com.haydra.autocare.data.model.Expense
import com.haydra.autocare.data.model.Maintenance
import com.haydra.autocare.data.model.Reminder
import com.haydra.autocare.data.model.User
import com.haydra.autocare.data.model.Vehicle
import java.time.LocalDate

/**
 * Tablas locales de AutoCare. Siguen los mismos nombres que las colecciones de
 * Cloud Firestore (antes tablas de AUTOCARE.txt, sección 32), para que la futura
 * sincronización sea un mapeo directo. Las llaves foráneas con CASCADE
 * cumplen RF17-RF18: al eliminar un usuario o vehículo se eliminan sus
 * registros asociados.
 */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["correo"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val correo: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String
)

@Entity(
    tableName = "vehiculos",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("usuario_id")]
)
data class VehicleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "usuario_id") val usuarioId: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val color: String,
    val placas: String,
    val kilometraje: Double,
    @ColumnInfo(name = "km_actualizado") val kmActualizado: LocalDate?
)

@Entity(
    tableName = "mantenimientos",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehiculo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehiculo_id")]
)
data class MaintenanceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehiculo_id") val vehiculoId: String,
    val tipo: String,
    val fecha: LocalDate,
    val kilometraje: Double,
    val costo: Double,
    val descripcion: String,
    val foto: String?
)

@Entity(
    tableName = "gastos",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehiculo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehiculo_id")]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehiculo_id") val vehiculoId: String,
    val categoria: String,
    val monto: Double,
    val fecha: LocalDate,
    val descripcion: String,
    val litros: Double?,
    val kilometraje: Double?,
    val foto: String?
)

@Entity(
    tableName = "recordatorios",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehiculo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehiculo_id")]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehiculo_id") val vehiculoId: String,
    val titulo: String,
    val fecha: LocalDate,
    val kilometraje: Double?,
    val completado: Boolean
)

@Entity(
    tableName = "documentos",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehiculo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehiculo_id")]
)
data class DocumentoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehiculo_id") val vehiculoId: String,
    val tipo: String,
    val vence: LocalDate?,
    val foto: String?,
    val notas: String
)

fun UserEntity.toModel() = User(id = id, nombre = nombre, correo = correo)

fun VehicleEntity.toModel() = Vehicle(
    id = id, usuarioId = usuarioId, marca = marca, modelo = modelo,
    anio = anio, color = color, placas = placas, kilometraje = kilometraje, kmActualizado = kmActualizado
)

fun Vehicle.toEntity() = VehicleEntity(
    id = id, usuarioId = usuarioId, marca = marca, modelo = modelo,
    anio = anio, color = color, placas = placas, kilometraje = kilometraje, kmActualizado = kmActualizado
)

fun MaintenanceEntity.toModel() = Maintenance(
    id = id, vehiculoId = vehiculoId, tipo = tipo, fecha = fecha,
    kilometraje = kilometraje, costo = costo, descripcion = descripcion, foto = foto
)

fun Maintenance.toEntity() = MaintenanceEntity(
    id = id, vehiculoId = vehiculoId, tipo = tipo, fecha = fecha,
    kilometraje = kilometraje, costo = costo, descripcion = descripcion, foto = foto
)

fun ExpenseEntity.toModel() = Expense(
    id = id, vehiculoId = vehiculoId, categoria = categoria,
    monto = monto, fecha = fecha, descripcion = descripcion,
    litros = litros, kilometraje = kilometraje, foto = foto
)

fun Expense.toEntity() = ExpenseEntity(
    id = id, vehiculoId = vehiculoId, categoria = categoria,
    monto = monto, fecha = fecha, descripcion = descripcion,
    litros = litros, kilometraje = kilometraje, foto = foto
)

fun ReminderEntity.toModel() = Reminder(
    id = id, vehiculoId = vehiculoId, titulo = titulo, fecha = fecha,
    kilometraje = kilometraje, completado = completado
)

fun Reminder.toEntity() = ReminderEntity(
    id = id, vehiculoId = vehiculoId, titulo = titulo, fecha = fecha,
    kilometraje = kilometraje, completado = completado
)

fun DocumentoEntity.toModel() = Documento(
    id = id, vehiculoId = vehiculoId, tipo = tipo, vence = vence, foto = foto, notas = notas
)

fun Documento.toEntity() = DocumentoEntity(
    id = id, vehiculoId = vehiculoId, tipo = tipo, vence = vence, foto = foto, notas = notas
)
