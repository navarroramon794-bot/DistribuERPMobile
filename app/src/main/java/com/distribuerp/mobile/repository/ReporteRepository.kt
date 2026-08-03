package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.ReporteClientesSaldoResponse
import com.distribuerp.mobile.models.ReportePagosResponse
import com.distribuerp.mobile.models.ReporteProductosResponse
import com.distribuerp.mobile.models.ReporteVentasResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReporteRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerVentas(
        desde: String?,
        hasta: String?,
        onSuccess: (ReporteVentasResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getReporteVentas(desde, hasta).enqueue(
            object : Callback<ReporteVentasResponse> {

                override fun onResponse(
                    call: Call<ReporteVentasResponse>,
                    response: Response<ReporteVentasResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ok == true) {
                        onSuccess(body)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ReporteVentasResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerPagos(
        desde: String?,
        hasta: String?,
        onSuccess: (ReportePagosResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getReportePagos(desde, hasta).enqueue(
            object : Callback<ReportePagosResponse> {

                override fun onResponse(
                    call: Call<ReportePagosResponse>,
                    response: Response<ReportePagosResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ok == true) {
                        onSuccess(body)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ReportePagosResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerProductos(
        desde: String?,
        hasta: String?,
        onSuccess: (ReporteProductosResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getReporteProductos(desde, hasta).enqueue(
            object : Callback<ReporteProductosResponse> {

                override fun onResponse(
                    call: Call<ReporteProductosResponse>,
                    response: Response<ReporteProductosResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ok == true) {
                        onSuccess(body)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ReporteProductosResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun obtenerClientesSaldo(
        onSuccess: (ReporteClientesSaldoResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getReporteClientesSaldo().enqueue(
            object : Callback<ReporteClientesSaldoResponse> {

                override fun onResponse(
                    call: Call<ReporteClientesSaldoResponse>,
                    response: Response<ReporteClientesSaldoResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body?.ok == true) {
                        onSuccess(body)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ReporteClientesSaldoResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }

    fun exportar(
        reporte: String,
        formato: String,
        desde: String?,
        hasta: String?,
        onSuccess: (Response<ResponseBody>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.exportarReporte(
            reporte,
            formato,
            desde,
            hasta
        ).enqueue(
            object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        onSuccess(response)
                    } else {
                        onError(extraerError(response))
                    }
                }

                override fun onFailure(
                    call: Call<ResponseBody>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }
}
