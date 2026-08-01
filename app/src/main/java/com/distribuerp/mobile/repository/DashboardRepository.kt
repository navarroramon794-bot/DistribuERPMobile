package com.distribuerp.mobile.repository

import com.distribuerp.mobile.api.ApiService
import com.distribuerp.mobile.api.RetrofitClient
import com.distribuerp.mobile.models.DashboardResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardRepository(
    private val api: ApiService = RetrofitClient.api
) {

    fun obtenerResumen(
        onSuccess: (DashboardResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        api.getDashboard().enqueue(
            object : Callback<DashboardResponse> {

                override fun onResponse(
                    call: Call<DashboardResponse>,
                    response: Response<DashboardResponse>
                ) {
                    val body = response.body()

                    if (response.isSuccessful && body != null) {
                        onSuccess(body)
                    } else {
                        onError(
                            Throwable(
                                "Error HTTP ${response.code()}"
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<DashboardResponse>,
                    t: Throwable
                ) {
                    onError(t)
                }
            }
        )
    }
}
