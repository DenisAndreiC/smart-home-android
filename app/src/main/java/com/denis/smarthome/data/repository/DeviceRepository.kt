package com.denis.smarthome.data.repository

import com.denis.smarthome.data.api.ApiService
import com.denis.smarthome.data.model.CommandRequest
import com.denis.smarthome.data.model.CommandResponse
import com.denis.smarthome.data.model.DeviceRequest
import com.denis.smarthome.data.model.DeviceResponse

class DeviceRepository(private val apiService: ApiService) {

    suspend fun getDevices(): Result<List<DeviceResponse>> = runCatching {
        apiService.getDevices()
    }

    suspend fun createDevice(request: DeviceRequest): Result<DeviceResponse> = runCatching {
        apiService.createDevice(request)
    }

    suspend fun deleteDevice(id: Int): Result<Unit> = runCatching {
        apiService.deleteDevice(id)
    }

    suspend fun getRooms(): Result<List<String>> = runCatching {
        apiService.getRooms()
    }

    suspend fun sendCommand(request: CommandRequest): Result<CommandResponse> = runCatching {
        apiService.sendCommand(request)
    }
}
