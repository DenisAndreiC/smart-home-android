package com.denis.smarthome.data.repository

import com.denis.smarthome.data.api.ApiService
import com.denis.smarthome.data.model.ActivityResponse
import com.denis.smarthome.data.model.DashboardStats

class DashboardRepository(private val apiService: ApiService) {

    suspend fun getStats(): Result<DashboardStats> = runCatching {
        apiService.getDashboardStats()
    }

    suspend fun getActivity(limit: Int = 10): Result<List<ActivityResponse>> = runCatching {
        apiService.getDashboardActivity(limit)
    }
}
