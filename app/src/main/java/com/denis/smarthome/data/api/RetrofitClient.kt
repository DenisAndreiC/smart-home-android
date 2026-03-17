/**
 * RetrofitClient.kt - Singleton pentru configurarea clientului HTTP Retrofit
 *
 * Configureaza instanta unica Retrofit folosita in toata aplicatia.
 * Include AuthInterceptor care ataseaza automat token-ul JWT la fiecare request HTTP,
 * si HttpLoggingInterceptor pentru debug-ul traficului de retea.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
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
 * Singleton Kotlin (object) care detine si expune instanta unica de [ApiService].
 *
 * Pattern Singleton: `object` in Kotlin garanteaza o singura instanta pe durata
 * intregii aplicatii. Trebuie initializat o data, prin apelul [init], inainte de
 * primul acces la [apiService]. Initializarea se face in MainActivity.
 *
 * Design decision: nu se foloseste un framework DI (ex: Hilt/Dagger) pentru a
 * pastra proiectul simplu — Singleton manual este suficient pentru aceasta scara.
 */
object RetrofitClient {

    // BASE_URL specifica adresa backend-ului FastAPI.
    // 10.0.2.2 este adresa speciala folosita in emulatorul Android pentru a
    // accesa localhost-ul masinii gazda (Mac/PC de dezvoltare).
    // Pe un dispozitiv fizic, ar trebui inlocuita cu IP-ul real al masinii
    // (ex: "http://192.168.1.100:8000/") sau un DNS dinamic.
    // Use "http://10.0.2.2:8000/" for Android Emulator
    // Use "http://192.168.x.x:8000/" for physical device (replace with your Mac's IP)
    const val EMULATOR_URL = "http://10.0.2.2:8000/api/"
    const val DEVICE_URL   = "http://192.168.100.184:8000/api/"

    var BASE_URL = EMULATOR_URL
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
     * Proprietate care expune instanta [ApiService] gata de utilizare.
     * Arunca exceptie daca [init] nu a fost apelat anterior — fail-fast design.
     */
    val apiService: ApiService
        get() = _apiService ?: error("RetrofitClient not initialized. Call RetrofitClient.init() first.")

    /**
     * Schimba URL-ul backend-ului si reconstruieste Retrofit.
     * Util pentru a comuta intre emulator (10.0.2.2) si dispozitiv fizic (IP local).
     */
    fun updateBaseUrl(url: String) {
        BASE_URL = url
        tokenManager?.let { _apiService = buildApiService(it) }
    }

    /**
     * Construieste si configureaza instanta [ApiService] cu OkHttpClient personalizat.
     * Metoda privata — apelata o singura data din [init].
     *
     * @param tokenManager folosit de AuthInterceptor pentru a prelua token-ul curent
     * @return instanta configurata de [ApiService]
     */
    private fun buildApiService(tokenManager: TokenManager): ApiService {
        // Interceptor de logging — afiseaza in Logcat toate request-urile si response-urile HTTP.
        // Level.BODY include header-ele si body-ul complet, util pentru debug.
        // In productie, nivelul ar trebui redus la NONE sau BASIC.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // AuthInterceptor — adauga automat header-ul "Authorization: Bearer <token>"
        // la FIECARE request HTTP trimis prin acest client.
        // runBlocking este necesar deoarece Interceptor.intercept() nu este o functie
        // suspendata, dar getToken() returneaza un Flow (asincron).
        // firstOrNull() colecteaza primul (si singurul) token emis din DataStore.
        // Daca token-ul este null (utilizator neautentificat), header-ul nu se adauga
        // si server-ul va returna 401 Unauthorized pentru endpoint-urile protejate.
        val authInterceptor = Interceptor { chain ->
            // Citire sincrona a token-ului din DataStore — necesara in contextul non-corutina
            val token = runBlocking { tokenManager.getToken().firstOrNull() }
            val request = chain.request().newBuilder().apply {
                // Adauga header-ul doar daca token-ul exista
                token?.let { header("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(request)
        }

        // Construieste OkHttpClient cu cei doi interceptori si timeout-uri de 30 secunde.
        // Ordinea interceptorilor conteaza: authInterceptor se executa inaintea loggingInterceptor,
        // astfel header-ul Authorization va fi vizibil in log-uri.
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Construieste instanta Retrofit cu:
        // - BASE_URL: adresa backend-ului
        // - GsonConverterFactory: serializeaza/deserializeaza automat JSON <-> data class
        // - client OkHttp personalizat cu interceptorii configurati mai sus
        return Retrofit.Builder()
            .baseUrl(BASE_URL) // foloseste BASE_URL curent (poate fi schimbat prin updateBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
