package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.MensajeResponse
import com.distribuerp.mobile.models.Proveedor
import com.distribuerp.mobile.models.ProveedorRequest
import com.distribuerp.mobile.models.ProveedorResponse
import com.distribuerp.mobile.models.ProveedoresResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProveedorRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerProveedores(
        onSuccess: (List<Proveedor>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getProveedores().enqueue(
            object : Callback<ProveedoresResponse> {

                override fun onResponse(
                    call: Call<ProveedoresResponse>,
                    response: Response<ProveedoresResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.proveedores != null) {
                        onSuccess(body.proveedores)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ProveedoresResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerProveedor(
        id: Int,
        onSuccess: (Proveedor) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getProveedor(id).enqueue(
            object : Callback<ProveedorResponse> {

                override fun onResponse(
                    call: Call<ProveedorResponse>,
                    response: Response<ProveedorResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.proveedor != null) {
                        onSuccess(body.proveedor)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ProveedorResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun crearProveedor(
        request: ProveedorRequest,
        onSuccess: (Proveedor) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.crearProveedor(request).enqueue(
            object : Callback<ProveedorResponse> {

                override fun onResponse(
                    call: Call<ProveedorResponse>,
                    response: Response<ProveedorResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.proveedor != null) {
                        onSuccess(body.proveedor)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ProveedorResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun actualizarProveedor(
        id: Int,
        request: ProveedorRequest,
        onSuccess: (Proveedor) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.actualizarProveedor(id, request).enqueue(
            object : Callback<ProveedorResponse> {

                override fun onResponse(
                    call: Call<ProveedorResponse>,
                    response: Response<ProveedorResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.proveedor != null) {
                        onSuccess(body.proveedor)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ProveedorResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun eliminarProveedor(
        id: Int,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.eliminarProveedor(id).enqueue(
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
