package com.denis.smarthome.data.api

import com.denis.smarthome.data.local.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Use "http://10.0.2.2:8000/" for Android Emulator
    // Use "http://192.168.x.x:8000/" for physical device (replace with your Mac's IP)
    const val BASE_URL = "http://10.0.2.2:8000/"

    private var tokenManager: TokenManager? = null
    private var _apiService: ApiService? = null

    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
        _apiService = buildApiService(tokenManager)
    }

    val apiService: ApiService
        get() = _apiService ?: error("RetrofitClient not initialized. Call RetrofitClient.init() first.")

    private fun buildApiService(tokenManager: TokenManager): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { tokenManager.getToken().firstOrNull() }
            val request = chain.request().newBuilder().apply {
                token?.let { header("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
