package com.denis.smarthome.data.api

import com.denis.smarthome.data.model.*
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): TokenResponse

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @GET("auth/me")
    suspend fun getMe(): UserResponse

    // ── Devices ───────────────────────────────────────────────────────────────

    @GET("devices")
    suspend fun getDevices(): List<DeviceResponse>

    @POST("devices")
    suspend fun createDevice(@Body body: DeviceRequest): DeviceResponse

    @PUT("devices/{id}")
    suspend fun updateDevice(@Path("id") id: Int, @Body body: DeviceRequest): DeviceResponse

    @DELETE("devices/{id}")
    suspend fun deleteDevice(@Path("id") id: Int)

    @GET("devices/rooms")
    suspend fun getRooms(): List<String>

    // ── Commands ──────────────────────────────────────────────────────────────

    @POST("commands/send")
    suspend fun sendCommand(@Body body: CommandRequest): CommandResponse

    @GET("commands/history")
    suspend fun getCommandHistory(@Query("device_id") deviceId: Int): List<CommandHistoryResponse>

    // ── Scenes ────────────────────────────────────────────────────────────────

    @GET("scenes")
    suspend fun getScenes(): List<SceneResponse>

    @POST("scenes")
    suspend fun createScene(@Body body: SceneRequest): SceneResponse

    @POST("scenes/{id}/execute")
    suspend fun executeScene(@Path("id") id: Int): Map<String, String>

    @DELETE("scenes/{id}")
    suspend fun deleteScene(@Path("id") id: Int)

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GET("dashboard/stats")
    suspend fun getDashboardStats(): DashboardStats

    @GET("dashboard/activity")
    suspend fun getDashboardActivity(@Query("limit") limit: Int = 10): List<ActivityResponse>
}
