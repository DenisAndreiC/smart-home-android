/**
 * AcControlViewModel.kt - ViewModel pentru controlul aparatului de aer conditionat
 *
 * Gestioneaza starea AC-ului: pornit/oprit, temperatura (16-30 grade), modul de functionare,
 * viteza ventilatorului, oscilatie si timer. Toate comenzile sunt trimise prin IR.
 * Functia privata sendCommand() este punctul central prin care trec toate operatiile.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
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
import android.util.Log
import kotlinx.coroutines.launch

/**
 * Modurile de functionare ale aparatului de aer conditionat.
 */
enum class AcMode { COOL, HEAT, FAN, DRY, AUTO }

/**
 * Vitezele disponibile ale ventilatorului intern al AC-ului.
 */
enum class FanSpeed { LOW, MED, HIGH, AUTO }

/**
 * ViewModel pentru ecranul de control al aparatului de aer conditionat.
 *
 * @param application contextul aplicatiei
 * @param deviceId ID-ul dispozitivului IR asociat AC-ului
 */
class AcControlViewModel(
    application: Application,
    private val deviceId: Int,
    private val initialStatus: String? = null
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

    init {
        initialStatus?.let { status ->
            _isOn.value = (status.lowercase() != "off")
            status.toIntOrNull()?.let { t ->
                if (t in 16..30) _temperature.value = t
            }
            runCatching { AcMode.valueOf(status.uppercase()) }.onSuccess { _mode.value = it }
            runCatching { FanSpeed.valueOf(status.uppercase()) }.onSuccess { _fanSpeed.value = it }
        }
    }

    fun togglePower() {
        _isOn.value = !_isOn.value
        sendCommand("power", if (_isOn.value) "on" else "off")
    }

    fun increaseTemperature() {
        if (_temperature.value < 30) {
            _temperature.value++
            sendCommand("temp_up", null)
        }
    }

    fun decreaseTemperature() {
        if (_temperature.value > 16) {
            _temperature.value--
            sendCommand("temp_down", null)
        }
    }

    fun setMode(acMode: AcMode) {
        _mode.value = acMode
        sendCommand("mode", acMode.name.lowercase())
    }

    fun setFanSpeed(speed: FanSpeed) {
        _fanSpeed.value = speed
        sendCommand("fan_speed", speed.name.lowercase())
    }

    fun toggleSwing() {
        _swingEnabled.value = !_swingEnabled.value
        sendCommand("swing", if (_swingEnabled.value) "on" else "off")
    }

    fun setTimer(hours: Int) {
        _timerHours.value = hours
        if (hours > 0) sendCommand("timer", "$hours")
    }

    private fun sendCommand(action: String, value: String?) {
        viewModelScope.launch {
            Log.d("AcControl", "sendCommand: device=$deviceId action=$action value=$value")
            repository.sendCommand(
                CommandRequest(device_id = deviceId, action = action, value = value)
            ).onSuccess {
                Log.d("AcControl", "command OK: ${it.message}")
            }.onFailure {
                Log.e("AcControl", "command FAILED: ${it.message}")
                _error.value = it.message
            }
        }
    }

    fun clearError() { _error.value = null }

    class Factory(
        private val application: Application, 
        private val deviceId: Int,
        private val initialStatus: String? = null
    ) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AcControlViewModel(application, deviceId, initialStatus) as T
        }
    }
}
