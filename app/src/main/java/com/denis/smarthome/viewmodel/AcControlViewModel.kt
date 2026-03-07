package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.CommandRequest
import com.denis.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AcMode { COOL, HEAT, FAN, DRY, AUTO }
enum class FanSpeed { LOW, MED, HIGH, AUTO }

class AcControlViewModel(
    application: Application,
    private val deviceId: Int
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    private val _temperature = MutableStateFlow(22)
    val temperature: StateFlow<Int> = _temperature.asStateFlow()

    private val _mode = MutableStateFlow(AcMode.COOL)
    val mode: StateFlow<AcMode> = _mode.asStateFlow()

    private val _fanSpeed = MutableStateFlow(FanSpeed.AUTO)
    val fanSpeed: StateFlow<FanSpeed> = _fanSpeed.asStateFlow()

    private val _swingEnabled = MutableStateFlow(false)
    val swingEnabled: StateFlow<Boolean> = _swingEnabled.asStateFlow()

    private val _timerHours = MutableStateFlow(0)
    val timerHours: StateFlow<Int> = _timerHours.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun togglePower() {
        _isOn.value = !_isOn.value
        sendCommand("power:${if (_isOn.value) "on" else "off"}")
    }

    fun increaseTemperature() {
        if (_temperature.value < 30) {
            _temperature.value++
            sendCommand("temp:${_temperature.value}")
        }
    }

    fun decreaseTemperature() {
        if (_temperature.value > 16) {
            _temperature.value--
            sendCommand("temp:${_temperature.value}")
        }
    }

    fun setMode(acMode: AcMode) {
        _mode.value = acMode
        sendCommand("mode:${acMode.name.lowercase()}")
    }

    fun setFanSpeed(speed: FanSpeed) {
        _fanSpeed.value = speed
        sendCommand("fan:${speed.name.lowercase()}")
    }

    fun toggleSwing() {
        _swingEnabled.value = !_swingEnabled.value
        sendCommand("swing:${if (_swingEnabled.value) "on" else "off"}")
    }

    fun setTimer(hours: Int) {
        _timerHours.value = hours
        if (hours > 0) sendCommand("timer:$hours")
    }

    private fun sendCommand(data: String) {
        viewModelScope.launch {
            repository.sendCommand(
                CommandRequest(
                    device_id = deviceId,
                    command_type = "ir",
                    command_data = data
                )
            ).onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }

    class Factory(private val application: Application, private val deviceId: Int) :
        ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AcControlViewModel(application, deviceId) as T
        }
    }
}
