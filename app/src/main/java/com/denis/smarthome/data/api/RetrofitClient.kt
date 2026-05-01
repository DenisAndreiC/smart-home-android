/**
 * RetrofitClient.kt - Singleton for configuring the Retrofit HTTP client.
 *
 * Holds the single Retrofit instance used across the entire app.
 * Includes an AuthInterceptor that automatically attaches the JWT token to
 * every HTTP request, and an HttpLoggingInterceptor for network debug logging.
 *
 * Project: SmartHome IoT - Licenta CSIE-ASE 2025
 * Author: Denis Andrei C.
 */
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

/**
 * Kotlin singleton (object) that holds and exposes the single [ApiService] instance.
 *
 * Singleton pattern: `object` in Kotlin guarantees one instance for the entire app
 * lifetime. Must be initialized once via [init] before the first [apiService] access.
 * Initialization happens in MainActivity.
 *
 * Design decision: no DI framework (e.g. Hilt/Dagger) is used to keep the project
 * simple — a manual singleton is sufficient at this scale.
 */
object RetrofitClient {

    // BASE_URL points to the FastAPI backend.
    // 10.0.2.2 is the special loopback alias used in the Android Emulator to
    // reach localhost on the host machine (Mac/PC).
    // On a physical device, replace with the machine's real local IP
    // (e.g. "http://192.168.1.100:8000/") or a dynamic DNS.
    // Use "http://10.0.2.2:8000/" for Android Emulator
    // Use "http://192.168.x.x:8000/" for physical device (replace with your Mac's IP)
    const val EMULATOR_URL = "http://10.0.2.2:8000/api/"
    const val DEVICE_URL = "http://91.98.118.24:8000/api/"
    var BASE_URL = DEVICE_URL
        private set

    private var tokenManager: TokenManager? = null
    private var _apiService: ApiService? = null

    /**
     * Initializeaza RetrofitClient cu token manager-ul aplicatiei.
     * Trebuie apelat o singura data, in MainActivity, inainte de setContent.
     *
     * @param tokenManager instanta [TokenManager] pentru citirea token-ului JWT din DataStore
     */
    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
        // Construieste serviciul API imediat la initializare
        _apiService = buildApiService(tokenManager)
    }

    /**
     * Exposes the ready-to-use [ApiService] instance.
     * Throws if [init] was not called first — fail-fast design.
     */
    val apiService: ApiService
        get() = _apiService ?: error("RetrofitClient not initialized. Call RetrofitClient.init() first.")

    /**
     * Switches the backend URL and rebuilds Retrofit.
     * Useful for toggling between the emulator (10.0.2.2) and a physical device (LAN IP).
     */
    fun updateBaseUrl(url: String) {
        BASE_URL = url
        tokenManager?.let { _apiService = buildApiService(it) }
    }

    /**
     * Builds and configures the [ApiService] instance with a custom OkHttpClient.
     * Private — called once from [init].
     *
     * @param tokenManager used by the auth interceptor to read the current JWT token
     * @return configured [ApiService] instance
     */
    private fun buildApiService(tokenManager: TokenManager): ApiService {
        // Logging interceptor — prints all requests and responses to Logcat.
        // Level.BODY includes full headers and body, useful during development.
        // In production this should be reduced to NONE or BASIC.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Auth interceptor — automatically attaches "Authorization: Bearer <token>"
        // to every outgoing HTTP request.
        // runBlocking is required because Interceptor.intercept() is not a suspend
        // function, but getToken() returns a Flow. firstOrNull() collects the first
        // (and only) value emitted by DataStore.
        // If the token is null (user not authenticated), the header is not added and
        // the server will respond with 401 for protected endpoints.
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { tokenManager.getToken().firstOrNull() }
            val request = chain.request().newBuilder().apply {
                token?.let { header("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(request)
        }

        // Response interceptor: clears the saved JWT token when the server returns 401.
        // This ensures the app does not keep a stale/expired token in DataStore.
        // On the next app launch the startup check will redirect to Login automatically.
        val authErrorInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                runBlocking { tokenManager.clearToken() }
            }
            response
        }

        // Build OkHttpClient with interceptors and 30-second timeouts.
        // Order matters: authInterceptor runs first so the Authorization header
        // is visible in the logging interceptor output.
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(authErrorInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Build the Retrofit instance with:
        // - BASE_URL: backend address (can be changed at runtime via updateBaseUrl)
        // - GsonConverterFactory: auto-serializes/deserializes JSON <-> data classes
        // - custom OkHttp client with the interceptors configured above
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
