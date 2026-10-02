package com.distribuerp.mobile.sync

import android.content.Context
import com.distribuerp.mobile.data.SessionManager
import com.distribuerp.mobile.data.local.AppDatabase
import com.distribuerp.mobile.models.ClientesResponse
import com.distribuerp.mobile.models.ProductosResponse
import com.distribuerp.mobile.repository.ClienteLocalRepository
import com.distribuerp.mobile.repository.ProductoLocalRepository
import com.distribuerp.mobile.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.Response

object SincronizadorOffline {

    suspend fun sincronizarClientesProductos(context: Context): ResultadoSync = withContext(Dispatchers.IO) {
        val session = SessionManager(context)
        val usuario = session.sesion.first() ?: return@withContext ResultadoSync.ERROR("Sin sesiA3n")
        val empresaIdStr = usuario.empresa_id
        val empresaId = empresaIdStr?.toIntOrNull() ?: return@withContext ResultadoSync.ERROR("Sin empresa_id")

        val db = AppDatabase.getInstance(context)
        val clienteLocal = ClienteLocalRepository(db)
        val productoLocal = ProductoLocalRepository(db)

        var okClientes = false
        var okProductos = false

        try {
            val respClientes: Response<ClientesResponse> = RetrofitClient.api.getClientes().execute()
            if (respClientes.isSuccessful) {
                val body = respClientes.body()
                val lista = body?.clientes ?: emptyList()
                clienteLocal.sincronizar(empresaId, lista)
                okClientes = true
            }
        } catch (_: Exception) {
        }

        try {
            val respProd: Response<ProductosResponse> = RetrofitClient.api.getProductos().execute()
            if (respProd.isSuccessful) {
                val body = respProd.body()
                val lista = body?.productos ?: emptyList()
                productoLocal.sincronizar(empresaId, lista)
                okProductos = true
            }
        } catch (_: Exception) {
        }

        when {
            okClientes && okProductos -> ResultadoSync.OK
            okClientes || okProductos -> ResultadoSync.PARCIAL
            else -> ResultadoSync.ERROR("Sin conexiA3n o error")
        }
    }

    sealed class ResultadoSync {
        data object OK : ResultadoSync()
        data object PARCIAL : ResultadoSync()
        data class ERROR(val mensaje: String) : ResultadoSync()
    }
}
