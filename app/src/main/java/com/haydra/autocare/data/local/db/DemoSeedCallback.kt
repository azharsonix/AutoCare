package com.haydra.autocare.data.local.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.haydra.autocare.data.DemoData
import com.haydra.autocare.data.local.PasswordHasher
import java.time.LocalDate
import java.util.UUID

/**
 * Precarga la cuenta demo (ver [DemoData]) la primera vez que se crea la base,
 * dentro de la misma apertura, para que ya exista antes de la primera consulta.
 * Después de eso los datos demo se comportan como cualquier otro registro.
 */
class DemoSeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        val hoy = LocalDate.now()
        db.beginTransaction()
        try {
            db.execSQL(
                "INSERT INTO usuarios (id, nombre, correo, password_hash) VALUES (?, ?, ?, ?)",
                arrayOf<Any?>(DemoData.USER_ID, DemoData.NOMBRE, DemoData.CORREO, PasswordHasher.hash(DemoData.PASSWORD))
            )

            insertarVehiculo(db, DemoData.VEHICLE_1_ID, "Toyota", "Corolla", 2022, "Rojo", "ABC-123", 15000.0)
            insertarVehiculo(db, DemoData.VEHICLE_2_ID, "Honda", "CR-V", 2020, "Gris", "XYZ-789", 42000.0)

            insertarMantenimiento(db, "Cambio de aceite", hoy.minusMonths(2), 13000.0, 850.0, "Aceite sintético 5W-30 y filtro")
            insertarMantenimiento(db, "Rotación de llantas", hoy.minusMonths(1), 14500.0, 300.0, "")

            // Tres cargas con litros y km: con ellas ya se ve el rendimiento.
            insertarGasto(db, "Gasolina", 960.0, hoy.minusDays(35), "Tanque lleno", litros = 40.0, km = 14_020.0)
            insertarGasto(db, "Gasolina", 936.0, hoy.minusDays(20), "Tanque lleno", litros = 39.0, km = 14_560.0)
            insertarGasto(db, "Gasolina", 888.0, hoy.minusDays(5), "Tanque lleno", litros = 37.0, km = 15_000.0)
            insertarGasto(db, "Verificación vehicular", 450.0, hoy.minusMonths(3), "")

            insertarRecordatorio(db, "Cambio de aceite", hoy.plusMonths(1), 18000.0)
            insertarRecordatorio(db, "Verificación vehicular", hoy.plusMonths(4), null)

            insertarDocumento(db, "Póliza de seguro", hoy.plusDays(20), "Aseguradora: llamar al 800 000 0000")
            insertarDocumento(db, "Tarjeta de circulación", hoy.plusYears(2), "")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertarVehiculo(
        db: SupportSQLiteDatabase, id: String, marca: String, modelo: String,
        anio: Int, color: String, placas: String, kilometraje: Double
    ) {
        db.execSQL(
            "INSERT INTO vehiculos (id, usuario_id, marca, modelo, anio, color, placas, kilometraje, km_actualizado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(id, DemoData.USER_ID, marca, modelo, anio, color, placas, kilometraje, LocalDate.now().toEpochDay())
        )
    }

    private fun insertarMantenimiento(
        db: SupportSQLiteDatabase, tipo: String, fecha: LocalDate,
        kilometraje: Double, costo: Double, descripcion: String
    ) {
        db.execSQL(
            "INSERT INTO mantenimientos (id, vehiculo_id, tipo, fecha, kilometraje, costo, descripcion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(nuevoId(), DemoData.VEHICLE_1_ID, tipo, fecha.toEpochDay(), kilometraje, costo, descripcion)
        )
    }

    private fun insertarGasto(
        db: SupportSQLiteDatabase, categoria: String, monto: Double, fecha: LocalDate, descripcion: String,
        litros: Double? = null, km: Double? = null
    ) {
        db.execSQL(
            "INSERT INTO gastos (id, vehiculo_id, categoria, monto, fecha, descripcion, litros, kilometraje) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(nuevoId(), DemoData.VEHICLE_1_ID, categoria, monto, fecha.toEpochDay(), descripcion, litros, km)
        )
    }

    private fun insertarDocumento(db: SupportSQLiteDatabase, tipo: String, vence: LocalDate?, notas: String) {
        db.execSQL(
            "INSERT INTO documentos (id, vehiculo_id, tipo, vence, notas) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(nuevoId(), DemoData.VEHICLE_1_ID, tipo, vence?.toEpochDay(), notas)
        )
    }

    private fun insertarRecordatorio(db: SupportSQLiteDatabase, titulo: String, fecha: LocalDate, kilometraje: Double?) {
        db.execSQL(
            "INSERT INTO recordatorios (id, vehiculo_id, titulo, fecha, kilometraje, completado) VALUES (?, ?, ?, ?, ?, 0)",
            arrayOf<Any?>(nuevoId(), DemoData.VEHICLE_1_ID, titulo, fecha.toEpochDay(), kilometraje)
        )
    }

    private fun nuevoId() = UUID.randomUUID().toString()
}
