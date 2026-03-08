/**
 * ApiService.kt - Interfata Retrofit pentru comunicarea cu backend-ul FastAPI
 *
 * Defineste toate endpoint-urile REST disponibile in aplicatie, grupate pe domenii:
 * autentificare, dispozitive, comenzi, scene si dashboard.
 * Fiecare functie este suspendata (suspend) pentru a fi apelata din coroutine.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.data.api

import com.denis.smarthome.data.model.*
import retrofit2.http.*

/**
 * Interfata principala Retrofit care mapeaza metodele Kotlin la apeluri HTTP.
 * Retrofit genereaza automat implementarea la runtime pe baza adnotatilor.
 * Toate functiile sunt `suspend` — se executa asincron in coroutine, fara a bloca UI-ul.
 */
interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Inregistreaza un utilizator nou in sistem.
     *
     * POST /auth/register
     * Trimite un JSON cu name, email si password.
     * Returneaza un [TokenResponse] cu access_token JWT daca inregistrarea a reusit.
     *
     * @param body datele de inregistrare (name, email, password) serializate ca JSON
     * @return [TokenResponse] cu token-ul JWT si tipul sau
     */
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): UserResponse

    /**
     * Autentifica un utilizator existent.
     *
     * POST /auth/login — FORM-ENCODED, nu JSON!
     * Backend-ul FastAPI foloseste OAuth2PasswordRequestForm, deci campurile
     * trebuie trimise ca application/x-www-form-urlencoded (nu ca JSON body).
     * De aceea se folosesc @FormUrlEncoded si @Field in loc de @Body.
     * Campul se numeste "username" in standard OAuth2, chiar daca aplicatia
     * primeste adresa de email.
     *
     * @param username adresa de email a utilizatorului (camp OAuth2 standard)
     * @param password parola in text clar (HTTPS o protejeaza in productie)
     * @return [TokenResponse] cu access_token JWT valid
     */
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    /**
     * Obtine profilul utilizatorului curent autentificat.
     *
     * GET /auth/me
     * Endpoint protejat — necesita header Authorization: Bearer <token>.
     * Token-ul este adaugat automat de AuthInterceptor din RetrofitClient.
     *
     * @return [UserResponse] cu id, name, email si data crearii contului
     */
    @GET("auth/me")
    suspend fun getMe(): UserResponse

    // ── Devices ───────────────────────────────────────────────────────────────

    /**
     * Returneaza lista tuturor dispozitivelor inregistrate pentru utilizatorul curent.
     *
     * GET /devices
     * Endpoint protejat — token-ul JWT este necesar.
     *
     * @return lista de [DeviceResponse] cu toate dispozitivele utilizatorului
     */
    @GET("devices")
    suspend fun getDevices(): List<DeviceResponse>

    /**
     * Creeaza un dispozitiv nou.
     *
     * POST /devices
     * Trimite configuratia dispozitivului ca JSON body.
     *
     * @param body datele noului dispozitiv (name, device_type, room, mqtt_topic etc.)
     * @return [DeviceResponse] cu dispozitivul creat, inclusiv id-ul generat de server
     */
    @POST("devices")
    suspend fun createDevice(@Body body: DeviceRequest): DeviceResponse

    /**
     * Actualizeaza un dispozitiv existent.
     *
     * PUT /devices/{id}
     * Id-ul dispozitivului este inclus in path-ul URL-ului.
     *
     * @param id identificatorul unic al dispozitivului de actualizat
     * @param body noile date ale dispozitivului
     * @return [DeviceResponse] cu dispozitivul actualizat
     */
    @PUT("devices/{id}")
    suspend fun updateDevice(@Path("id") id: Int, @Body body: DeviceRequest): DeviceResponse

    /**
     * Sterge un dispozitiv din sistem.
     *
     * DELETE /devices/{id}
     * Nu returneaza body — backend-ul raspunde cu 204 No Content la succes.
     *
     * @param id identificatorul dispozitivului de sters
     */
    @DELETE("devices/{id}")
    suspend fun deleteDevice(@Path("id") id: Int)

    /**
     * Returneaza lista distincta a camerelor in care exista dispozitive.
     *
     * GET /devices/rooms
     * Folosita in DevicesListScreen pentru chip-urile de filtrare dupa camera.
     *
     * @return lista de string-uri reprezentand numele camerelor (ex: "Living", "Dormitor")
     */
    @GET("devices/rooms")
    suspend fun getRooms(): List<String>

    // ── Commands ──────────────────────────────────────────────────────────────

    /**
     * Trimite o comanda catre un dispozitiv (ex: ON, OFF, SET_TEMP).
     *
     * POST /commands/send
     * Backend-ul preia comanda, o publica pe topic-ul MQTT al dispozitivului
     * si/sau trimite semnal IR, in functie de tipul dispozitivului.
     *
     * @param body comanda de trimis (device_id, command_type, command_data optional)
     * @return [CommandResponse] cu mesaj de confirmare si status de succes
     */
    @POST("commands/send")
    suspend fun sendCommand(@Body body: CommandRequest): CommandResponse

    /**
     * Obtine istoricul comenzilor pentru un dispozitiv specific.
     *
     * GET /commands/history?device_id={deviceId}
     * Parametrul deviceId este trimis ca query parameter in URL.
     *
     * @param deviceId id-ul dispozitivului pentru care se cere istoricul
     * @return lista de [CommandHistoryResponse] cu comenzile anterioare
     */
    @GET("commands/history")
    suspend fun getCommandHistory(@Query("device_id") deviceId: Int): List<CommandHistoryResponse>

    // ── Scenes ────────────────────────────────────────────────────────────────

    /**
     * Returneaza toate scenele definite de utilizatorul curent.
     *
     * GET /scenes
     *
     * @return lista de [SceneResponse] cu scenele si actiunile lor
     */
    @GET("scenes")
    suspend fun getScenes(): List<SceneResponse>

    /**
     * Creeaza o scena noua cu o lista de actiuni pe dispozitive.
     *
     * POST /scenes
     *
     * @param body datele scenei (name, icon optional, lista de actiuni)
     * @return [SceneResponse] cu scena creata si id-ul sau
     */
    @POST("scenes")
    suspend fun createScene(@Body body: SceneRequest): SceneResponse

    /**
     * Executa o scena existenta — backend-ul trimite toate comenzile din scena.
     *
     * POST /scenes/{id}/execute
     * Backend-ul itereaza actiunile scenei, respectand delay_seconds dintre ele,
     * si trimite comenzile catre dispozitivele corespunzatoare.
     *
     * @param id identificatorul scenei de executat
     * @return Map cu mesaj de confirmare de la server (ex: {"status": "ok"})
     */
    @POST("scenes/{id}/execute")
    suspend fun executeScene(@Path("id") id: Int): Map<String, String>

    /**
     * Sterge o scena din sistem.
     *
     * DELETE /scenes/{id}
     *
     * @param id identificatorul scenei de sters
     */
    @DELETE("scenes/{id}")
    suspend fun deleteScene(@Path("id") id: Int)

    // ── Dashboard ─────────────────────────────────────────────────────────────

    /**
     * Obtine statisticile agregate pentru dashboard-ul principal.
     *
     * GET /dashboard/stats
     * Returneaza numar total de dispozitive, dispozitive active, comenzi trimise
     * azi si cel mai utilizat dispozitiv.
     *
     * @return [DashboardStats] cu metricile calculate de backend
     */
    @GET("dashboard/stats")
    suspend fun getDashboardStats(): DashboardStats

    /**
     * Obtine lista de activitati recente (feed de activitate).
     *
     * GET /dashboard/activity?limit={limit}
     * Limita implicita este 10 inregistrari, parametrizabila prin query param.
     *
     * @param limit numarul maxim de activitati de returnat (implicit 10)
     * @return lista de [ActivityResponse] cu activitatile recente, cele mai noi primele
     */
    @GET("dashboard/activity")
    suspend fun getDashboardActivity(@Query("limit") limit: Int = 10): List<ActivityResponse>
}
