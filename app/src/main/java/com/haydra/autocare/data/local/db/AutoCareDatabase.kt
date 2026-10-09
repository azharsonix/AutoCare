package com.haydra.autocare.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

/** Las fechas se guardan como días desde época (1970-01-01) para poder ordenarlas en SQL. */
class Converters {
    @TypeConverter
    fun fromLocalDate(fecha: LocalDate?): Long? = fecha?.toEpochDay()

    @TypeConverter
    fun toLocalDate(dias: Long?): LocalDate? = dias?.let(LocalDate::ofEpochDay)
}

/**
 * Base de datos local de AutoCare (requisito no funcional de Persistencia).
 * Sustituye a los repositorios Fake* en memoria: los datos sobreviven al
 * cerrar la app. Cuando se conecte Supabase podrá usarse como caché offline.
 */
@Database(
    entities = [
        UserEntity::class,
        VehicleEntity::class,
        MaintenanceEntity::class,
        ExpenseEntity::class,
        ReminderEntity::class,
        DocumentoEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AutoCareDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun reminderDao(): ReminderDao
    abstract fun documentoDao(): DocumentoDao

    companion object {
        private const val NOMBRE = "autocare.db"

        fun create(context: Context): AutoCareDatabase =
            Room.databaseBuilder(context.applicationContext, AutoCareDatabase::class.java, NOMBRE)
                .addCallback(DemoSeedCallback())
                .addMigrations(MIGRACION_1_2)
                .build()
    }
}

/**
 * v2: rendimiento de gasolina (litros y km en gastos), foto del comprobante
 * en gastos y mantenimientos, fecha del último kilometraje y la tabla de
 * documentos. Los registros existentes se conservan.
 */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE gastos ADD COLUMN litros REAL")
        db.execSQL("ALTER TABLE gastos ADD COLUMN kilometraje REAL")
        db.execSQL("ALTER TABLE gastos ADD COLUMN foto TEXT")
        db.execSQL("ALTER TABLE mantenimientos ADD COLUMN foto TEXT")
        db.execSQL("ALTER TABLE vehiculos ADD COLUMN km_actualizado INTEGER")
        // Se toma hoy como último kilometraje para no avisar nada más actualizar la app.
        db.execSQL("UPDATE vehiculos SET km_actualizado = CAST(strftime('%s', 'now') AS INTEGER) / 86400")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `documentos` (`id` TEXT NOT NULL, `vehiculo_id` TEXT NOT NULL, " +
                "`tipo` TEXT NOT NULL, `vence` INTEGER, `foto` TEXT, `notas` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`vehiculo_id`) REFERENCES `vehiculos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_documentos_vehiculo_id` ON `documentos` (`vehiculo_id`)")
    }
}
