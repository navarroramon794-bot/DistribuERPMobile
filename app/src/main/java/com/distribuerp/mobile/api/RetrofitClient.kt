package com.distribuerp.mobile.api

import com.distribuerp.mobile.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val BASE_URL: String =
        BuildConfig.API_BASE_URL

    var onSessionExpirada: (() -> Unit)? = null

    private val cookieJar = object : CookieJar {

        private val cookies = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(
            url: HttpUrl,
            cookies: List<Cookie>
        ) {
            this.cookies.getOrPut(url.host()) { mutableListOf() }
                .addAll(cookies)
        }

        override fun loadForRequest(
            url: HttpUrl
        ): List<Cookie> =
            cookies[url.host()] ?: emptyList()

        fun limpiar() {
            cookies.clear()
        }

        fun tieneCookies(): Boolean =
            cookies.values.any { lista ->
                lista.any { it.expiresAt() > System.currentTimeMillis() }
            }
    }

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
                    response.code() == 401 ||
                    response.code() == 403
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
