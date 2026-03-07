package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.local.TokenManager
import com.denis.smarthome.data.model.DashboardStats
import com.denis.smarthome.data.model.UserResponse
import com.denis.smarthome.data.repository.AuthRepository
import com.denis.smarthome.data.repository.DashboardRepository
import com.denis.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class RoomInfo(
    val name: String,
    val deviceCount: Int,
    val activeCount: Int
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val dashboardRepo = DashboardRepository(RetrofitClient.apiService)
    private val deviceRepo = DeviceRepository(RetrofitClient.apiService)
    private val authRepo = AuthRepository(RetrofitClient.apiService, tokenManager)

    private val _stats = MutableStateFlow<DashboardStats?>(null)
    val stats: StateFlow<DashboardStats?> = _stats.asStateFlow()

    private val _rooms = MutableStateFlow<List<RoomInfo>>(emptyList())
    val rooms: StateFlow<List<RoomInfo>> = _rooms.asStateFlow()

    private val _user = MutableStateFlow<UserResponse?>(null)
    val user: StateFlow<UserResponse?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val greeting: String
        get() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> "Bună dimineața"
                in 12..17 -> "Bună ziua"
                else -> "Bună seara"
            }
        }

    val currentDate: String
        get() {
            val format = SimpleDateFormat("EEEE, d MMMM", Locale("ro", "RO"))
            return format.format(Date()).replaceFirstChar { it.uppercase() }
        }

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            authRepo.getMe().onSuccess { _user.value = it }
            dashboardRepo.getStats().onSuccess { _stats.value = it }
                .onFailure { _error.value = it.message }

            // Build RoomInfo from devices list
            deviceRepo.getDevices().onSuccess { devices ->
                _rooms.value = devices
                    .groupBy { it.room }
                    .map { (room, devs) ->
                        RoomInfo(
                            name = room,
                            deviceCount = devs.size,
                            activeCount = devs.count { it.is_active }
                        )
                    }
            }

            _isLoading.value = false
        }
    }
}
