/**
 * NotificationsViewModel.kt - ViewModel pentru ecranul de notificari
 *
 * Incarca lista de notificari ale utilizatorului prin GET /api/notifications/.
 * Expune starea de incarcare si erorile catre UI prin StateFlow-uri imutabile.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.NotificationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pentru NotificationsScreen.
 *
 * Apeleaza GET /notifications/ la initializare si expune lista prin [notifications].
 * Notificarile necitite (is_read == false) sunt folosite si de HomeViewModel
 * pentru a calcula numarul afisat pe badge-ul clopotelului din dashboard.
 */
class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val _notifications = MutableStateFlow<List<NotificationResponse>>(emptyList())
    val notifications: StateFlow<List<NotificationResponse>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Incarcam notificarile imediat la crearea ViewModel-ului
        loadNotifications()
    }

    /**
     * Incarca lista de notificari din API.
     *
     * Foloseste runCatching direct pe apiService (fara repository separat)
     * deoarece este singura operatie pe acest endpoint.
     * La succes actualizeaza [_notifications]; la eroare seteaza [_error].
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { RetrofitClient.apiService.getNotifications() }
                .onSuccess { _notifications.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
