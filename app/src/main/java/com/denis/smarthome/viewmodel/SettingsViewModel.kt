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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.local.TokenManager
import com.denis.smarthome.data.repository.AuthRepository
import com.denis.smarthome.ui.theme.ThemeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
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

    // Email-ul utilizatorului autentificat
    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUserInfo()
        // Sincronizeaza starea initiala cu valoarea persistata in DataStore
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
                    _userName.value = it.username
                    _userEmail.value = it.email
                    // Conexiunea cu serverul este confirmata - cererea a reusit
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

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            authRepo.updateUser(newUsername)
                .onSuccess { _userName.value = it.username }
        }
    }
}
