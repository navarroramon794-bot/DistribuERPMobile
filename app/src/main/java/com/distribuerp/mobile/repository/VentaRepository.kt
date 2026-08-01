package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.Venta
import com.distribuerp.mobile.models.VentaRequest
import com.distribuerp.mobile.models.VentaResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VentaRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun crearVenta(
        request: VentaRequest,
        onSuccess: (Venta) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.crearVenta(request).enqueue(
            object : Callback<VentaResponse> {

                override fun onResponse(
                    call: Call<VentaResponse>,
                    response: Response<VentaResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.venta != null) {
                        onSuccess(body.venta)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<VentaResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }
}
