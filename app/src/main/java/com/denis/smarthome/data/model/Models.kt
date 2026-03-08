/**
 * Models.kt - Data class-urile pentru serializarea/deserializarea JSON cu backend-ul
 *
 * Contine toate modelele de date folosite la comunicarea prin API REST.
 * Modelele sunt grupate in 5 categorii: Auth, Devices, Commands, Scenes, Dashboard.
 * @SerializedName este folosit pentru a mapa campurile cu snake_case din JSON
 * la proprietati Kotlin cu camelCase sau snake_case explicit.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.data.model

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

/**
 * Request body pentru endpoint-ul POST /auth/register.
 * Trimis ca JSON (nu form-encoded) — diferit de login.
 * Campurile nu necesita @SerializedName deoarece numele Kotlin coincide cu JSON-ul asteptat.
 */
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Response primit de la server dupa login sau register cu succes.
 * Contine token-ul JWT care va fi stocat in DataStore prin TokenManager.
 *
 * @SerializedName("access_token") mapeaza campul "access_token" din JSON
 * (snake_case, standard OAuth2) la proprietatea Kotlin. Backend-ul FastAPI
 * foloseste snake_case conform conventiei Python/OAuth2.
 */
data class TokenResponse(
    // "access_token" in JSON (OAuth2 standard) -> accesat in Kotlin ca access_token
    @SerializedName("access_token") val access_token: String,
    // Tipul token-ului, de obicei "bearer" — folosit in header-ul Authorization
    @SerializedName("token_type") val token_type: String
)

/**
 * Response cu datele profilului utilizatorului autentificat.
 * Returnat de GET /auth/me — endpoint protejat cu JWT.
 */
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    // Data si ora crearii contului, serializata ca string ISO 8601 de catre FastAPI
    @SerializedName("created_at") val created_at: String
)

// ── Devices ───────────────────────────────────────────────────────────────────

/**
 * Request body pentru crearea sau actualizarea unui dispozitiv.
 * Folosit la POST /devices si PUT /devices/{id}.
 * Campurile snake_case sunt necesare pentru a corespunde schemei Pydantic din backend.
 */
data class DeviceRequest(
    val name: String,
    // Tipul dispozitivului (ex: "ac", "tv", "light") — determina comenzile disponibile
    @SerializedName("device_type") val device_type: String,
    // Camera in care se afla dispozitivul (ex: "Living", "Dormitor")
    val room: String,
    // Protocol IR optional (ex: "NEC", "SAMSUNG") — null pentru dispozitive doar MQTT
    @SerializedName("ir_protocol") val ir_protocol: String? = null,
    // Topic-ul MQTT pe care backend-ul publica comenzile pentru acest dispozitiv
    @SerializedName("mqtt_topic") val mqtt_topic: String
)

/**
 * Response cu datele complete ale unui dispozitiv, asa cum sunt stocate in baza de date.
 * Returnat de GET /devices (lista) si POST/PUT /devices (dupa creare/actualizare).
 */
data class DeviceResponse(
    // Id-ul unic generat de server (auto-increment in baza de date)
    val id: Int,
    val name: String,
    @SerializedName("device_type") val device_type: String,
    val room: String,
    @SerializedName("ir_protocol") val ir_protocol: String?,
    @SerializedName("mqtt_topic") val mqtt_topic: String,
    // Indica daca dispozitivul este online/activ in momentul interogarii
    @SerializedName("is_active") val is_active: Boolean,
    @SerializedName("created_at") val created_at: String
)

// ── Commands ──────────────────────────────────────────────────────────────────

/**
 * Request body pentru trimiterea unei comenzi catre un dispozitiv.
 * Folosit la POST /commands/send.
 * Backend-ul determina canalul de comunicare (MQTT sau IR) in functie de tipul dispozitivului.
 */
data class CommandRequest(
    // Id-ul dispozitivului destinatar al comenzii
    @SerializedName("device_id") val device_id: Int,
    // Tipul comenzii (ex: "ON", "OFF", "SET_TEMP", "SET_VOLUME")
    @SerializedName("command_type") val command_type: String,
    // Date aditionale pentru comanda (ex: "22" pentru temperatura) — optional
    @SerializedName("command_data") val command_data: String? = null
)

/**
 * Response primit dupa trimiterea unei comenzi.
 * Confirma ca backend-ul a primit si procesat comanda (nu garanteaza livrarea la dispozitiv).
 */
data class CommandResponse(
    // Mesaj descriptiv de la server (ex: "Command sent successfully")
    val message: String,
    // true daca comanda a fost acceptata de server, false in caz de eroare logica
    val success: Boolean = true
)

/**
 * Un element din istoricul comenzilor unui dispozitiv.
 * Returnat de GET /commands/history?device_id={id}.
 */
data class CommandHistoryResponse(
    val id: Int,
    @SerializedName("device_id") val device_id: Int,
    @SerializedName("command_type") val command_type: String,
    @SerializedName("command_data") val command_data: String?,
    // Timestamp-ul executiei comenzii (format ISO 8601, ex: "2025-03-08T14:30:00")
    @SerializedName("created_at") val created_at: String
)

// ── Scenes ────────────────────────────────────────────────────────────────────

/**
 * O actiune individuala dintr-o scena — o comanda programata pe un dispozitiv.
 * O scena contine o lista ordonata de astfel de actiuni, executate secvential.
 */
data class SceneAction(
    // Dispozitivul asupra caruia se aplica aceasta actiune
    @SerializedName("device_id") val device_id: Int,
    @SerializedName("command_type") val command_type: String,
    @SerializedName("command_data") val command_data: String? = null,
    // Intarzierea in secunde fata de actiunea anterioara — permite scenarii tip "stinge lumina dupa 5 secunde"
    @SerializedName("delay_seconds") val delay_seconds: Int = 0
)

/**
 * Request body pentru crearea unei scene noi.
 * Folosit la POST /scenes.
 */
data class SceneRequest(
    // Numele scenei afisata in UI (ex: "Film", "Buna dimineata")
    val name: String,
    // Emoji sau identificator de icon pentru afisare in UI — optional
    val icon: String? = null,
    // Lista de actiuni care compun scena, executate in ordine de server
    val actions: List<SceneAction>
)

/**
 * Response cu datele complete ale unei scene, inclusiv actiunile sale.
 * Returnat de GET /scenes si POST /scenes.
 */
data class SceneResponse(
    val id: Int,
    val name: String,
    val icon: String?,
    // Lista actiunilor scenei — folosita la afisarea detaliilor si la executie
    val actions: List<SceneAction>,
    // Indica daca scena este activa/disponibila (scena dezactivata nu poate fi executata)
    @SerializedName("is_active") val is_active: Boolean
)

// ── Dashboard ─────────────────────────────────────────────────────────────────

/**
 * Statistici agregate pentru ecranul principal (HomeScreen).
 * Returnat de GET /dashboard/stats — calculat de server in timp real.
 */
data class DashboardStats(
    // Numarul total de dispozitive inregistrate de utilizator
    @SerializedName("total_devices") val total_devices: Int,
    // Numarul de dispozitive marcate ca active/online in momentul interogarii
    @SerializedName("active_devices") val active_devices: Int,
    // Numarul de comenzi trimise in ziua curenta (resetat la miezul noptii)
    @SerializedName("total_commands_today") val total_commands_today: Int,
    // Numele dispozitivului cu cele mai multe comenzi — null daca nu exista comenzi
    @SerializedName("most_used_device") val most_used_device: String?
)

/**
 * Un element din feed-ul de activitate recenta al dashboard-ului.
 * Returnat de GET /dashboard/activity?limit={n}.
 * Fiecare element reprezinta o comanda trimisa, cu informatii despre dispozitivul vizat.
 */
data class ActivityResponse(
    val id: Int,
    // Numele dispozitivului (join pe server intre comenzi si dispozitive)
    @SerializedName("device_name") val device_name: String,
    @SerializedName("command_type") val command_type: String,
    // Momentul executiei comenzii — afisat relativ in UI (ex: "acum 5 minute")
    @SerializedName("created_at") val created_at: String
)
