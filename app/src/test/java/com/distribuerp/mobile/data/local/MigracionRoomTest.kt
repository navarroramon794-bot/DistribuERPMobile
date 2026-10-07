package com.distribuerp.mobile.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigracionRoomTest {

    private lateinit var context: Context
    private val nombreDb = "migracion_test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(nombreDb)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(nombreDb)
    }

    /** Crea una base v1 real: solo la tabla de ubicaciones, sin outbox. */
    private fun crearBaseV1() {
        val ruta = context.getDatabasePath(nombreDb)
        ruta.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(ruta, null)
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
            """
            INSERT INTO `ubicaciones_pendientes`
                (`vendedorId`,`latitud`,`longitud`,`precisionM`,`velocidad`,`fuente`,`fecha`,`timestampMs`)
            VALUES (7, 12.34, -56.78, 5.5, 1.2, 'fused', '2026-01-01T00:00:00Z', 1000)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `ubicaciones_pendientes`
                (`vendedorId`,`latitud`,`longitud`,`precisionM`,`velocidad`,`fuente`,`fecha`,`timestampMs`)
            VALUES (7, 1.11, 2.22, NULL, NULL, 'gps', '2026-01-02T00:00:00Z', 2000)
            """.trimIndent()
        )
        db.version = 1
        db.close()
    }

    private fun abrirV2() = Room.databaseBuilder(context, AppDatabase::class.java, nombreDb)
        .addMigrations(*MigracionesRoom.TODAS)
        .allowMainThreadQueries()
        .build()

    @Test
    fun `migracion 1 a 2 crea la tabla outbox`() = runBlocking {
        crearBaseV1()
        val db = abrirV2()

        val version = try {
            val f = AppDatabase::class.java.getDeclaredField("SCHEMA_VERSION")
            f.isAccessible = true
            f.getInt(null)
        } catch (e: Exception) {
            2
        }
        assertEquals(4, version)
        assertNotNull(db.outboxDao())
        assertTrue(db.outboxDao().contar() == 0)

        db.close()
    }

    @Test
    fun `migracion conserva las ubicaciones pendientes`() = runBlocking {
        crearBaseV1()
        val db = abrirV2()

        val ubicaciones = db.ubicacionPendienteDao().obtenerTodas()
        assertEquals(2, ubicaciones.size)
        assertEquals(12.34, ubicaciones[0].latitud, 0.0001)
        assertEquals(7, ubicaciones[0].vendedorId)
        assertEquals(1000L, ubicaciones[0].timestampMs)
        assertEquals("fused", ubicaciones[0].fuente)
        assertEquals(5.5, ubicaciones[0].precisionM!!, 0.0001)
        assertEquals(null, ubicaciones[1].precisionM)
        assertEquals(null, ubicaciones[1].velocidad)

        db.close()
    }

    @Test
    fun `outbox es usable despues de migrar`() = runBlocking {
        crearBaseV1()
        val db = abrirV2()

        val op = OutboxOperation(tipo = TiposOutbox.TEST, payload = """{"v":1}""")
        db.outboxDao().insertar(op)

        assertEquals(1, db.outboxDao().contar())
        assertEquals(op.uuid, db.outboxDao().obtenerPorUuid(op.uuid)!!.uuid)
        assertEquals(2, db.ubicacionPendienteDao().obtenerTodas().size)

        db.close()
    }

    @Test
    fun `base nueva se crea directamente en version 2 sin perder ubicaciones`() = runBlocking {
        val db = abrirV2()

        db.ubicacionPendienteDao().insertar(
            UbicacionPendienteEntity(
                vendedorId = 7,
                latitud = 9.9,
                longitud = 8.8,
                precisionM = null,
                velocidad = null,
                fuente = "test",
                fecha = "2026-01-03T00:00:00Z",
                timestampMs = 3000L
            )
        )

        assertEquals(1, db.ubicacionPendienteDao().obtenerTodas().size)
        assertEquals(0, db.outboxDao().contar())

        db.close()
    }
}