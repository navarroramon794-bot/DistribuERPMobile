package com.distribuerp.mobile.sync

import android.content.Context
import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.data.local.EstadoVentaSincronizacion
import com.distribuerp.mobile.data.local.OutboxOperation
import com.distribuerp.mobile.data.local.TiposOutbox
import com.distribuerp.mobile.models.VentaRequest
import com.distribuerp.mobile.models.VentaResponse
import com.distribuerp.mobile.repository.PayloadVenta
import com.google.gson.Gson
import com.google.gson.JsonParser
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

/**
 * Seam minimo que necesita el enviador.
 *
 * Evita depender de `ApiService` completo para una sola llamada y permite
 * probar el enviador sin red.
 */
fun interface EnviadorVentaApi {
    fun crearVenta(
        request: VentaRequest,
        headers: Map<String, String>
    ): Call<VentaResponse>
}

/**
 * Enviador de operaciones Outbox de tipo VENTA.
 *
 * Reenvia SIEMPRE la misma `clientOperationId` como `X-Idempotency-Key`, de modo
 * que un reintento por timeout o perdida de respuesta nunca duplica la venta:
 * el backend responde 200 con la venta ya creada.
 */
class EnviadorVenta(
    private val api: EnviadorVentaApi,
    private val db: AppDatabase,
    private val gson: Gson = Gson()
) : EnviadorOutbox {

    override suspend fun enviar(op: OutboxOperation): ResultadoEnvio {
        val payload = try {
            gson.fromJson(op.payload, PayloadVenta::class.java)
        } catch (e: Exception) {
            // Payload ilegible: no se reintenta, no se pierde la venta.
            marcarDefinitivo(op, "Payload de venta ilegible: ${e.message}")
            return ResultadoEnvio.Descartar("payload_ilegible")
        }

        val headers = mapOf("X-Idempotency-Key" to payload.clientOperationId)

        return try {
            val response: Response<VentaResponse> =
                api.crearVenta(payload.request, headers).execute()

            when {
                response.isSuccessful -> {
                    val venta = response.body()?.venta
                    marcarSincronizada(op, venta?.folio, venta?.id)
                    ResultadoEnvio.Confirmado
                }

                response.code() == 401 || response.code() == 302 -> {
                    // Sesion HTTP inexistente o expirada: no es un rechazo del
                    // dato, es falta de autenticacion. La venta no se elimina,
                    // queda recuperable y se reintenta tras un nuevo login.
                    val codigo = response.code()
                    val motivo = mensajeDeError(response)
                        ?: "HTTP $codigo"
                    marcarAuthRequerida(op, codigo, motivo)
                    ResultadoEnvio.AuthRequerida(codigo)
                }

                response.code() == 409 -> {
                    // La clave ya existe con un payload distinto: conflicto real.
                    marcarDefinitivo(
                        op,
                        "La clave de idempotencia ya se uso con otro contenido"
                    )
                    ResultadoEnvio.Descartar("idempotencia_conflicto")
                }

                else -> {
                    val codigo = response.code()
                    val motivo = mensajeDeError(response)
                        ?: "HTTP $codigo"

                    when {
                        codigo in 400..499 && codigo != 408 && codigo != 429 -> {
                            marcarDefinitivo(op, "HTTP $codigo: $motivo")
                            ResultadoEnvio.Descartar("http_$codigo")
                        }

                        else -> {
                            marcarRecuperable(op, "HTTP $codigo: $motivo")
                            ResultadoEnvio.Retry("http_$codigo")
                        }
                    }
                }
            }
        } catch (e: IOException) {
            // Sin red o timeout: se conserva y se reintenta con la misma key.
            marcarRecuperable(op, e.message ?: "sin_conexion")
            ResultadoEnvio.Retry("io")
        }
    }

    private suspend fun marcarSincronizada(
        op: OutboxOperation,
        folio: String?,
        ventaId: Int?
    ) {
        ventaLocalDe(op)?.let { local ->
            db.ventaPendienteDao().marcarSincronizada(
                id = local.id,
                folio = folio,
                ventaId = ventaId
            )
        }
    }

    private suspend fun marcarDefinitivo(op: OutboxOperation, motivo: String) {
        ventaLocalDe(op)?.let { local ->
            db.ventaPendienteDao().marcarError(
                id = local.id,
                estado = EstadoVentaSincronizacion.ERROR_DEFINITIVO.valor,
                error = motivo
            )
        }
    }

    private suspend fun marcarRecuperable(op: OutboxOperation, motivo: String) {
        ventaLocalDe(op)?.let { local ->
            db.ventaPendienteDao().marcarError(
                id = local.id,
                estado = EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
                error = motivo
            )
        }
    }

    private suspend fun marcarAuthRequerida(
        op: OutboxOperation,
        codigo: Int,
        motivo: String
    ) {
        // La venta sigue intacta y recuperable: se sincronizara sola cuando la
        // sesion vuelva a estar disponible tras un nuevo login.
        ventaLocalDe(op)?.let { local ->
            db.ventaPendienteDao().marcarError(
                id = local.id,
                estado = EstadoVentaSincronizacion.ERROR_RECUPERABLE.valor,
                error = "Sesión requerida (HTTP $codigo): $motivo"
            )
        }
    }

    /**
     * Resuelve la venta local a la que pertenece esta operacion.
     *
     * Se busca por (empresaId, clientOperationId) para respetar el aislamiento
     * multi-tenant: una operacion nunca toca la venta de otra empresa.
     */
    private suspend fun ventaLocalDe(
        op: OutboxOperation
    ): com.distribuerp.mobile.data.local.VentaPendienteEntity? {
        val payload = try {
            gson.fromJson(op.payload, PayloadVenta::class.java)
        } catch (e: Exception) {
            null
        } ?: return null

        return db.ventaPendienteDao().obtenerPorClientOperationId(
            empresaId = payload.empresaId,
            clientOpId = payload.clientOperationId
        )
    }

    private fun mensajeDeError(response: Response<*>): String? =
        response.errorBody()?.string()?.let { cuerpo ->
            try {
                JsonParser.parseString(cuerpo)
                    .asJsonObject
                    .get("mensaje")
                    ?.asString
            } catch (e: Exception) {
                null
            }
        }
}

/**
 * Conecta el enviador de ventas al registro real de Outbox, bajo el tipo
 * `TiposOutbox.VENTA`.
 *
 * Se invoca desde `SyncGeneralWorker.doWork()`, que es el unico consumidor de
 * `RegistroEnvio`, de modo que la resolucion de VENTA y la de cualquier otro
 * tipo pasan siempre por el mismo mapa.
 *
 * Se apoya en piezas ya existentes: `ApiService` via `RetrofitClient` para la
 * llamada HTTP y `AppDatabase` para leer y actualizar `ventas_pendientes`.
 * `SessionManager` no hace falta: la empresa y el vendedor viajan dentro del
 * payload de la operacion.
 *
 * Registrar dos veces el mismo tipo es seguro: `RegistroEnvio` guarda un unico
 * enviador por tipo, asi que la segunda llamada reemplaza a la anterior y nunca
 * quedan dos enviadores compitiendo por la misma operacion.
 */
fun registrarEnviadorVenta(
    context: Context,
    db: AppDatabase = AppDatabase.getInstance(context),
    api: ApiService = RetrofitClient.api
) {
    RegistroEnvio.registrar(
        TiposOutbox.VENTA,
        EnviadorVenta(
            api = EnviadorVentaApi { request, headers -> api.crearVenta(request, headers) },
            db = db
        )
    )
}