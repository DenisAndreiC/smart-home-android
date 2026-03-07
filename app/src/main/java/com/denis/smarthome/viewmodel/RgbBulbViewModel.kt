package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
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

data class ColorPreset(
    val name: String,
    val color: Color,
    val angle: Float
)

val rgbPresets = listOf(
    ColorPreset("Warm",   Color(0xFFFFD700), 50f),
    ColorPreset("Cool",   Color(0xFFE0E8FF), 215f),
    ColorPreset("Red",    Color(0xFFFF0000), 0f),
    ColorPreset("Blue",   Color(0xFF0000FF), 240f),
    ColorPreset("Green",  Color(0xFF00FF00), 120f),
    ColorPreset("Purple", Color(0xFF9C27B0), 290f)
)

class RgbBulbViewModel(
    application: Application,
    private val deviceId: Int
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    private val _device = MutableStateFlow<DeviceResponse?>(null)
    val device: StateFlow<DeviceResponse?> = _device.asStateFlow()

    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    private val _brightness = MutableStateFlow(85)
    val brightness: StateFlow<Int> = _brightness.asStateFlow()

    private val _selectedColor = MutableStateFlow(Color(0xFF00BCD4))
    val selectedColor: StateFlow<Color> = _selectedColor.asStateFlow()

    private val _selectedAngle = MutableStateFlow(185f)
    val selectedAngle: StateFlow<Float> = _selectedAngle.asStateFlow()

    private val _selectedPreset = MutableStateFlow<String?>(null)
    val selectedPreset: StateFlow<String?> = _selectedPreset.asStateFlow()

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
        sendCommand("power", if (_isOn.value) "on" else "off")
    }

    fun setBrightness(value: Int) {
        _brightness.value = value
        sendCommand("set_brightness", "$value")
    }

    fun setColorFromWheel(color: Color, angle: Float) {
        _selectedColor.value = color
        _selectedAngle.value = angle
        _selectedPreset.value = null
        sendColorCommand(color)
    }

    fun selectPreset(preset: ColorPreset) {
        _selectedColor.value = preset.color
        _selectedAngle.value = preset.angle
        _selectedPreset.value = preset.name
        sendColorCommand(preset.color)
    }

    private fun sendColorCommand(color: Color) {
        val hex = "#%02X%02X%02X".format(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        sendCommand("set_color", hex)
    }

    private fun sendCommand(type: String, data: String) {
        viewModelScope.launch {
            repository.sendCommand(
                CommandRequest(device_id = deviceId, command_type = type, command_data = data)
            ).onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }

    class Factory(private val application: Application, private val deviceId: Int) :
        ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return RgbBulbViewModel(application, deviceId) as T
        }
    }
}
