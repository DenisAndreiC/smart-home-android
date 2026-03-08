package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.local.TokenManager
import com.denis.smarthome.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val authRepo = AuthRepository(RetrofitClient.apiService, tokenManager)

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _notifications = MutableStateFlow(true)
    val notifications: StateFlow<Boolean> = _notifications.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("--:--")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadUserInfo() }

    fun loadUserInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepo.getMe()
                .onSuccess {
                    _userName.value = it.name
                    _userEmail.value = it.email
                    _isServerConnected.value = true
                    _lastSyncTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                }
                .onFailure { _isServerConnected.value = false }
            _isLoading.value = false
        }
    }

    fun toggleDarkTheme() { _darkTheme.value = !_darkTheme.value }
    fun toggleNotifications() { _notifications.value = !_notifications.value }

    fun logout() {
        viewModelScope.launch { authRepo.logout() }
    }
}
