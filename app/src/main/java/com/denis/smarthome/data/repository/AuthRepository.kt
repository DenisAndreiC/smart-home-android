package com.denis.smarthome.data.repository

import com.denis.smarthome.data.api.ApiService
import com.denis.smarthome.data.local.TokenManager
import com.denis.smarthome.data.model.RegisterRequest
import com.denis.smarthome.data.model.UserResponse

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    suspend fun login(email: String, password: String): Result<String> = runCatching {
        val response = apiService.login(username = email, password = password)
        tokenManager.saveToken(response.access_token)
        response.access_token
    }

    suspend fun register(name: String, email: String, password: String): Result<String> = runCatching {
        val response = apiService.register(RegisterRequest(name, email, password))
        tokenManager.saveToken(response.access_token)
        response.access_token
    }

    suspend fun getMe(): Result<UserResponse> = runCatching {
        apiService.getMe()
    }

    suspend fun logout() {
        tokenManager.clearToken()
    }
}
