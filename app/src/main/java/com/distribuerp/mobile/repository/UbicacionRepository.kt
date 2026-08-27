package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.Ubicacion
import com.distribuerp.mobile.models.UbicacionRequest
import com.distribuerp.mobile.models.UbicacionResponse
import com.distribuerp.mobile.models.UbicacionesResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UbicacionRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun enviarUbicacion(
        request: UbicacionRequest,
        onSuccess: (Ubicacion) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.enviarUbicacion(request).enqueue(
            object : Callback<UbicacionResponse> {

                override fun onResponse(
                    call: Call<UbicacionResponse>,
                    response: Response<UbicacionResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ubicacion != null) {
                        onSuccess(body.ubicacion)
                    } else if (body?.ok == false && !body.mensaje.isNullOrBlank()) {
                        onError(Throwable(body.mensaje))
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<UbicacionResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerUltimaUbicacion(
        vendedorId: Int,
        onSuccess: (Ubicacion?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.obtenerUltimaUbicacion(vendedorId).enqueue(
            object : Callback<UbicacionResponse> {

                override fun onResponse(
                    call: Call<UbicacionResponse>,
                    response: Response<UbicacionResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful) {
                        onSuccess(body?.ubicacion)
                    } else if (body?.ok == false && !body.mensaje.isNullOrBlank()) {
                        onError(Throwable(body.mensaje))
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<UbicacionResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun cambiarEstadoUbicacion(
        activo: Boolean,
        onSuccess: (Boolean) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.cambiarEstadoUbicacion(mapOf("activo" to activo)).enqueue(
            object : Callback<UbicacionResponse> {

                override fun onResponse(
                    call: Call<UbicacionResponse>,
                    response: Response<UbicacionResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ok == true) {
                        onSuccess(activo)
                    } else if (body?.ok == false && !body.mensaje.isNullOrBlank()) {
                        onError(Throwable(body.mensaje))
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<UbicacionResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerUbicacionesActivas(
        onSuccess: (List<Ubicacion>?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.obtenerUbicacionesActivas().enqueue(
            object : Callback<UbicacionesResponse> {

                override fun onResponse(
                    call: Call<UbicacionesResponse>,
                    response: Response<UbicacionesResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ok == true) {
                        onSuccess(body.ubicaciones)
                    } else if (body?.ok == false && !body.mensaje.isNullOrBlank()) {
                        onError(Throwable(body.mensaje))
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<UbicacionesResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }
}
