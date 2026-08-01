package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.Cliente
import com.distribuerp.mobile.models.ClienteRequest
import com.distribuerp.mobile.models.ClienteResponse
import com.distribuerp.mobile.models.ClientesResponse
import com.distribuerp.mobile.models.MensajeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClienteRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerClientes(
        onSuccess: (List<Cliente>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getClientes().enqueue(
            object : Callback<ClientesResponse> {

                override fun onResponse(
                    call: Call<ClientesResponse>,
                    response: Response<ClientesResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.clientes != null) {
                        onSuccess(body.clientes)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ClientesResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerCliente(
        id: Int,
        onSuccess: (Cliente) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getCliente(id).enqueue(
            object : Callback<ClienteResponse> {

                override fun onResponse(
                    call: Call<ClienteResponse>,
                    response: Response<ClienteResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.cliente != null) {
                        onSuccess(body.cliente)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ClienteResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun crearCliente(
        request: ClienteRequest,
        onSuccess: (Cliente) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.crearCliente(request).enqueue(
            object : Callback<ClienteResponse> {

                override fun onResponse(
                    call: Call<ClienteResponse>,
                    response: Response<ClienteResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.cliente != null) {
                        onSuccess(body.cliente)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ClienteResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun actualizarCliente(
        id: Int,
        request: ClienteRequest,
        onSuccess: (Cliente) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.actualizarCliente(id, request).enqueue(
            object : Callback<ClienteResponse> {

                override fun onResponse(
                    call: Call<ClienteResponse>,
                    response: Response<ClienteResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.cliente != null) {
                        onSuccess(body.cliente)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ClienteResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun eliminarCliente(
        id: Int,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.eliminarCliente(id).enqueue(
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
