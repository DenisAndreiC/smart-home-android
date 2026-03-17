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
    @SerializedName("display_name") val display_name: String?,
    @SerializedName("avatar_url")   val avatar_url: String?,
    @SerializedName("created_at")   val created_at: String
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
    @SerializedName("current_password") val current_password: String,
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
