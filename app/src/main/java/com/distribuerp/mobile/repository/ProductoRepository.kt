package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.MensajeResponse
import com.distribuerp.mobile.models.Producto
import com.distribuerp.mobile.models.ProductoRequest
import com.distribuerp.mobile.models.ProductoResponse
import com.distribuerp.mobile.models.ProductosResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductoRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerProductos(
        onSuccess: (List<Producto>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getProductos().enqueue(
            object : Callback<ProductosResponse> {

                override fun onResponse(
                    call: Call<ProductosResponse>,
                    response: Response<ProductosResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.productos != null) {
                        onSuccess(body.productos)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ProductosResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerProducto(
        id: Int,
        onSuccess: (Producto) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getProducto(id).enqueue(
            object : Callback<ProductoResponse> {

                override fun onResponse(
                    call: Call<ProductoResponse>,
                    response: Response<ProductoResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.producto != null) {
                        onSuccess(body.producto)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ProductoResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun crearProducto(
        request: ProductoRequest,
        onSuccess: (Producto) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.crearProducto(request).enqueue(
            object : Callback<ProductoResponse> {

                override fun onResponse(
                    call: Call<ProductoResponse>,
                    response: Response<ProductoResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.producto != null) {
                        onSuccess(body.producto)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ProductoResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun actualizarProducto(
        id: Int,
        request: ProductoRequest,
        onSuccess: (Producto) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.actualizarProducto(id, request).enqueue(
            object : Callback<ProductoResponse> {

                override fun onResponse(
                    call: Call<ProductoResponse>,
                    response: Response<ProductoResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.producto != null) {
                        onSuccess(body.producto)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ProductoResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun eliminarProducto(
        id: Int,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.eliminarProducto(id).enqueue(
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
