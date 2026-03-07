package com.denis.smarthome.data.model

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val access_token: String,
    @SerializedName("token_type") val token_type: String
)

data class UserResponse(
    val id: Int,
    val name: String,
    val email: String,
    @SerializedName("created_at") val created_at: String
)

// ── Devices ───────────────────────────────────────────────────────────────────

data class DeviceRequest(
    val name: String,
    @SerializedName("device_type") val device_type: String,
    val room: String,
    @SerializedName("ir_protocol") val ir_protocol: String? = null,
    @SerializedName("mqtt_topic") val mqtt_topic: String
)

data class DeviceResponse(
    val id: Int,
    val name: String,
    @SerializedName("device_type") val device_type: String,
    val room: String,
    @SerializedName("ir_protocol") val ir_protocol: String?,
    @SerializedName("mqtt_topic") val mqtt_topic: String,
    @SerializedName("is_active") val is_active: Boolean,
    @SerializedName("created_at") val created_at: String
)

// ── Commands ──────────────────────────────────────────────────────────────────

data class CommandRequest(
    @SerializedName("device_id") val device_id: Int,
    @SerializedName("command_type") val command_type: String,
    @SerializedName("command_data") val command_data: String? = null
)

data class CommandResponse(
    val message: String,
    val success: Boolean = true
)

data class CommandHistoryResponse(
    val id: Int,
    @SerializedName("device_id") val device_id: Int,
    @SerializedName("command_type") val command_type: String,
    @SerializedName("command_data") val command_data: String?,
    @SerializedName("created_at") val created_at: String
)

// ── Scenes ────────────────────────────────────────────────────────────────────

data class SceneAction(
    @SerializedName("device_id") val device_id: Int,
    @SerializedName("command_type") val command_type: String,
    @SerializedName("command_data") val command_data: String? = null,
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
    @SerializedName("total_devices") val total_devices: Int,
    @SerializedName("active_devices") val active_devices: Int,
    @SerializedName("total_commands_today") val total_commands_today: Int,
    @SerializedName("most_used_device") val most_used_device: String?
)

data class ActivityResponse(
    val id: Int,
    @SerializedName("device_name") val device_name: String,
    @SerializedName("command_type") val command_type: String,
    @SerializedName("created_at") val created_at: String
)
