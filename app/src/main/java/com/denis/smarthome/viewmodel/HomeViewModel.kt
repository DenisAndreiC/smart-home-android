package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.ActivityResponse
import com.denis.smarthome.data.model.DashboardStats
import com.denis.smarthome.data.model.UserResponse
import com.denis.smarthome.data.repository.AuthRepository
import com.denis.smarthome.data.repository.DashboardRepository
import com.denis.smarthome.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val dashboardRepo = DashboardRepository(RetrofitClient.apiService)
    private val authRepo = AuthRepository(RetrofitClient.apiService, tokenManager)

    private val _stats = MutableStateFlow<DashboardStats?>(null)
    val stats: StateFlow<DashboardStats?> = _stats.asStateFlow()

    private val _activity = MutableStateFlow<List<ActivityResponse>>(emptyList())
    val activity: StateFlow<List<ActivityResponse>> = _activity.asStateFlow()

    private val _user = MutableStateFlow<UserResponse?>(null)
    val user: StateFlow<UserResponse?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            authRepo.getMe().onSuccess { _user.value = it }
            dashboardRepo.getStats().onSuccess { _stats.value = it }.onFailure { _error.value = it.message }
            dashboardRepo.getActivity().onSuccess { _activity.value = it }

            _isLoading.value = false
        }
    }
}
