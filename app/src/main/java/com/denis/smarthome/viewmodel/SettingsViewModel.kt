/**
 * SettingsViewModel.kt - ViewModel pentru ecranul de setari si profil utilizator
 *
 * Incarca informatiile profilului utilizatorului prin GET /auth/me si seteaza
 * starea conexiunii cu serverul. Gestioneaza preferintele locale (tema, notificari)
 * si logout-ul care sterge token-ul JWT din DataStore.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.local.TokenManager
import com.denis.smarthome.data.model.MLSettingsRequest
import com.denis.smarthome.data.repository.AuthRepository
import com.denis.smarthome.ui.theme.ThemeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel pentru SettingsScreen.
 *
 * Responsabilitati principale:
 * 1. Incarcarea profilului utilizatorului autentificat (GET /auth/me)
 * 2. Detectarea starii conexiunii cu serverul FastAPI
 * 3. Gestionarea preferintelor locale: tema intunecata si notificari
 * 4. Logout: sterge token-ul din DataStore si redirectioneaza la Login
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val authRepo = AuthRepository(RetrofitClient.apiService, tokenManager)

    // Numele utilizatorului autentificat, incarcat din /auth/me
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // User email
    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    // Avatar URL returned by the server after a successful upload
    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    // True while an avatar upload is in progress
    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

    /**
     * Starea conexiunii cu serverul FastAPI.
     *
     * Devine true daca GET /auth/me returneaza succes, false daca cererea esueaza.
     * Afisata in UI ca indicator "Server Connected" / "Server Offline".
     */
    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    // Preferinta pentru tema intunecata — sincronizata cu ThemeState si DataStore
    private val _darkTheme = MutableStateFlow(ThemeState.isDark)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    // Preferinta pentru notificari push, implicita true
    private val _notifications = MutableStateFlow(true)
    val notifications: StateFlow<Boolean> = _notifications.asStateFlow()

    /**
     * Ora ultimei sincronizari cu serverul, formatata ca "HH:mm".
     *
     * Actualizata la fiecare apel reusit [loadUserInfo].
     * Valoarea initiala "--:--" indica ca nu s-a facut nicio sincronizare.
     * [SimpleDateFormat] cu Locale.getDefault() afiseaza ora in formatul local.
     */
    private val _lastSyncTime = MutableStateFlow("--:--")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    // Email verification status loaded from /auth/me
    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

    // Minimum occurrences for ML pattern detection (3-20, default 5)
    private val _mlMinOccurrences = MutableStateFlow(5)
    val mlMinOccurrences: StateFlow<Int> = _mlMinOccurrences.asStateFlow()

    // Minimum distinct days for ML pattern detection (2-7, default 4)
    private val _mlMinDays = MutableStateFlow(4)
    val mlMinDays: StateFlow<Int> = _mlMinDays.asStateFlow()

    // True after a resend-verification email is successfully sent
    private val _verificationSent = MutableStateFlow(false)
    val verificationSent: StateFlow<Boolean> = _verificationSent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUserInfo()
        loadMLSettings()
        // Sync initial theme state with persisted DataStore value
        viewModelScope.launch {
            val saved = tokenManager.getTheme().firstOrNull() ?: true
            ThemeState.isDark = saved
            _darkTheme.value = saved
        }
    }

    /**
     * Incarca informatiile profilului utilizatorului din API si actualizeaza starea conexiunii.
     *
     * Apeleaza GET /auth/me cu token-ul curent din DataStore (adaugat automat de RetrofitClient).
     * Daca cererea reuseste: actualizeaza userName, userEmail, seteaza isServerConnected = true
     * si inregistreaza ora sincronizarii cu SimpleDateFormat("HH:mm").
     * Daca cererea esueaza: seteaza isServerConnected = false (server offline sau token expirat).
     */
    fun loadUserInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepo.getMe()
                .onSuccess {
                    _userName.value = it.display_name?.takeIf { n -> n.isNotBlank() } ?: it.username
                    _userEmail.value = it.email
                    _avatarUrl.value = it.avatar_url?.takeIf { u -> u.isNotBlank() }
                    _isVerified.value = it.is_verified
                    // Server connection confirmed — request succeeded
                    _isServerConnected.value = true
                    // Inregistram ora sincronizarii in format HH:mm (ex: "14:35")
                    _lastSyncTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                }
                .onFailure {
                    // Cererea a esuat: serverul e offline sau token-ul este invalid/expirat
                    _isServerConnected.value = false
                }
            _isLoading.value = false
        }
    }

    /**
     * Comuta intre tema intunecata si tema luminoasa.
     * Preferinta este gestionata local (nu e persistata in DataStore in aceasta versiune).
     */
    fun toggleDarkTheme() {
        val newValue = !_darkTheme.value
        _darkTheme.value = newValue
        ThemeState.isDark = newValue
        viewModelScope.launch { tokenManager.saveTheme(newValue) }
    }

    /**
     * Comuta activarea/dezactivarea notificarilor push.
     * Preferinta este gestionata local.
     */
    fun toggleNotifications() { _notifications.value = !_notifications.value }

    /**
     * Efectueaza logout-ul utilizatorului.
     *
     * Sterge token-ul JWT din DataStore prin [TokenManager.clearToken].
     * Dupa stergerea token-ului, NavGraph detecteaza absenta token-ului
     * si redirectioneaza automat la ecranul de Login.
     */
    fun logout() {
        viewModelScope.launch { authRepo.logout() }
    }

    /**
     * Sends PUT /api/users/me with the new display_name, then re-fetches the profile.
     */
    fun updateUsername(newDisplayName: String) {
        viewModelScope.launch {
            authRepo.updateUser(newDisplayName)
                .onSuccess {
                    _userName.value = it.display_name?.takeIf { n -> n.isNotBlank() } ?: it.username
                    loadUserInfo()
                }
        }
    }

    /**
     * Loads ML pattern detection settings from GET /ml/settings.
     */
    fun loadMLSettings() {
        viewModelScope.launch {
            runCatching { RetrofitClient.apiService.getMLSettings() }
                .onSuccess {
                    _mlMinOccurrences.value = it.min_occurrences
                    _mlMinDays.value = it.min_days
                }
        }
    }

    /**
     * Updates the minimum pattern occurrences threshold via POST /ml/settings.
     */
    fun updateMLMinOccurrences(value: Int) {
        _mlMinOccurrences.value = value
        viewModelScope.launch {
            runCatching {
                RetrofitClient.apiService.updateMLSettings(
                    MLSettingsRequest(min_occurrences = value, min_days = _mlMinDays.value)
                )
            }
        }
    }

    /**
     * Updates the minimum distinct days threshold via POST /ml/settings.
     */
    fun updateMLMinDays(days: Int) {
        _mlMinDays.value = days
        viewModelScope.launch {
            runCatching {
                RetrofitClient.apiService.updateMLSettings(
                    MLSettingsRequest(min_occurrences = _mlMinOccurrences.value, min_days = days)
                )
            }
        }
    }

    /**
     * Sends POST /auth/resend-verification and sets verificationSent on success.
     */
    fun resendVerification() {
        viewModelScope.launch {
            runCatching { RetrofitClient.apiService.resendVerification() }
                .onSuccess { _verificationSent.value = true }
        }
    }

    /**
     * Picks the image at [uri] from the content provider, converts it to a multipart body,
     * and uploads it via POST /api/users/me/avatar.
     * On success the returned avatar_url is stored in [avatarUrl].
     */
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            runCatching {
                val cr = getApplication<Application>().contentResolver
                val mimeType = cr.getType(uri) ?: "image/jpeg"
                val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                val extension = if (mimeType.contains("png")) "png" else "jpg"
                val part = MultipartBody.Part.createFormData("avatar", "avatar.$extension", requestBody)
                authRepo.uploadAvatar(part)
                    .onSuccess { user ->
                        _avatarUrl.value = user.avatar_url?.takeIf { it.isNotBlank() }
                        user.display_name?.takeIf { it.isNotBlank() }?.let { _userName.value = it }
                    }
            }
            _isUploadingAvatar.value = false
        }
    }
}
