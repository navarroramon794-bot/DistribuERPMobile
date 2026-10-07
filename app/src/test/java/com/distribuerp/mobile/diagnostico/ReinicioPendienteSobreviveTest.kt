package com.distribuerp.mobile.diagnostico

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoOutbox
import com.distribuerp.mobile.data.local.EstadoVentaSincronizacion
import com.distribuerp.mobile.data.local.TiposOutbox
import com.distribuerp.mobile.repository.LineaSnapshot
import com.distribuerp.mobile.repository.VentaPendienteRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UAT-05 TEST A: ?la venta pendiente sobrevive al cierre/reinicio del proceso?
 *
 * Los demas tests de persistencia usan `Room.inMemoryDatabaseBuilder`, que no
 * modela el reinicio: al morir el proceso el archivo en disco es lo unico que
 * queda. Aqui la base se crea en disco, se cierra por completo (como al matar
 * el proceso) y se reabre para comprobar que `ventas_pendientes` y `outbox`
 * siguen ahi, con su estado original.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReinicioPendienteSobreviveTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val nombreBase = "diag_restart_${System.currentTimeMillis()}.db"

    private fun linea() = LineaSnapshot(
        productoId = 1,
        cantidad = 2.0,
        precioUnitario = 10.0,
        subtotal = 20.0
    )

    @After
    fun limpiar() {
        context.deleteDatabase(nombreBase)
    }

    @Test
    fun `la venta pendiente y su outbox sobreviven al cierre y reapertura de la base`() =
        runBlocking {
            val ventaIdLocal = UUID().let {
                val db = Room.databaseBuilder(context, AppDatabase::class.java, nombreBase)
                    .allowMainThreadQueries()
                    .build()
                val repo = VentaPendienteRepository(db)

                val creada = repo.crearVentaOffline(
                    empresaId = 1,
                    vendedorId = 5,
                    clienteId = 9,
                    formaPago = "CONTADO",
                    lineas = listOf(linea()),
                    clientOperationId = "clave-reinicio-determinista"
                )
                db.close()
                creada.ventaIdLocal
            }

            // Apertura nueva: simula el proceso reabriendo la misma base en disco.
            val db = Room.databaseBuilder(context, AppDatabase::class.java, nombreBase)
                .allowMainThreadQueries()
                .build()

            val venta = db.ventaPendienteDao().obtenerVentaPorId(ventaIdLocal)
            assertNotNull("La venta pendiente debe sobrevivir al reinicio", venta)
            assertEquals(
                "La venta debe seguir PENDIENTE_SYNC tras el reinicio",
                EstadoVentaSincronizacion.PENDIENTE_SYNC.valor,
                venta!!.estadoLocal
            )
            assertEquals(1, venta.empresaId)
            assertEquals(5, venta.vendedorId)

            val ops = db.outboxDao().obtenerTodas()
            assertEquals(1, ops.size)
            assertEquals(TiposOutbox.VENTA, ops[0].tipo)
            assertEquals(
                "La operacion de la outbox debe seguir PENDIENTE tras el reinicio",
                EstadoOutbox.PENDIENTE.valor,
                ops[0].estado
            )

            // Los detalles congelados tambien sobreviven.
            val detalles = db.ventaPendienteDao().obtenerDetallesPorVentaId(ventaIdLocal)
            assertEquals(1, detalles.size)

            db.close()
        }

    private fun UUID() = java.util.UUID.randomUUID().toString()
}