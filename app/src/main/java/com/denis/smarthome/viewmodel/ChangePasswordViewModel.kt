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

/** Which verification method the user has chosen on the Change Password screen. */
enum class VerificationMethod { PASSWORD, EMAIL_CODE }

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

    /** Whether the "code sent" confirmation banner is visible. */
    private val _codeSent = MutableStateFlow(false)
    val codeSent: StateFlow<Boolean> = _codeSent.asStateFlow()

    /** Which method the user has selected: current password or email OTP. */
    private val _method = MutableStateFlow(VerificationMethod.PASSWORD)
    val method: StateFlow<VerificationMethod> = _method.asStateFlow()

    fun setMethod(method: VerificationMethod) {
        _method.value  = method
        _codeSent.value = false
        // Reset any stale error when the user switches methods
        if (_uiState.value is ChangePasswordUiState.Error) {
            _uiState.value = ChangePasswordUiState.Idle
        }
    }

    /**
     * Calls POST /auth/request-password-change to send a 6-digit OTP to the user's email.
     * Sets [codeSent] to true on success so the UI can show the confirmation banner.
     */
    fun requestCode() {
        viewModelScope.launch {
            _uiState.value = ChangePasswordUiState.Loading
            authRepo.requestPasswordChangeCode()
                .onSuccess {
                    _codeSent.value = true
                    _uiState.value  = ChangePasswordUiState.Idle
                }
                .onFailure {
                    _uiState.value = ChangePasswordUiState.Error(
                        it.message ?: "Failed to send verification code"
                    )
                }
        }
    }

    /**
     * Validates inputs and sends POST /auth/change-password with the appropriate
     * verification field based on the currently selected [VerificationMethod].
     */
    fun changePassword(
        currentPassword: String,
        emailCode: String,
        newPassword: String,
        confirmPassword: String
    ) {
        // Validate common fields
        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = ChangePasswordUiState.Error("Please fill in all fields")
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.value = ChangePasswordUiState.Error("New passwords do not match")
            return
        }
        if (newPassword.length < 6) {
            _uiState.value = ChangePasswordUiState.Error("Password must be at least 6 characters")
            return
        }

        // Validate the chosen verification field
        when (_method.value) {
            VerificationMethod.PASSWORD -> {
                if (currentPassword.isBlank()) {
                    _uiState.value = ChangePasswordUiState.Error("Please enter your current password")
                    return
                }
            }
            VerificationMethod.EMAIL_CODE -> {
                if (emailCode.isBlank()) {
                    _uiState.value = ChangePasswordUiState.Error("Please enter the verification code")
                    return
                }
                if (emailCode.length != 6) {
                    _uiState.value = ChangePasswordUiState.Error("Code must be exactly 6 digits")
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.value = ChangePasswordUiState.Loading
            val result = when (_method.value) {
                VerificationMethod.PASSWORD ->
                    authRepo.changePassword(currentPassword = currentPassword, newPassword = newPassword)
                VerificationMethod.EMAIL_CODE ->
                    authRepo.changePassword(emailCode = emailCode, newPassword = newPassword)
            }
            result
                .onSuccess { _uiState.value = ChangePasswordUiState.Success }
                .onFailure { _uiState.value = ChangePasswordUiState.Error(it.message ?: "Failed to change password") }
        }
    }

    /** Resets the state back to Idle (used when navigating away or after success). */
    fun resetState() {
        _uiState.value  = ChangePasswordUiState.Idle
        _codeSent.value = false
    }
}
