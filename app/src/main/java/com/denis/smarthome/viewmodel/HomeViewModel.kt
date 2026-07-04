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
import com.denis.smarthome.data.model.AnomalyItem
import com.denis.smarthome.data.model.DashboardStats
import com.denis.smarthome.data.model.RoutineRecommendation
import com.denis.smarthome.data.model.SceneResponse
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

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    // User scenes loaded from GET /api/scenes/ — shown as Quick Action chips in dashboard
    private val _scenes = MutableStateFlow<List<SceneResponse>>(emptyList())
    val scenes: StateFlow<List<SceneResponse>> = _scenes.asStateFlow()

    // ID of scene currently executing — used for chip loading state
    private val _executingSceneId = MutableStateFlow<Int?>(null)
    val executingSceneId: StateFlow<Int?> = _executingSceneId.asStateFlow()

    // ML routine recommendations from GET /ml/recommendations
    private val _recommendations = MutableStateFlow<List<RoutineRecommendation>>(emptyList())
    val recommendations: StateFlow<List<RoutineRecommendation>> = _recommendations.asStateFlow()

    // Dismissed recommendation device_ids+actions stored locally so they don't reappear
    private val _dismissedKeys = MutableStateFlow<Set<String>>(emptySet())

    // ML anomalies from GET /ml/anomalies — shown as a warning banner
    private val _anomalies = MutableStateFlow<List<AnomalyItem>>(emptyList())
    val anomalies: StateFlow<List<AnomalyItem>> = _anomalies.asStateFlow()

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
            }

            // Numarul de notificari necitite pentru badge-ul clopotelului din header
            runCatching { RetrofitClient.apiService.getNotifications() }
                .onSuccess { notifications ->
                    _unreadNotificationCount.value = notifications.count { !it.is_read }
                }

            // Load user scenes for Quick Actions chips
            runCatching { RetrofitClient.apiService.getScenes() }
                .onSuccess { _scenes.value = it }

            // Load ML routine recommendations; filter out already-dismissed ones
            runCatching { RetrofitClient.apiService.getRecommendations() }
                .onSuccess { response ->
                    val dismissed = _dismissedKeys.value
                    _recommendations.value = response.recommendations
                        .filter { r -> "${r.device_id}:${r.action}" !in dismissed }
                        .take(3)
                }

            // Load ML anomalies for the warning banner
            runCatching { RetrofitClient.apiService.getAnomalies() }
                .onSuccess { _anomalies.value = it.anomalies }

            _isLoading.value = false
        }
    }

    /**
     * Removes a recommendation from the visible list by adding its key to the dismissed set.
     * Dismissed recommendations survive a dashboard refresh within the same session.
     */
    fun dismissRecommendation(recommendation: RoutineRecommendation) {
        val key = "${recommendation.device_id}:${recommendation.action}"
        _dismissedKeys.value = _dismissedKeys.value + key
        _recommendations.value = _recommendations.value.filter { r ->
            "${r.device_id}:${r.action}" != key
        }
    }

    /**
     * Creates a Routine from a dashboard ML recommendation via POST /api/routines/.
     * This must create a Routine (not a Scene) so it shows up in the Routines tab,
     * not the Scenes tab — the recommendation only has device_id/action/suggested_time,
     * so value is left null and days_of_week defaults to every day (the simpler
     * /ml/recommendations endpoint does not return specific days like /routines/detect does).
     */
    fun createRoutineFromRecommendation(recommendation: RoutineRecommendation) {
        viewModelScope.launch {
            runCatching {
                val request = com.denis.smarthome.data.model.RoutineCreate(
                    name = "${recommendation.device_name} at ${recommendation.suggested_time}",
                    device_id = recommendation.device_id,
                    action = recommendation.action,
                    value = null,
                    trigger_time = recommendation.suggested_time,
                    days_of_week = "1,2,3,4,5,6,7"
                )
                RetrofitClient.apiService.createRoutine(request)
            }.onSuccess {
                dismissRecommendation(recommendation)
            }.onFailure { _error.value = it.message }
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
                .onFailure { _error.value = it.message }
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
                .onFailure { _error.value = it.message }
        }
    }

    /**
     * Executes a scene via POST /api/scenes/{id}/execute and shows snackbar result.
     *
     * Sets [_executingSceneId] while the request is in flight so the chip can show
     * a loading state, then clears it regardless of success or failure.
     *
     * @param id scene ID to execute
     */
    fun executeQuickScene(id: Int) {
        viewModelScope.launch {
            _executingSceneId.value = id
            runCatching { RetrofitClient.apiService.executeScene(id) }
                .onFailure { _error.value = it.message }
            _executingSceneId.value = null
        }
    }
}
