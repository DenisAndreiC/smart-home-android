package com.denis.smarthome.data.model

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val access_token: String,
    @SerializedName("token_type")   val token_type: String
)

data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("display_name")  val display_name: String?,
    @SerializedName("avatar_url")    val avatar_url: String?,
    @SerializedName("created_at")    val created_at: String,
    @SerializedName("is_verified")   val is_verified: Boolean = false
)

// ── Rooms ─────────────────────────────────────────────────────────────────────

data class RoomRequest(val name: String)

data class RoomResponse(
    val id: Int,
    val name: String,
    @SerializedName("owner_id")   val owner_id: Int,
    @SerializedName("created_at") val created_at: String
)

// ── Devices ───────────────────────────────────────────────────────────────────

data class DeviceRequest(
    val name: String,
    @SerializedName("device_type")     val device_type: String,
    val room: String? = null,
    @SerializedName("mqtt_topic")      val mqtt_topic: String? = null,
    @SerializedName("ir_codes")        val ir_codes: Map<String, String>? = null,
    @SerializedName("ir_remote_type")  val ir_remote_type: String? = null
)

data class DeviceResponse(
    val id: Int,
    val name: String,
    @SerializedName("device_type")  val device_type: String,
    val room: String?,
    @SerializedName("room_id")      val room_id: Int?,
    @SerializedName("mqtt_topic")   val mqtt_topic: String?,
    @SerializedName("is_online")    val is_online: Boolean,
    @SerializedName("last_status")  val last_status: String?,
    @SerializedName("mac_address")  val mac_address: String?,
    @SerializedName("ir_codes")        val ir_codes: String?,
    @SerializedName("ir_remote_type")  val ir_remote_type: String?,
    @SerializedName("owner_id")        val owner_id: Int,
    @SerializedName("created_at")   val created_at: String
)

// ── Commands ──────────────────────────────────────────────────────────────────

data class CommandRequest(
    @SerializedName("device_id") val device_id: Int,
    val action: String,
    val value: String? = null
)

data class CommandResponse(
    val message: String,
    val success: Boolean = true
)

data class CommandHistoryResponse(
    val id: Int,
    @SerializedName("device_id")   val device_id: Int,
    val action: String,
    val value: String?,
    @SerializedName("created_at")  val created_at: String
)

// ── Scenes ────────────────────────────────────────────────────────────────────

data class SceneAction(
    @SerializedName("device_id")    val device_id: Int,
    @SerializedName("action")       val command_type: String,
    @SerializedName("value")        val command_data: String? = null,
    @SerializedName("delay_seconds") val delay_seconds: Int = 0
)

data class SceneRequest(
    val name: String,
    val icon: String? = null,
    val actions: List<SceneAction>
)

data class SceneResponse(
    val id: Int,
    val name: String,
    val icon: String?,
    val actions: List<SceneAction>,
    @SerializedName("is_active") val is_active: Boolean
)

// ── Dashboard ─────────────────────────────────────────────────────────────────

data class DashboardStats(
    @SerializedName("total_devices")          val total_devices: Int,
    @SerializedName("total_commands_today")   val total_commands_today: Int,
    @SerializedName("total_routines_active")  val total_routines_active: Int,
    @SerializedName("total_scenes")           val total_scenes: Int,
    @SerializedName("most_used_device")       val most_used_device: String?,
    @SerializedName("peak_hour")              val peak_hour: Int?,
    @SerializedName("commands_by_day")        val commands_by_day: List<Map<String, Any>>?,
    @SerializedName("commands_by_device")     val commands_by_device: List<Map<String, Any>>?,
    @SerializedName("device_type_distribution") val device_type_distribution: List<Map<String, Any>>?
)

data class ActivityResponse(
    val id: Int,
    @SerializedName("device_name") val device_name: String,
    val action: String,
    @SerializedName("created_at")  val created_at: String
)

// ── Notifications ──────────────────────────────────────────────────────────────

data class NotificationResponse(
    val id: Int,
    val message: String,
    @SerializedName("is_read")    val is_read: Boolean,
    @SerializedName("created_at") val created_at: String
)

// ── Users ──────────────────────────────────────────────────────────────────────

data class UpdateUserRequest(
    @SerializedName("display_name") val display_name: String
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val current_password: String? = null,
    @SerializedName("email_code")       val email_code: String?       = null,
    @SerializedName("new_password")     val new_password: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class MessageResponse(
    val message: String
)

// ── Stats ──────────────────────────────────────────────────────────────────────

data class EnergyStats(
    @SerializedName("kwh_today") val kwh_today: Double
)

// ── ML ─────────────────────────────────────────────────────────────────────────

data class RecommendationsResponse(
    val recommendations: List<RoutineCandidate>,
    @SerializedName("analyzed_days")   val analyzed_days: Int,
    @SerializedName("total_commands")  val total_commands: Int
)

data class AnomalyItem(
    @SerializedName("device_id")   val device_id: Int,
    @SerializedName("device_name") val device_name: String,
    val action: String,
    val time: String,
    @SerializedName("z_score")     val z_score: Float,
    val message: String
)

data class AnomaliesResponse(
    val anomalies: List<AnomalyItem>,
    @SerializedName("checked_period") val checked_period: String
)

data class MLSettingsRequest(
    @SerializedName("min_occurrences") val min_occurrences: Int,
    @SerializedName("min_days")        val min_days: Int = 4
)

data class MLSettingsResponse(
    @SerializedName("min_occurrences") val min_occurrences: Int,
    @SerializedName("min_days")        val min_days: Int = 4
)

// ── Routines ───────────────────────────────────────────────────────────────────

data class RoutineCreate(
    val name: String,
    @SerializedName("device_id")    val device_id: Int,
    val action: String,
    val value: String? = null,
    @SerializedName("trigger_time") val trigger_time: String,
    @SerializedName("days_of_week") val days_of_week: String
)

data class RoutineToggle(
    @SerializedName("is_active") val is_active: Boolean
)

data class RoutineResponse(
    val id: Int,
    @SerializedName("user_id")         val user_id: Int,
    val name: String,
    @SerializedName("device_id")       val device_id: Int,
    val action: String,
    val value: String?,
    @SerializedName("trigger_time")    val trigger_time: String,
    @SerializedName("days_of_week")    val days_of_week: String,
    @SerializedName("is_active")       val is_active: Boolean,
    @SerializedName("is_ml_suggested") val is_ml_suggested: Boolean,
    val confidence: Float?,
    @SerializedName("created_at")      val created_at: String
)

/**
 * Un candidat de rutina detectat de ML — NU e inca persistat pe backend.
 *
 * Model comun pentru GET /routines/detect si GET /ml/recommendations: backend-ul
 * foloseste aceeasi functie detect_routines() pentru ambele, deci returneaza exact
 * aceleasi campuri. candidate_index e prezent doar in raspunsul /routines/detect
 * (identifica sugestia in cadrul acelui raspuns — nu exista inca un id de baza de
 * date pana cand utilizatorul nu alege sa creeze rutina); lipseste din /ml/recommendations.
 *
 * candidate_index e nullable (nu are default numeric) intentionat: Retrofit foloseste
 * GsonConverterFactory simplu (fara adapter Kotlin-aware), care aloca obiectul prin
 * reflectie si NU trece prin constructor — orice default din Kotlin pentru un camp
 * numeric absent din JSON ar fi ignorat silentios (Gson ar lasa 0, nu valoarea default
 * declarata). Un tip nullable primeste corect `null` de la Gson cand cheia lipseste din
 * JSON, deci codul care citeste candidate_index trebuie sa trateze explicit cazul null
 * (nu presupune niciodata ca e valid/unic pentru elemente venite din /ml/recommendations).
 */
data class RoutineCandidate(
    @SerializedName("device_id")       val device_id: Int,
    @SerializedName("device_name")     val device_name: String = "",
    val action: String,
    val value: String?,
    @SerializedName("trigger_time")    val trigger_time: String,
    @SerializedName("days_of_week")    val days_of_week: String,
    val occurrences: Int = 0,
    @SerializedName("distinct_days")   val distinct_days: Int = 0,
    val confidence: Float,
    val name: String,
    @SerializedName("candidate_index") val candidate_index: Int? = null
)

data class RoutineDetectResponse(
    @SerializedName("routines_detected") val routines_detected: Int,
    @SerializedName("routines_saved")    val routines_saved: Int,
    val data: List<RoutineCandidate>
)
