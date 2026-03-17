/**
 * HomeViewModel.kt - ViewModel pentru ecranul principal (dashboard)
 *
 * Incarca statisticile generale ale sistemului, informatiile utilizatorului
 * si lista de camere derivata din dispozitivele active.
 * Calculeaza salutul si data curenta dinamic, in functie de ora si locale.
 * Expune actiunile rapide allOff() si awayMode() cu feedback prin snackbar.
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
import com.denis.smarthome.data.model.DashboardStats
import com.denis.smarthome.data.model.UserResponse
import com.denis.smarthome.data.repository.AuthRepository
import com.denis.smarthome.data.repository.DashboardRepository
import com.denis.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Model de date pentru o camera din casa.
 *
 * Construita prin gruparea dispozitivelor dupa campul [room] din API.
 * Nu exista un endpoint dedicat pentru camere — lista se deduce din dispozitive.
 *
 * @param name numele camerei (ex: "Living", "Bedroom")
 * @param deviceCount numarul total de dispozitive din camera
 * @param activeCount numarul de dispozitive active din camera
 */
data class RoomInfo(
    val name: String,
    val deviceCount: Int,
    val activeCount: Int
)

/**
 * ViewModel pentru HomeScreen.
 *
 * Agrega date din trei repository-uri: dashboard stats, lista de dispozitive si profil user.
 * Lista de camere ([rooms]) nu vine direct din API, ci este construita prin groupBy pe lista
 * de dispozitive, extragand camerele unice si numarand dispozitivele active per camera.
 *
 * Consumul energetic ([energyKwh]) este estimat local pe baza tipului fiecarui dispozitiv
 * activ si un numar de ore de utilizare/zi: TV=0.1kW, AC=1.5kW, RGB=0.01kW, relay=0.05kW.
 *
 * Numarul de notificari necitite ([unreadNotificationCount]) este incarcat la fiecare
 * refresh si afisat pe badge-ul clopotelului din header.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val dashboardRepo = DashboardRepository(RetrofitClient.apiService)
    private val deviceRepo = DeviceRepository(RetrofitClient.apiService)
    private val authRepo = AuthRepository(RetrofitClient.apiService, tokenManager)

    private val _stats = MutableStateFlow<DashboardStats?>(null)
    val stats: StateFlow<DashboardStats?> = _stats.asStateFlow()

    private val _rooms = MutableStateFlow<List<RoomInfo>>(emptyList())
    val rooms: StateFlow<List<RoomInfo>> = _rooms.asStateFlow()

    private val _user = MutableStateFlow<UserResponse?>(null)
    val user: StateFlow<UserResponse?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    // Estimated daily energy consumption in kWh based on active devices
    private val _energyKwh = MutableStateFlow(0.0)
    val energyKwh: StateFlow<Double> = _energyKwh.asStateFlow()

    /**
     * Salut dinamic calculat in functie de ora curenta din [Calendar].
     *
     * 5-11 -> morning, 12-17 -> afternoon, altfel -> evening.
     * Recalculat la fiecare acces (proprietate fara backing field).
     */
    val greeting: String
        get() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> "Good morning"
                in 12..17 -> "Good afternoon"
                else -> "Good evening"
            }
        }

    /**
     * Data curenta formatata in engleza (ex: "Monday, March 17").
     *
     * Foloseste [SimpleDateFormat] cu [Locale.ENGLISH] pentru consistenta
     * cu restul interfetei in engleza.
     */
    val currentDate: String
        get() {
            val format = SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH)
            return format.format(Date())
        }

    init {
        // Incarcam datele dashboard-ului la initializarea ViewModel-ului
        loadDashboard()
    }

    /**
     * Incarca toate datele necesare dashboard-ului intr-un singur bloc.
     *
     * Apeleaza in secventa: getMe() pentru profil, getStats() pentru statistici,
     * getDevices() pentru camere si calcul energie, getNotifications() pentru badge.
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            authRepo.getMe().onSuccess { _user.value = it }
            dashboardRepo.getStats().onSuccess { _stats.value = it }
                .onFailure { _error.value = it.message }

            deviceRepo.getDevices().onSuccess { devices ->
                // Construim lista RoomInfo din dispozitive: groupBy room -> map la RoomInfo
                _rooms.value = devices
                    .groupBy { it.room }
                    .map { (room, devs) ->
                        RoomInfo(
                            name = room ?: "Unknown",
                            deviceCount = devs.size,
                            activeCount = devs.count { it.is_online }
                        )
                    }

                // Calcul consum estimat zilnic: pentru fiecare dispozitiv activ,
                // putere (kW) * 8 ore/zi. Statusul "activ" vine din last_status cand exista.
                val activeDevices = devices.filter {
                    it.last_status?.lowercase() == "on" || (it.last_status == null && it.is_online)
                }
                val kwhEstimate = activeDevices.sumOf { device ->
                    val typeL = device.device_type.lowercase()
                    val nameL = device.name.lowercase()
                    val powerKw = when {
                        typeL == "ir_ac" || nameL.contains("ac") || nameL.contains("air") -> 1.5
                        typeL == "ir_tv" || nameL.contains("tv") -> 0.1
                        typeL == "ir_rgb" || nameL.contains("rgb") || nameL.contains("bulb") -> 0.01
                        else -> 0.05
                    }
                    powerKw * 8.0 // 8 ore/zi estimat
                }
                _energyKwh.value = kwhEstimate
            }

            // Numarul de notificari necitite pentru badge-ul clopotelului din header
            runCatching { RetrofitClient.apiService.getNotifications() }
                .onSuccess { notifications ->
                    _unreadNotificationCount.value = notifications.count { !it.is_read }
                }

            _isLoading.value = false
        }
    }

    /**
     * Trimite comanda POST /devices/all-off si afiseaza snackbar cu rezultatul.
     *
     * La succes: "All devices turned off". La eroare: mesajul de la API.
     */
    fun allOff() {
        viewModelScope.launch {
            deviceRepo.allOff()
                .onSuccess { _snackbarMessage.value = "All devices turned off" }
                .onFailure { _snackbarMessage.value = "Failed: ${it.message}" }
        }
    }

    /**
     * Trimite comanda POST /devices/away-mode si afiseaza snackbar cu rezultatul.
     *
     * La succes: "Away mode activated". La eroare: mesajul de la API.
     */
    fun awayMode() {
        viewModelScope.launch {
            deviceRepo.awayMode()
                .onSuccess { _snackbarMessage.value = "Away mode activated" }
                .onFailure { _snackbarMessage.value = "Failed: ${it.message}" }
        }
    }

    /** Sterge mesajul de snackbar dupa ce a fost afisat. */
    fun clearSnackbar() { _snackbarMessage.value = null }
}
