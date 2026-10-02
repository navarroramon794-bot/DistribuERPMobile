package com.distribuerp.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigracionV2aV3Test {

    private lateinit var context: Context
    private val nombreDb = "migracion_v2_v3.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(nombreDb)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(nombreDb)
    }

    private fun crearBaseV1() {
        val ruta = context.getDatabasePath(nombreDb)
        ruta.parentFile?.mkdirs()
        val db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(ruta, null)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ubicaciones_pendientes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vendedorId` INTEGER NOT NULL,
                `latitud` REAL NOT NULL,
                `longitud` REAL NOT NULL,
                `precisionM` REAL,
                `velocidad` REAL,
                `fuente` TEXT NOT NULL,
                `fecha` TEXT NOT NULL,
                `timestampMs` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO `ubicaciones_pendientes` (`vendedorId`,`latitud`,`longitud`,`precisionM`,`velocidad`,`fuente`,`fecha`,`timestampMs`) VALUES (7,12.34,-56.78,5.5,1.2,'fused','2026-01-01T00:00:00Z',1000)"
        )
        db.version = 1
        db.close()
    }

    private fun crearBaseV2() {
        crearBaseV1()
        val ruta = context.getDatabasePath(nombreDb)
        val db = android.database.sqlite.SQLiteDatabase.openDatabase(ruta.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `outbox` (
                `uuid` TEXT NOT NULL,
                `tipo` TEXT NOT NULL,
                `payload` TEXT NOT NULL,
                `estado` TEXT NOT NULL,
                `intentos` INTEGER NOT NULL,
                `ultimo_error` TEXT,
                `creado_en` INTEGER NOT NULL,
                `enviar_despues` INTEGER NOT NULL,
                PRIMARY KEY(`uuid`)
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO `outbox` (`uuid`,`tipo`,`payload`,`estado`,`intentos`,`ultimo_error`,`creado_en`,`enviar_despues`) VALUES ('123e4567-e89b-12d3-a456-426614174000','TEST','{}','PENDIENTE',0,NULL,1000,0)")
        db.version = 2
        db.close()
    }

    @Test
    fun `migracion v2 a v3 preserva datos y crea tablas`() = runBlocking {
        crearBaseV2()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, nombreDb)
            .addMigrations(*MigracionesRoom.TODAS)
            .allowMainThreadQueries()
            .build()

        assertEquals(3, AppDatabase.SCHEMA_VERSION)
        assertEquals(1, db.ubicacionPendienteDao().obtenerTodas().size)
        assertEquals(1, db.outboxDao().contar())
        assertNotNull(db.outboxDao().obtenerPorUuid("123e4567-e89b-12d3-a456-426614174000"))

        db.close()
    }
}