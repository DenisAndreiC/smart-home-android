package com.denis.smarthome.data.api

import com.denis.smarthome.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): UserResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @GET("auth/me")
    suspend fun getMe(): UserResponse

    // ── Rooms ─────────────────────────────────────────────────────────────────

    @GET("rooms/")
    suspend fun getRooms(): List<RoomResponse>

    @POST("rooms/")
    suspend fun createRoom(@Body body: RoomRequest): RoomResponse

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: Int)

    // ── Devices ───────────────────────────────────────────────────────────────

    @GET("devices/")
    suspend fun getDevices(): List<DeviceResponse>

    @GET("devices/{id}")
    suspend fun getDevice(@Path("id") id: Int): DeviceResponse

    @POST("devices/")
    suspend fun createDevice(@Body body: DeviceRequest): DeviceResponse

    @PUT("devices/{id}")
    suspend fun updateDevice(@Path("id") id: Int, @Body body: DeviceRequest): DeviceResponse

    @DELETE("devices/{id}")
    suspend fun deleteDevice(@Path("id") id: Int)

    @POST("devices/all-off")
    suspend fun allOff(): CommandResponse

    @POST("devices/away-mode")
    suspend fun awayMode(): CommandResponse

    // ── Commands ──────────────────────────────────────────────────────────────

    @POST("commands/send")
    suspend fun sendCommand(@Body body: CommandRequest): CommandResponse

    @GET("commands/history")
    suspend fun getCommandHistory(@Query("device_id") deviceId: Int): List<CommandHistoryResponse>

    @POST("commands/set-brand")
    suspend fun setBrand(@Query("brand") brand: String): CommandResponse

    // ── Scenes ────────────────────────────────────────────────────────────────

    @GET("scenes/")
    suspend fun getScenes(): List<SceneResponse>

    @POST("scenes/")
    suspend fun createScene(@Body body: SceneRequest): SceneResponse

    @POST("scenes/{id}/execute")
    suspend fun executeScene(@Path("id") id: Int): Map<String, String>

    @DELETE("scenes/{id}")
    suspend fun deleteScene(@Path("id") id: Int)

    // ── Routines ──────────────────────────────────────────────────────────────

    @GET("routines/")
    suspend fun getRoutines(): List<RoutineResponse>

    @POST("routines/")
    suspend fun createRoutine(@Body body: RoutineCreate): RoutineResponse

    @GET("routines/detect")
    suspend fun detectRoutines(
        @Query("min_occurrences") minOccurrences: Int? = null,
        @Query("min_distinct_days") minDistinctDays: Int? = null
    ): RoutineDetectResponse

    @PUT("routines/{id}/toggle")
    suspend fun toggleRoutine(@Path("id") id: Int, @Body body: RoutineToggle): RoutineResponse

    @DELETE("routines/{id}")
    suspend fun deleteRoutine(@Path("id") id: Int)

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GET("dashboard/stats")
    suspend fun getDashboardStats(): DashboardStats

    @GET("dashboard/activity")
    suspend fun getDashboardActivity(@Query("limit") limit: Int = 10): List<ActivityResponse>

    // ── Notifications ─────────────────────────────────────────────────────────

    @GET("notifications/")
    suspend fun getNotifications(): List<NotificationResponse>

    // ── Users ─────────────────────────────────────────────────────────────────

    @PUT("users/me")
    suspend fun updateUser(@Body body: UpdateUserRequest): UserResponse

    @Multipart
    @POST("users/me/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): UserResponse

    @POST("auth/request-password-change")
    suspend fun requestPasswordChangeCode(): MessageResponse

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): MessageResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GET("stats/energy")
    suspend fun getEnergyStats(): EnergyStats

    // ── ML ────────────────────────────────────────────────────────────────────

    @GET("ml/recommendations")
    suspend fun getRecommendations(
        @Query("min_occurrences") minOccurrences: Int? = null,
        @Query("min_distinct_days") minDistinctDays: Int? = null
    ): RecommendationsResponse

    @GET("ml/anomalies")
    suspend fun getAnomalies(): AnomaliesResponse

    @GET("ml/settings")
    suspend fun getMLSettings(): MLSettingsResponse

    @POST("ml/settings")
    suspend fun updateMLSettings(@Body request: MLSettingsRequest): MLSettingsResponse

    @POST("auth/resend-verification")
    suspend fun resendVerification(): MessageResponse
}
