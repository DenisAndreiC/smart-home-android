package com.denis.smarthome.data.repository

import com.denis.smarthome.data.api.ApiService
import com.denis.smarthome.data.model.SceneRequest
import com.denis.smarthome.data.model.SceneResponse

class SceneRepository(private val apiService: ApiService) {

    suspend fun getScenes(): Result<List<SceneResponse>> = runCatching {
        apiService.getScenes()
    }

    suspend fun createScene(request: SceneRequest): Result<SceneResponse> = runCatching {
        apiService.createScene(request)
    }

    suspend fun executeScene(id: Int): Result<Map<String, String>> = runCatching {
        apiService.executeScene(id)
    }

    suspend fun deleteScene(id: Int): Result<Unit> = runCatching {
        apiService.deleteScene(id)
    }
}
