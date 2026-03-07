package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.SceneResponse
import com.denis.smarthome.data.repository.SceneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScenesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SceneRepository(RetrofitClient.apiService)

    private val _scenes = MutableStateFlow<List<SceneResponse>>(emptyList())
    val scenes: StateFlow<List<SceneResponse>> = _scenes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _executeMessage = MutableStateFlow<String?>(null)
    val executeMessage: StateFlow<String?> = _executeMessage.asStateFlow()

    init {
        loadScenes()
    }

    fun loadScenes() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getScenes().onSuccess { _scenes.value = it }.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun executeScene(id: Int) {
        viewModelScope.launch {
            repository.executeScene(id)
                .onSuccess { _executeMessage.value = it["message"] ?: "Scene executed" }
                .onFailure { _error.value = it.message }
        }
    }

    fun deleteScene(id: Int) {
        viewModelScope.launch {
            repository.deleteScene(id).onSuccess { loadScenes() }
        }
    }

    fun clearMessage() { _executeMessage.value = null }
    fun clearError() { _error.value = null }
}
