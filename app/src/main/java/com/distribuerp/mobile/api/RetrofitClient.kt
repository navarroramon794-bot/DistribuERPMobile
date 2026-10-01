package com.distribuerp.mobile.api

import com.distribuerp.mobile.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val BASE_URL: String =
        BuildConfig.API_BASE_URL

    var onSessionExpirada: (() -> Unit)? = null

    private val cookieJar = SesionCookieJar()

    private val client =
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                val ruta = request.url().encodedPath()

                if (response.code() == 302 ||
                    response.code() == 401
                ) {
                    if (ruta.startsWith("/api/") &&
                        ruta != "/api/login"
                    ) {
                        onSessionExpirada?.invoke()
                    }
                }

                response
            }
            .build()

    val api: ApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }

    fun limpiarCookies() {
        cookieJar.limpiar()
    }

    fun tieneCookies(): Boolean =
        cookieJar.tieneCookies()
}
