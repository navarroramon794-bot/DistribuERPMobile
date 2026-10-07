package com.distribuerp.mobile.repository

import androidx.room.withTransaction
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.DetalleVentaPendienteEntity
import com.distribuerp.mobile.data.local.EstadoVentaSincronizacion
import com.distribuerp.mobile.data.local.OutboxOperation
import com.distribuerp.mobile.data.local.TiposOutbox
import com.distribuerp.mobile.data.local.VentaPendienteEntity
import com.distribuerp.mobile.models.ItemVentaRequest
import com.distribuerp.mobile.models.VentaRequest
import com.google.gson.Gson
import java.util.UUID

/**
 * Payload persistido en la Outbox para una venta.
 *
 * `OutboxOperation` no tiene columna `clientOperationId`, asi que la clave de
 * idempotencia viaja dentro del payload. Esto permite reutilizar SIEMPRE la
 * misma clave en cada reintento, sin duplicar la venta en el backend.
 */
data class PayloadVenta(
    val clientOperationId: String,
    val empresaId: Int,
    val ventaIdLocal: String,
    val request: VentaRequest
)

/** Snapshot de precio taken al momento de crear la venta pendiente. */
data class LineaSnapshot(
    val productoId: Int,
    val cantidad: Double,
    val precioUnitario: Double,
    val subtotal: Double
)

/** Identificadores de la venta local recien creada. */
data class VentaOfflineRegistrada(
    val ventaIdLocal: String,
    val clientOperationId: String
)

class VentaPendienteRepository(
    private val db: AppDatabase,
    private val gson: Gson = Gson()
) {

    /**
     * Crea la venta pendiente y su operacion Outbox en una sola transaccion.
     *
     * - `clientOperationId` se genera UNA sola vez aqui y se persiste.
     * - Los precios quedan congelados en `detalles_ventas_pendientes`.
     * - No se genera folio comercial: `folioBackend` queda en null hasta que
     *   el backend responda.
     */
    suspend fun crearVentaOffline(
        empresaId: Int,
        vendedorId: Int,
        clienteId: Int,
        formaPago: String,
        lineas: List<LineaSnapshot>,
        clientOperationId: String = UUID.randomUUID().toString()
    ): VentaOfflineRegistrada {
        require(empresaId > 0) { "empresaId invalido" }
        require(vendedorId > 0) { "vendedorId invalido" }
        require(clienteId > 0) { "clienteId invalido" }
        require(lineas.isNotEmpty()) { "La venta no tiene productos" }

        val ventaIdLocal = UUID.randomUUID().toString()
        val ahora = System.currentTimeMillis()

        val subtotal = lineas.sumOf { it.subtotal }
        val total = lineas.sumOf { it.subtotal }

        val venta = VentaPendienteEntity(
            id = ventaIdLocal,
            clientOperationId = clientOperationId,
            empresaId = empresaId,
            vendedorId = vendedorId,
            clienteId = clienteId,
            formaPago = formaPago,
            fecha = ahora,
            subtotal = subtotal,
            total = total,
            estadoLocal = EstadoVentaSincronizacion.PENDIENTE_SYNC.valor,
            folioBackend = null,
            ventaIdBackend = null,
            error = null,
            intentos = 0,
            creadoEn = ahora,
            actualizadoEn = ahora
        )

        val detalles = lineas.map { linea ->
            DetalleVentaPendienteEntity(
                ventaId = ventaIdLocal,
                productoId = linea.productoId,
                cantidad = linea.cantidad,
                precioUnitario = linea.precioUnitario,
                subtotal = linea.subtotal
            )
        }

        val payload = gson.toJson(
            PayloadVenta(
                clientOperationId = clientOperationId,
                empresaId = empresaId,
                ventaIdLocal = ventaIdLocal,
                request = VentaRequest(
                    cliente_id = clienteId,
                    vendedor_id = vendedorId,
                    productos = lineas.map { lineaSnapshot ->
                        ItemVentaRequest(
                            producto_id = lineaSnapshot.productoId,
                            cantidad = lineaSnapshot.cantidad
                        )
                    },
                    forma_pago = formaPago
                )
            )
        )

        val outbox = OutboxOperation(
            tipo = TiposOutbox.VENTA,
            payload = payload
        )

        db.withTransaction {
            // Idempotencia local: si la clave ya existe para esta empresa se
            // devuelve la venta original en lugar de duplicarla.
            val existente = db.ventaPendienteDao()
                .obtenerPorClientOperationId(empresaId, clientOperationId)

            if (existente == null) {
                db.ventaPendienteDao().insertarVentaCompleta(venta, detalles)
                db.outboxDao().insertar(outbox)
            }
        }

        val guardada = db.ventaPendienteDao()
            .obtenerPorClientOperationId(empresaId, clientOperationId)

        return VentaOfflineRegistrada(
            ventaIdLocal = guardada?.id ?: ventaIdLocal,
            clientOperationId = clientOperationId
        )
    }

    /** Recupera el registro local a partir de la clave de idempotencia. */
    suspend fun obtenerPorClientOperationId(
        empresaId: Int,
        clientOperationId: String
    ): VentaPendienteEntity? =
        db.ventaPendienteDao().obtenerPorClientOperationId(empresaId, clientOperationId)

    suspend fun obtenerVentaCompleta(ventaIdLocal: String) =
        db.ventaPendienteDao().obtenerVentaCompleta(ventaIdLocal)
}