package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.Compra
import com.distribuerp.mobile.models.CompraRequest
import com.distribuerp.mobile.models.CompraResponse
import com.distribuerp.mobile.models.ComprasResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CompraRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerCompras(
        proveedorId: Int?,
        onSuccess: (List<Compra>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getCompras(proveedorId).enqueue(
            object : Callback<ComprasResponse> {

                override fun onResponse(
                    call: Call<ComprasResponse>,
                    response: Response<ComprasResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.compras != null) {
                        onSuccess(body.compras)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ComprasResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerCompra(
        id: Int,
        onSuccess: (Compra) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getCompra(id).enqueue(
            object : Callback<CompraResponse> {

                override fun onResponse(
                    call: Call<CompraResponse>,
                    response: Response<CompraResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.compra != null) {
                        onSuccess(body.compra)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<CompraResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun crearCompra(
        request: CompraRequest,
        onSuccess: (Compra) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.crearCompra(request).enqueue(
            object : Callback<CompraResponse> {

                override fun onResponse(
                    call: Call<CompraResponse>,
                    response: Response<CompraResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.compra != null) {
                        onSuccess(body.compra)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<CompraResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }
}
