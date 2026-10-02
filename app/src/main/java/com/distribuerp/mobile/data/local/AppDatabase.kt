package com.distribuerp.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UbicacionPendienteEntity::class, OutboxOperation::class],
    version = AppDatabase.SCHEMA_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ubicacionPendienteDao(): UbicacionPendienteDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        const val SCHEMA_VERSION = 2
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "distribuerp.db"
                )
                    .addMigrations(*MigracionesRoom.TODAS)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}