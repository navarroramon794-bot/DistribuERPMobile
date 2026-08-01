package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.MensajeResponse
import com.distribuerp.mobile.models.Vendedor
import com.distribuerp.mobile.models.VendedorRequest
import com.distribuerp.mobile.models.VendedorResponse
import com.distribuerp.mobile.models.VendedoresResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VendedorRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerVendedores(
        onSuccess: (List<Vendedor>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getVendedores().enqueue(
            object : Callback<VendedoresResponse> {

                override fun onResponse(
                    call: Call<VendedoresResponse>,
                    response: Response<VendedoresResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.vendedores != null) {
                        onSuccess(body.vendedores)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<VendedoresResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerVendedor(
        id: Int,
        onSuccess: (Vendedor) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getVendedor(id).enqueue(
            object : Callback<VendedorResponse> {

                override fun onResponse(
                    call: Call<VendedorResponse>,
                    response: Response<VendedorResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.vendedor != null) {
                        onSuccess(body.vendedor)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<VendedorResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun crearVendedor(
        request: VendedorRequest,
        onSuccess: (Vendedor) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.crearVendedor(request).enqueue(
            object : Callback<VendedorResponse> {

                override fun onResponse(
                    call: Call<VendedorResponse>,
                    response: Response<VendedorResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.vendedor != null) {
                        onSuccess(body.vendedor)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<VendedorResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun actualizarVendedor(
        id: Int,
        request: VendedorRequest,
        onSuccess: (Vendedor) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.actualizarVendedor(id, request).enqueue(
            object : Callback<VendedorResponse> {

                override fun onResponse(
                    call: Call<VendedorResponse>,
                    response: Response<VendedorResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.vendedor != null) {
                        onSuccess(body.vendedor)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<VendedorResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun eliminarVendedor(
        id: Int,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.eliminarVendedor(id).enqueue(
            object : Callback<MensajeResponse> {

                override fun onResponse(
                    call: Call<MensajeResponse>,
                    response: Response<MensajeResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ok == true) {
                        onSuccess()
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<MensajeResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }
}
