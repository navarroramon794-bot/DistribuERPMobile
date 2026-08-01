package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.InventarioResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InventarioRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerInventario(
        vendedorId: Int,
        onSuccess: (InventarioResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getInventario(vendedorId).enqueue(
            object : Callback<InventarioResponse> {

                override fun onResponse(
                    call: Call<InventarioResponse>,
                    response: Response<InventarioResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ok == true) {
                        onSuccess(body)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<InventarioResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }
}
