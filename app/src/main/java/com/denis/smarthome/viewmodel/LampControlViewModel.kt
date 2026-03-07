package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.CommandRequest
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Schedule(
    val name: String,
    val time: String,
    val repeat: String,
    val isOn: Boolean,
    val icon: String  // "sunset" | "night"
)

class LampControlViewModel(
    application: Application,
    private val deviceId: Int
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    private val _device = MutableStateFlow<DeviceResponse?>(null)
    val device: StateFlow<DeviceResponse?> = _device.asStateFlow()

    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    val powerWatts: Double = 12.5
    val usageHours: Double = 4.2

    private val _schedules = MutableStateFlow(
        listOf(
            Schedule("Sunset On",  "6:45 PM",  "Daily", true,  "sunset"),
            Schedule("Night Off",  "11:30 PM", "Daily", false, "night")
        )
    )
    val schedules: StateFlow<List<Schedule>> = _schedules.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadDevice() }

    fun loadDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getDevices()
                .onSuccess { devices ->
                    _device.value = devices.find { it.id == deviceId }
                    _isOn.value = _device.value?.is_active ?: false
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun togglePower() {
        _isOn.value = !_isOn.value
        viewModelScope.launch {
            repository.sendCommand(
                CommandRequest(
                    device_id = deviceId,
                    command_type = "power",
                    command_data = if (_isOn.value) "on" else "off"
                )
            ).onFailure {
                _isOn.value = !_isOn.value
                _error.value = it.message
            }
        }
    }

    fun toggleSchedule(index: Int) {
        _schedules.value = _schedules.value.mapIndexed { i, s ->
            if (i == index) s.copy(isOn = !s.isOn) else s
        }
    }

    fun addSchedule(schedule: Schedule) {
        _schedules.value = _schedules.value + schedule
    }

    fun clearError() { _error.value = null }

    class Factory(private val application: Application, private val deviceId: Int) :
        ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LampControlViewModel(application, deviceId) as T
        }
    }
}
