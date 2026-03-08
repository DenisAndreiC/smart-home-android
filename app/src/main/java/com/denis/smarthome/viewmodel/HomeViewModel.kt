/**
 * HomeViewModel.kt - ViewModel pentru ecranul principal (dashboard)
 *
 * Incarca statisticile generale ale sistemului, informatiile utilizatorului
 * si lista de camere derivata din dispozitivele active.
 * Calculeaza salutul si data curenta dinamic, in functie de ora si locale.
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
 * @param activeCount numarul de dispozitive active (is_active == true) din camera
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

    /**
     * Salut dinamic calculat in functie de ora curenta din [Calendar].
     *
     * 5-11 → dimineata, 12-17 → zi, altfel → seara.
     * Recalculat la fiecare acces (proprietate fara backing field).
     */
    val greeting: String
        get() {
            // Citim ora curenta a zilei din Calendar pentru a alege salutul potrivit
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> "Buna dimineata"
                in 12..17 -> "Buna ziua"
                else -> "Buna seara"
            }
        }

    /**
     * Data curenta formatata in romana (ex: "Luni, 3 Martie").
     *
     * Foloseste [SimpleDateFormat] cu Locale("ro", "RO") pentru numele zilei
     * si lunii in limba romana. Prima litera este transformata in majuscula.
     */
    val currentDate: String
        get() {
            // Formatul "EEEE, d MMMM" produce ex: "luni, 3 martie" -> capitalizam prima litera
            val format = SimpleDateFormat("EEEE, d MMMM", Locale("ro", "RO"))
            return format.format(Date()).replaceFirstChar { it.uppercase() }
        }

    init {
        // Incarcam datele dashboard-ului la initializarea ViewModel-ului
        loadDashboard()
    }

    /**
     * Incarca toate datele necesare dashboard-ului intr-un singur bloc.
     *
     * Apeleaza in paralel: getMe() pentru profil, getStats() pentru statistici
     * si getDevices() pentru constructia listei de camere prin groupBy.
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            authRepo.getMe().onSuccess { _user.value = it }
            dashboardRepo.getStats().onSuccess { _stats.value = it }
                .onFailure { _error.value = it.message }

            // Construim lista RoomInfo din dispozitive: groupBy room → map la RoomInfo
            // Nu exista endpoint /rooms, deci derivam camerele din lista de dispozitive
            deviceRepo.getDevices().onSuccess { devices ->
                _rooms.value = devices
                    .groupBy { it.room }
                    .map { (room, devs) ->
                        RoomInfo(
                            name = room,
                            deviceCount = devs.size,
                            // Numaram doar dispozitivele cu is_active == true
                            activeCount = devs.count { it.is_active }
                        )
                    }
            }

            _isLoading.value = false
        }
    }
}
