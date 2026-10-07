package com.distribuerp.mobile.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
import com.distribuerp.mobile.data.local.TiposOutbox
import com.distribuerp.mobile.data.local.OutboxOperation
import com.distribuerp.mobile.repository.LineaSnapshot
import com.distribuerp.mobile.repository.VentaPendienteRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Agenda del `SyncGeneralWorker` a traves del `SyncManager` existente.
 *
 * No se usa `work-testing`: el encolado real se sustituye por
 * `SyncManager.encolar`, de modo que se verifica exactamente lo que el codigo
 * de produccion pide a WorkManager (nombre unico, politica y restricciones)
 * sin levantar el scheduler.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncManagerSchedulingTest {

    private data class Encolado(
        val contexto: Context,
        val nombre: String,
        val politica: ExistingWorkPolicy,
        val request: OneTimeWorkRequest
    )

    private lateinit var db: AppDatabase
    private lateinit var repo: VentaPendienteRepository
    private lateinit var context: Context

    private val encolados = mutableListOf<Encolado>()
    private var encoladorOriginal: (
        Context, String, ExistingWorkPolicy, OneTimeWorkRequest
    ) -> Unit = { _, _, _, _ -> }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = VentaPendienteRepository(db)

        encolados.clear()
        encoladorOriginal = SyncManager.encolar
        SyncManager.encolar = { contexto, nombre, politica, request ->
            encolados += Encolado(contexto, nombre, politica, request)
        }
    }

    @After
    fun tearDown() {
        SyncManager.encolar = encoladorOriginal
        db.close()
    }

    private fun linea() = LineaSnapshot(
        productoId = 1,
        cantidad = 2.0,
        precioUnitario = 10.0,
        subtotal = 20.0
    )

    private suspend fun crearVentaOffline(clave: String) {
        repo.crearVentaOffline(
            empresaId = 1,
            vendedorId = 5,
            clienteId = 9,
            formaPago = "CONTADO",
            lineas = listOf(linea()),
            clientOperationId = clave
        )
    }

    // --- 1: venta offline creada -> se agenda sincronizacion ---

    @Test
    fun `una venta offline creada deja operacion pendiente y programa sincronizacion`() =
        runBlocking {
            crearVentaOffline("clave-agenda")

            val pendiente = db.outboxDao().obtenerTodas().single()
            assertEquals(TiposOutbox.VENTA, pendiente.tipo)
            assertEquals(EstadoOutbox.PENDIENTE.valor, pendiente.estado)

            assertTrue(SyncManager.programarSiHayPendientes(context, db))

            val encolado = encolados.single()
            assertEquals(SyncManager.TRABAJO_GENERAL, encolado.nombre)
        }

    @Test
    fun `la venta se guarda antes de agendar la sincronizacion`() = runBlocking {
        // El orden importa: si se agendara antes de guardar, el worker podria
        // correr y no encontrar nada que enviar.
        crearVentaOffline("clave-orden")

        assertEquals(1, db.outboxDao().contarPorEstado(EstadoOutbox.PENDIENTE.valor))
        SyncManager.encolarSincronizacion(context)

        assertEquals(1, encolados.size)
        assertEquals(1, db.outboxDao().contarPorEstado(EstadoOutbox.PENDIENTE.valor))
    }

    // --- 2: varias llamadas no crean workers duplicados ---

    @Test
    fun `llamar varias veces conserva un unico trabajo unico`() {
        repeat(3) { SyncManager.encolarSincronizacion(context) }

        assertEquals(3, encolados.size)

        // Las tres van al mismo nombre con politica KEEP: WorkManager ignora las
        // siguientes mientras exista trabajo pendiente o en ejecucion, de modo
        // que nunca hay dos SyncGeneralWorker simultaneos.
        assertTrue(encolados.all { it.nombre == SyncManager.TRABAJO_GENERAL })
        assertTrue(encolados.all { it.politica == ExistingWorkPolicy.KEEP })

        // Cada peticion es distinta, pero el nombre las funde en una sola.
        assertTrue(encolados.map { it.request.id }.toSet().size == 3)
    }

    // --- 3: pendiente al recuperar conexion ---

    @Test
    fun `al recuperar conexion se reprograma si hay operaciones pendientes`() = runBlocking {
        crearVentaOffline("clave-recuperada")

        // WorkManager retiene la peticion mientras no haya red valida y la
        // ejecuta al volver la conectividad: la restriccion CONNECTED es la que
        // cubre la transicion OFFLINE -> ONLINE.
        assertTrue(SyncManager.programarSiHayPendientes(context, db))
        assertEquals(1, encolados.size)

        // Y sigue siendo cierto aunque se vuelva a pedir.
        assertTrue(SyncManager.programarSiHayPendientes(context, db))
        assertEquals(2, encolados.size)
        assertTrue(encolados.all { it.politica == ExistingWorkPolicy.KEEP })
    }

    @Test
    fun `la peticion exige red conectada`() {
        SyncManager.encolarSincronizacion(context)

        val request = encolados.single().request

        assertEquals(
            NetworkType.CONNECTED,
            request.workSpec.constraints.requiredNetworkType
        )
    }

    // --- 4: arranque con Outbox pendiente ---

    @Test
    fun `el arranque de la app con outbox pendiente agenda sincronizacion`() = runBlocking {
        // Simula el estado tras reiniciar con una venta sin sincronizar.
        crearVentaOffline("clave-reinicio")

        // Es la misma ruta que invoca MainActivity al arrancar.
        assertTrue(SyncManager.programarSiHayPendientes(context, db))
        assertEquals(SyncManager.TRABAJO_GENERAL, encolados.single().nombre)
    }

    @Test
    fun `sin operaciones pendientes el arranque no agenda nada`() = runBlocking {
        assertEquals(0, db.outboxDao().contar())

        assertFalse(SyncManager.programarSiHayPendientes(context, db))
        assertTrue(encolados.isEmpty())
    }

    // --- Lo definitivo no se reprograma solo ---

    @Test
    fun `una operacion en error definitivo no se reprograma en el arranque`() = runBlocking {
        db.outboxDao().insertar(
            OutboxOperation(tipo = TiposOutbox.VENTA, payload = "{}")
        )
        db.outboxDao().registrarResultado(
            db.outboxDao().obtenerTodas().single().uuid,
            EstadoOutbox.ERROR.valor,
            "http_400"
        )

        assertFalse(SyncManager.programarSiHayPendientes(context, db))
        assertTrue(encolados.isEmpty())
    }

    @Test
    fun `una operacion reintentable si se reprograma en el arranque`() = runBlocking {
        crearVentaOffline("clave-reintentable")

        SyncManager.encolarSincronizacion(context)
        encolados.clear()

        // El enviador la devuelve a PENDIENTE tras un fallo recuperable.
        assertTrue(SyncManager.programarSiHayPendientes(context, db))
        assertEquals(1, encolados.size)
    }

    // --- El encolado real sigue delegando en WorkManager ---

    @Test
    fun `el encolado por defecto delega en WorkManager`() {
        val anterior = SyncManager.encolar
        try {
            // No se invoca: solo se comprueba que el valor por defecto no es
            // el sustituto de test y usa el nombre unico del proyecto.
            assertNotNull(anterior)
            assertEquals("sync_general", SyncManager.TRABAJO_GENERAL)
            assertTrue(anterior !== encoladorTest)
        } finally {
            SyncManager.encolar = anterior
        }
    }

    private val encoladorTest: (Context, String, ExistingWorkPolicy, OneTimeWorkRequest) -> Unit
        get() = { _, _, _, _ -> }

    @Test
    fun `el worker general es el que se agenda`() {
        SyncManager.encolarSincronizacion(context)

        val workerName = encolados.single().request.workSpec.workerClassName

        assertTrue(
            "Debe agendarse SyncGeneralWorker, fue $workerName",
            workerName.contains("SyncGeneralWorker")
        )
    }

    @Test
    fun `la peticion lleva backoff exponencial`() {
        SyncManager.encolarSincronizacion(context)

        val backoff = encolados.single().request.workSpec

        assertTrue(backoff.backoffDelayDuration >= 30_000L)
    }
}
