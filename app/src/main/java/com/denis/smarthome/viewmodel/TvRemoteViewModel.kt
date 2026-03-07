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

class TvRemoteViewModel(
    application: Application,
    private val deviceId: Int
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _commandSent = MutableStateFlow<String?>(null)
    val commandSent: StateFlow<String?> = _commandSent.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun sendCommand(command: String) {
        viewModelScope.launch {
            when (command) {
                "power" -> _isOn.value = !_isOn.value
                "mute" -> _isMuted.value = !_isMuted.value
            }
            _commandSent.value = command
            repository.sendCommand(
                CommandRequest(
                    device_id = deviceId,
                    command_type = "ir",
                    command_data = command
                )
            ).onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }
    fun clearCommand() { _commandSent.value = null }

    class Factory(private val application: Application, private val deviceId: Int) :
        ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TvRemoteViewModel(application, deviceId) as T
        }
    }
}
