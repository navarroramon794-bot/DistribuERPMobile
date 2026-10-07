package com.distribuerp.mobile.data.local

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
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

/**
 * Upgrade real de la base de v3 a v4.
 *
 * Este test reproduce el camino que fallo en el dispositivo fisico: una
 * instalacion existente con la base en version 3 que abre la app ya en v4.
 *
 * `MigracionRoomTest` solo ejercita 1->2 y `MigracionV2aV3Test` solo 2->3, de
 * modo que el salto 3->4 nunca se validaba contra las entidades. Room, en
 * cambio, valida columnas E indices despues de migrar, asi que un `CREATE
 * INDEX` en la migracion que la entidad no declara aborta la apertura con
 * `IllegalStateException: Migration didn't properly handle:
 * ventas_pendientes(...)` y la app no arranca en ninguna instalacion existente.
 *
 * El esquema v3 NO se escribe a mano: se obtiene ejecutando las migraciones de
 * produccion (1->2 y 2->3) sobre una base v1 real. Asi el punto de partida es
 * exactamente el que genera el codigo de la app y no una copia que pueda
 * desincronizarse.
 *
 * No requiere `androidx.room:room-testing`: la validacion se consigue abriendo
 * la base con Room, que es la misma ruta que recorre la app en el dispositivo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigracionV3aV4Test {

    private lateinit var context: Context
    private lateinit var nombreDb: String
    private var room: AppDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        nombreDb = "prueba_v3a_v4.db"
        context.deleteDatabase(nombreDb)
    }

    @After
    fun tearDown() {
        room?.close()
        room = null
        context.deleteDatabase(nombreDb)
    }

    // ------------------------------------------------------------------
    // Construccion de una base v3 representativa
    // ------------------------------------------------------------------

    /**
     * Base v1 real: solo `ubicaciones_pendientes`, que es lo que tenian las
     * instalaciones mas antiguas.
     */
    private fun crearBaseV1ConDatos() {
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

    /**
     * Sube la base v1 a version 3 con las migraciones de produccion y la deja
     * con datos de una instalacion que ya venia usando la app.
     *
     * Se usa el helper de framework porque `Migration.migrate` espera un
     * `SupportSQLiteDatabase`, y las migraciones invocadas son las mismas que
     * registra `AppDatabase`.
     */
    private fun subirABaseV3ConDatos() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(nombreDb)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)

        try {
            val db = helper.writableDatabase

            MigracionesRoom.MIGRACION_1_2.migrate(db)
            MigracionesRoom.MIGRACION_2_3.migrate(db)

            // Datos que existian antes del upgrade.
            db.execSQL(
                """
                INSERT INTO `clientes`
                    (`empresa_id`,`id`,`nombre`,`direccion`,`telefono`,`limite_credito`,
                     `activo`,`fecha_creacion`,`credito_autorizado`,`dias_credito`,`bloqueado`)
                VALUES (3, 11, 'ACME', 'Calle 1', '555', 1000.0, 1, '2026-01-01', 1, 30, 0)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `productos`
                    (`empresa_id`,`id`,`codigo`,`nombre`,`descripcion`,`costo`,`precio`,
                     `existencia`,`codigo_barras`,`tipo_codigo`,`unidad_venta`,`activo`,`fecha_creacion`)
                VALUES (3, 77, 'WID-077', 'Widget', 'Widget de prueba', 10.0, 25.5,
                        10.0, '7501234567890', 'EAN13', 'PZA', 1, '2026-01-01')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `outbox`
                    (`uuid`,`tipo`,`payload`,`estado`,`intentos`,`ultimo_error`,`creado_en`,`enviar_despues`)
                VALUES ('uuid-pendiente','VENTA','{}','PENDIENTE',0,NULL,1000,0)
                """.trimIndent()
            )

            db.version = 3
        } finally {
            helper.close()
        }
    }

    /** Prepara una base en version 3 exacta, como la de una instalacion real. */
    private fun prepararBaseV3ConDatos() {
        crearBaseV1ConDatos()
        subirABaseV3ConDatos()
    }

    // ------------------------------------------------------------------
    // Apertura real con Room: aqui ocurre la migracion 3->4 y la validacion
    // ------------------------------------------------------------------

    private fun abrirConRoom(): AppDatabase {
        val existente = room
        if (existente != null) return existente

        val db = Room.databaseBuilder(context, AppDatabase::class.java, nombreDb)
            .addMigrations(*MigracionesRoom.TODAS)
            .allowMainThreadQueries()
            .build()

        // Forzar la apertura: dispara onUpgrade(3 -> 4) y la validacion de Room.
        db.openHelper.writableDatabase

        room = db
        return db
    }

    // ------------------------------------------------------------------
    // TEST 1: el upgrade 3 -> 4 valida contra las entidades actuales
    // ------------------------------------------------------------------

    @Test
    fun `migracion 3 a 4 valida el esquema contra las entidades actuales`() {
        prepararBaseV3ConDatos()

        // Si la migracion creara indices que la entidad no declara, esta
        // apertura lanzaria IllegalStateException con
        // "Migration didn't properly handle: ventas_pendientes(...)".
        val db = abrirConRoom()

        assertNotNull(
            "La base debe abrir sin 'Migration didn't properly handle'",
            db.openHelper.writableDatabase
        )
    }

    @Test
    fun `la migracion 3 a 4 deja los dos indices esperados en ventas_pendientes`() {
        prepararBaseV3ConDatos()

        val indices = leerIndices("ventas_pendientes")

        assertTrue(
            "Debe existir index_ventas_pendientes_empresaId, indices=$indices",
            indices.contains("index_ventas_pendientes_empresaId")
        )
        assertTrue(
            "Debe existir el indice sobre clientOperationId, indices=$indices",
            indices.contains("index_ventas_pendientes_clienteOperationId")
        )
    }

    /**
     * Este es el test que realmente atrapa el defecto.
     *
     * Abrir la base con Room no basta: bajo Robolectric la comparacion de
     * indices que Room ejecuta al validar no llega a dispararse, asi que una
     * entidad sin indices declarados pasaria el test aunque en un dispositivo
     * real la app no arrancara. Y `@Entity` tiene retencion SOURCE, asi que los
     * indices declarados no se pueden leer por reflexion.
     *
     * El invariante se comprueba entonces por otra via: una base migrada desde
     * la v3 debe quedar con EXACTAMENTE el mismo esquema que una base creada
     * nueva por Room en v4. Si difieren, Room rechazara la apertura del
     * dispositivo que se esta actualizando.
     *
     * Sin el fix: la instalacion nueva no crea indices y la migrada si, y este
     * test falla.
     */
    @Test
    fun `el esquema migrado desde la v3 es identico al de una base nueva`() {
        // A) Base migrada: parte de una v3 real y ejecuta MIGRACION_3_4.
        prepararBaseV3ConDatos()
        abrirConRoom()
        val indicesMigrados = leerIndices("ventas_pendientes").filtrarIndicesSqlite()
        room?.close()
        room = null

        // B) Base nueva: la crea Room directamente en v4 desde las entidades.
        val nombreNuevo = "prueba_v3a_v4_nueva.db"
        context.deleteDatabase(nombreNuevo)
        val nueva = Room.databaseBuilder(context, AppDatabase::class.java, nombreNuevo)
            .addMigrations(*MigracionesRoom.TODAS)
            .allowMainThreadQueries()
            .build()

        val indicesNuevos = mutableSetOf<String>()
        nueva.openHelper.readableDatabase
            .query("PRAGMA index_list(`ventas_pendientes`)")
            .use { c ->
                val columna = c.getColumnIndex("name")
                while (c.moveToNext()) {
                    indicesNuevos.add(c.getString(columna))
                }
            }
        val esperados = indicesNuevos.filtrarIndicesSqlite()

        nueva.close()
        context.deleteDatabase(nombreNuevo)

        assertEquals(
            "Una base migrada desde la v3 debe quedar igual que una base nueva: " +
                "si difieren, Room rechazara la apertura en el dispositivo",
            esperados,
            indicesMigrados
        )
    }

    /** Los indices que crea SQLite para la primary key no los declara Room. */
    private fun Set<String>.filtrarIndicesSqlite(): Set<String> =
        filterNot { it.startsWith("sqlite_autoindex_") }.toSet()

    @Test
    fun `la migracion 3 a 4 no altera la tabla outbox`() {
        prepararBaseV3ConDatos()

        // outbox se creo en 1->2: la migracion 3->4 no debe afectarla.
        assertEquals(1, contar("SELECT COUNT(*) FROM `outbox`"))
    }

    // ------------------------------------------------------------------
    // TEST 2: los datos de la v3 sobreviven al upgrade
    // ------------------------------------------------------------------

    @Test
    fun `los datos existentes de la version 3 sobreviven a la migracion 3 a 4`() {
        prepararBaseV3ConDatos()
        abrirConRoom()

        // ubicaciones_pendientes (creada en v1)
        assertEquals(2, contar("SELECT COUNT(*) FROM `ubicaciones_pendientes`"))
        assertEquals(7L, entero("SELECT `vendedorId` FROM `ubicaciones_pendientes` WHERE `id` = 1"))
        assertEquals(
            "gps",
            texto("SELECT `fuente` FROM `ubicaciones_pendientes` WHERE `id` = 2")
        )

        // clientes (creada en 2->3): tenant, identidad y limite de credito
        assertEquals(3L, entero("SELECT `empresa_id` FROM `clientes` WHERE `id` = 11"))
        assertEquals(11L, entero("SELECT `id` FROM `clientes` WHERE `id` = 11"))
        assertEquals("ACME", texto("SELECT `nombre` FROM `clientes` WHERE `id` = 11"))
        assertEquals(1000.0, decimal("SELECT `limite_credito` FROM `clientes` WHERE `id` = 11"), 0.001)

        // productos (creada en 2->3)
        assertEquals(77L, entero("SELECT `id` FROM `productos` WHERE `id` = 77"))
        assertEquals(25.5, decimal("SELECT `precio` FROM `productos` WHERE `id` = 77"), 0.001)

        // outbox (creada en 1->2): su operacion pendiente sigue ahi
        assertEquals("uuid-pendiente", texto("SELECT `uuid` FROM `outbox`"))
        assertEquals("PENDIENTE", texto("SELECT `estado` FROM `outbox`"))
    }

    /**
     * `ventas_pendientes` la crea la propia migracion 3->4, asi que no puede
     * tener filas previas: una fila existente en esa tabla en version 3 seria
     * imposible por construccion.
     *
     * Lo que importa para OFFLINE-2B es que la tabla quede creada, vacia y
     * usable, con los indices que hacen posible consultarla por tenant y por
     * idempotencia. Aqui se inserta una fila post-migracion y se relee para
     * comprobar que la tabla y los indices funcionan de verdad.
     */
    @Test
    fun `tras la migracion ventas_pendientes queda creada y usable`() = runBlocking {
        prepararBaseV3ConDatos()
        val db = abrirConRoom()

        assertEquals(
            "La migracion crea la tabla vacia",
            0,
            contar("SELECT COUNT(*) FROM `ventas_pendientes`")
        )

        db.ventaPendienteDao().insertarVenta(
            VentaPendienteEntity(
                id = "venta-1",
                clientOperationId = "op-abc",
                empresaId = 3,
                vendedorId = 8,
                clienteId = 11,
                formaPago = "CONTADO",
                fecha = 1_700_000_000_000L,
                subtotal = 51.0,
                total = 51.0,
                estadoLocal = "PENDIENTE_SYNC"
            )
        )

        val guardada = db.ventaPendienteDao()
            .obtenerPorClientOperationId(3, "op-abc")

        assertNotNull("La fila debe poder insertarse y leerse", guardada)
        assertEquals(3, guardada!!.empresaId)
        assertEquals(8, guardada.vendedorId)
        assertEquals(11, guardada.clienteId)
        assertEquals("op-abc", guardada.clientOperationId)
        assertEquals("CONTADO", guardada.formaPago)
        assertEquals(51.0, guardada.total, 0.001)

        // Consulta por tenant, la que aprovecha index_ventas_pendientes_empresaId
        assertEquals(
            "La consulta por tenant debe encontrar la venta",
            1,
            db.ventaPendienteDao().obtenerPorEmpresa(3).size
        )
    }

    // ------------------------------------------------------------------
    // Lectura directa por SQL
    // ------------------------------------------------------------------

    private fun leerIndices(tabla: String): Set<String> {
        val nombres = mutableSetOf<String>()
        abrirConRoom().openHelper.readableDatabase
            .query("PRAGMA index_list(`$tabla`)")
            .use { c ->
                val columna = c.getColumnIndex("name")
                while (c.moveToNext()) {
                    nombres.add(c.getString(columna))
                }
            }
        return nombres
    }

    private fun leer(sql: String, Accion: (Cursor) -> Unit) {
        abrirConRoom().openHelper.readableDatabase.query(sql).use { c ->
            assertTrue("La consulta debe traer una fila: $sql", c.moveToFirst())
            Accion(c)
        }
    }

    private fun contar(sql: String): Int {
        var resultado = 0
        leer(sql) { c -> resultado = c.getInt(0) }
        return resultado
    }

    private fun entero(sql: String): Long {
        var resultado = 0L
        leer(sql) { c -> resultado = c.getLong(0) }
        return resultado
    }

    private fun decimal(sql: String): Double {
        var resultado = 0.0
        leer(sql) { c -> resultado = c.getDouble(0) }
        return resultado
    }

    private fun texto(sql: String): String {
        lateinit var resultado: String
        leer(sql) { c -> resultado = c.getString(0) }
        return resultado
    }
}
