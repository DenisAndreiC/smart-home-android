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

/** All possible UI states for the change-password screen. */
sealed class ChangePasswordUiState {
    object Idle    : ChangePasswordUiState()
    object Loading : ChangePasswordUiState()
    object Success : ChangePasswordUiState()
    data class Error(val message: String) : ChangePasswordUiState()
}

class ChangePasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val authRepo     = AuthRepository(RetrofitClient.apiService, tokenManager)

    private val _uiState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState.Idle)
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    /**
     * Validates the inputs and sends POST /auth/change-password.
     * Sets [uiState] to Success on 200 OK, or Error with a message otherwise.
     */
    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        when {
            currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                _uiState.value = ChangePasswordUiState.Error("Please fill in all fields")
                return
            }
            newPassword != confirmPassword -> {
                _uiState.value = ChangePasswordUiState.Error("New passwords do not match")
                return
            }
            newPassword.length < 6 -> {
                _uiState.value = ChangePasswordUiState.Error("Password must be at least 6 characters")
                return
            }
        }
        viewModelScope.launch {
            _uiState.value = ChangePasswordUiState.Loading
            authRepo.changePassword(currentPassword, newPassword)
                .onSuccess { _uiState.value = ChangePasswordUiState.Success }
                .onFailure { _uiState.value = ChangePasswordUiState.Error(it.message ?: "Failed to change password") }
        }
    }

    /** Resets the state back to Idle (used when navigating away or after success). */
    fun resetState() {
        _uiState.value = ChangePasswordUiState.Idle
    }
}
