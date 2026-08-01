package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.EstadoCuentaResponse
import com.distribuerp.mobile.models.Pago
import com.distribuerp.mobile.models.PagoRequest
import com.distribuerp.mobile.models.PagoResponse
import com.distribuerp.mobile.models.PagosResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CobranzaRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerPagos(
        onSuccess: (List<Pago>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getPagos().enqueue(
            object : Callback<PagosResponse> {

                override fun onResponse(
                    call: Call<PagosResponse>,
                    response: Response<PagosResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.pagos != null) {
                        onSuccess(body.pagos)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<PagosResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun crearPago(
        request: PagoRequest,
        onSuccess: (Pago) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.crearPago(request).enqueue(
            object : Callback<PagoResponse> {

                override fun onResponse(
                    call: Call<PagoResponse>,
                    response: Response<PagoResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.pago != null) {
                        onSuccess(body.pago)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<PagoResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerEstadoCuenta(
        clienteId: Int,
        onSuccess: (EstadoCuentaResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getEstadoCuenta(clienteId).enqueue(
            object : Callback<EstadoCuentaResponse> {

                override fun onResponse(
                    call: Call<EstadoCuentaResponse>,
                    response: Response<EstadoCuentaResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ok == true) {
                        onSuccess(body)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<EstadoCuentaResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }
}
