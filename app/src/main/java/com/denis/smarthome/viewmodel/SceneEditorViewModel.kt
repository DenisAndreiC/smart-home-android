package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.data.model.SceneAction
import com.denis.smarthome.data.model.SceneRequest
import com.denis.smarthome.data.repository.DeviceRepository
import com.denis.smarthome.data.repository.SceneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SceneActionForm(
    val deviceId: Int,
    val deviceName: String,
    val commandType: String,
    val commandData: String?,
    val delaySeconds: Int = 0
)

class SceneEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val sceneRepo = SceneRepository(RetrofitClient.apiService)
    private val deviceRepo = DeviceRepository(RetrofitClient.apiService)

    private val _sceneName = MutableStateFlow("")
    val sceneName: StateFlow<String> = _sceneName.asStateFlow()

    private val _selectedIcon = MutableStateFlow("Movie")
    val selectedIcon: StateFlow<String> = _selectedIcon.asStateFlow()

    private val _actions = MutableStateFlow<List<SceneActionForm>>(emptyList())
    val actions: StateFlow<List<SceneActionForm>> = _actions.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<DeviceResponse>>(emptyList())
    val availableDevices: StateFlow<List<DeviceResponse>> = _availableDevices.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadDevices() }

    fun loadDevices() {
        viewModelScope.launch {
            deviceRepo.getDevices()
                .onSuccess { _availableDevices.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun setName(name: String) { _sceneName.value = name }
    fun setIcon(icon: String) { _selectedIcon.value = icon }

    fun addAction(action: SceneActionForm) {
        _actions.value = _actions.value + action
    }

    fun removeAction(index: Int) {
        _actions.value = _actions.value.toMutableList().also { it.removeAt(index) }
    }

    fun saveScene() {
        if (_sceneName.value.isBlank()) { _error.value = "Scene name is required"; return }
        viewModelScope.launch {
            _isLoading.value = true
            sceneRepo.createScene(
                SceneRequest(
                    name = _sceneName.value,
                    icon = _selectedIcon.value,
                    actions = _actions.value.map { form ->
                        SceneAction(
                            device_id = form.deviceId,
                            command_type = form.commandType,
                            command_data = form.commandData,
                            delay_seconds = form.delaySeconds
                        )
                    }
                )
            ).onSuccess {
                _saveSuccess.value = true
            }.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
    fun clearSaveSuccess() { _saveSuccess.value = false }
}
