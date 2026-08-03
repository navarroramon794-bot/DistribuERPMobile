package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.Carga
import com.distribuerp.mobile.models.CargaRequest
import com.distribuerp.mobile.models.CargaResponse
import com.distribuerp.mobile.models.CargasResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CargaRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerCargas(
        vendedorId: Int?,
        onSuccess: (List<Carga>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getCargas(vendedorId).enqueue(
            object : Callback<CargasResponse> {

                override fun onResponse(
                    call: Call<CargasResponse>,
                    response: Response<CargasResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.cargas != null) {
                        onSuccess(body.cargas)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<CargasResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerCarga(
        id: Int,
        onSuccess: (Carga) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getCarga(id).enqueue(
            object : Callback<CargaResponse> {

                override fun onResponse(
                    call: Call<CargaResponse>,
                    response: Response<CargaResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.carga != null) {
                        onSuccess(body.carga)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<CargaResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun crearCarga(
        request: CargaRequest,
        onSuccess: (Carga) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.crearCarga(request).enqueue(
            object : Callback<CargaResponse> {

                override fun onResponse(
                    call: Call<CargaResponse>,
                    response: Response<CargaResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.carga != null) {
                        onSuccess(body.carga)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<CargaResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }
}
