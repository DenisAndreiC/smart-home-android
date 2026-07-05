/**
 * ScenesViewModel.kt - ViewModel pentru ecranul de gestionare a scenelor
 *
 * Incarca lista de scene din API, permite executia si stergerea acestora.
 * Foloseste executingSceneId pentru a oferi feedback vizual pe butonul "Activate"
 * in timp ce scena se executa, cu un delay de 2 secunde inainte de resetare.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.data.model.RoutineCandidate
import com.denis.smarthome.data.model.RoutineCreate
import com.denis.smarthome.data.model.RoutineResponse
import com.denis.smarthome.data.model.SceneResponse
import com.denis.smarthome.data.repository.DeviceRepository
import com.denis.smarthome.data.repository.RoutineRepository
import com.denis.smarthome.data.repository.SceneRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pentru ScenesScreen.
 *
 * Gestioneaza lista de scene IoT si operatiile pe acestea: incarcare, executie, stergere.
 * Scenele sunt secvente de actiuni pe mai multe dispozitive, executate in ordinea definita.
 */
class ScenesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SceneRepository(RetrofitClient.apiService)
    private val routineRepository = RoutineRepository(RetrofitClient.apiService)
    private val deviceRepository = DeviceRepository(RetrofitClient.apiService)

    private val _scenes = MutableStateFlow<List<SceneResponse>>(emptyList())
    val scenes: StateFlow<List<SceneResponse>> = _scenes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Rutine automate (manuale + sugerate de ML), separate de scenele executate manual
    private val _routines = MutableStateFlow<List<RoutineResponse>>(emptyList())
    val routines: StateFlow<List<RoutineResponse>> = _routines.asStateFlow()

    // Dispozitivele utilizatorului, folosite pentru afisarea numelui in cardul rutinei
    // si pentru selectorul de dispozitiv din dialogul de creare manuala
    private val _devices = MutableStateFlow<List<DeviceResponse>>(emptyList())
    val devices: StateFlow<List<DeviceResponse>> = _devices.asStateFlow()

    private val _isLoadingRoutines = MutableStateFlow(false)
    val isLoadingRoutines: StateFlow<Boolean> = _isLoadingRoutines.asStateFlow()

    // Mesaj afisat dupa rularea detectiei ML (ex: "2 rutine noi detectate")
    private val _mlMessage = MutableStateFlow<String?>(null)
    val mlMessage: StateFlow<String?> = _mlMessage.asStateFlow()

    // Candidatii returnati de GET /routines/detect — doar sugestii, nimic salvat inca.
    // Utilizatorul bifeaza care sa fie create prin dialogul de selectie.
    private val _mlCandidates = MutableStateFlow<List<RoutineCandidate>>(emptyList())
    val mlCandidates: StateFlow<List<RoutineCandidate>> = _mlCandidates.asStateFlow()

    private val _showMlCandidatesDialog = MutableStateFlow(false)
    val showMlCandidatesDialog: StateFlow<Boolean> = _showMlCandidatesDialog.asStateFlow()

    /**
     * ID-ul scenei care se executa in momentul curent.
     *
     * Folosit de UI pentru feedback vizual: butonul "Activate" al scenei cu acest ID
     * afiseaza un indicator de incarcare sau o animatie. Valoarea este null cand nicio
     * scena nu se executa si revine la null dupa 2 secunde de la finalizare.
     */
    private val _executingSceneId = MutableStateFlow<Int?>(null)
    val executingSceneId: StateFlow<Int?> = _executingSceneId.asStateFlow()

    init { loadScenes(); loadRoutines(); loadDevices() }

    /**
     * Incarca lista completa de scene disponibile din API.
     */
    fun loadScenes() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getScenes()
                .onSuccess { _scenes.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    /**
     * Executa o scena si ofera feedback vizual pe durata executiei.
     *
     * Flow: seteaza executingSceneId = id → apeleaza API → afiseaza mesaj succes/eroare
     * → delay(2000ms) pentru a mentine feedback-ul vizibil → reseteaza executingSceneId la null.
     * Delay-ul de 2 secunde permite utilizatorului sa observe ca butonul a reactionat
     * inainte ca el sa revina la starea normala.
     *
     * @param id ID-ul scenei de executat
     */
    fun executeScene(id: Int) {
        viewModelScope.launch {
            // Marcam scena ca "in executie" pentru feedback vizual in UI
            _executingSceneId.value = id
            repository.executeScene(id)
                .onFailure { _error.value = it.message }
            // Asteptam 2 secunde ca utilizatorul sa vada feedback-ul vizual
            delay(2000)
            // Resetam ID-ul pentru a reveni butonul la starea normala
            _executingSceneId.value = null
        }
    }

    /**
     * Sterge o scena dupa ID si reincarca lista.
     *
     * @param id ID-ul scenei de sters
     */
    fun deleteScene(id: Int) {
        viewModelScope.launch {
            repository.deleteScene(id)
                .onSuccess { loadScenes() }  // Reincarcam lista dupa stergere reusita
                .onFailure { _error.value = it.message }
        }
    }

    /** Sterge mesajul de eroare curent. */
    fun clearError() { _error.value = null }

    /** Incarca lista de rutine (manuale + sugerate de ML) a utilizatorului curent. */
    fun loadRoutines() {
        viewModelScope.launch {
            _isLoadingRoutines.value = true
            routineRepository.getRoutines()
                .onSuccess { _routines.value = it }
                .onFailure { _error.value = it.message }
            _isLoadingRoutines.value = false
        }
    }

    /** Incarca lista de dispozitive, folosita pentru afisare si pentru crearea manuala de rutine. */
    private fun loadDevices() {
        viewModelScope.launch {
            deviceRepository.getDevices().onSuccess { _devices.value = it }
        }
    }

    /**
     * Activeaza/dezactiveaza o rutina. Rutinele ML sunt salvate inactive implicit,
     * deci utilizatorul trebuie sa le activeze manual din UI pentru ca schedulerul
     * de pe backend sa inceapa sa le execute.
     */
    fun toggleRoutine(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            routineRepository.toggleRoutine(id, isActive)
                .onSuccess { updated ->
                    _routines.value = _routines.value.map { if (it.id == updated.id) updated else it }
                }
                .onFailure { _error.value = it.message }
        }
    }

    /** Sterge o rutina si reincarca lista. */
    fun deleteRoutine(id: Int) {
        viewModelScope.launch {
            routineRepository.deleteRoutine(id)
                .onSuccess { loadRoutines() }
                .onFailure { _error.value = it.message }
        }
    }

    /** Creeaza o rutina manuala noua si reincarca lista dupa succes. */
    fun createRoutine(name: String, deviceId: Int, action: String, value: String?, triggerTime: String, daysOfWeek: String) {
        viewModelScope.launch {
            routineRepository.createRoutine(
                RoutineCreate(
                    name = name,
                    device_id = deviceId,
                    action = action,
                    value = value,
                    trigger_time = triggerTime,
                    days_of_week = daysOfWeek
                )
            ).onSuccess { loadRoutines() }
                .onFailure { _error.value = it.message }
        }
    }

    /**
     * Ruleaza detectia ML (DBSCAN) pe istoricul de comenzi al utilizatorului.
     * GET /routines/detect este READ-ONLY — nu salveaza nimic, doar returneaza
     * candidati. Daca exista candidati, deschidem dialogul de selectie; altfel
     * afisam un mesaj ca nu s-a gasit nimic nou.
     *
     * Citim min_occurrences/min_days curente din Settings (GET /ml/settings) si le
     * trimitem explicit ca parametri catre /routines/detect, ca dialogul sa arate
     * exact aceeasi lista de candidati ca recomandarile de pe dashboard pentru
     * aceleasi valori de slider.
     */
    fun generateMlRoutines() {
        viewModelScope.launch {
            _isLoadingRoutines.value = true
            val settings = runCatching { RetrofitClient.apiService.getMLSettings() }.getOrNull()
            routineRepository.detectRoutines(
                minOccurrences = settings?.min_occurrences,
                minDistinctDays = settings?.min_days
            )
                .onSuccess { result ->
                    if (result.data.isEmpty()) {
                        _mlMessage.value = "No new routines detected"
                    } else {
                        _mlCandidates.value = result.data
                        _showMlCandidatesDialog.value = true
                    }
                }
                .onFailure { _error.value = it.message }
            _isLoadingRoutines.value = false
        }
    }

    /** Inchide dialogul de selectie a candidatilor ML fara sa creeze nimic. */
    fun dismissMlCandidatesDialog() {
        _showMlCandidatesDialog.value = false
        _mlCandidates.value = emptyList()
    }

    /**
     * Creeaza doar rutinele bifate de utilizator in dialogul de selectie ML.
     * Fiecare candidat selectat devine un POST /routines/ separat (rutina manuala,
     * is_active=true implicit pe backend). Nu se creeaza automat toti candidatii.
     */
    fun createSelectedRoutineCandidates(selected: List<RoutineCandidate>) {
        viewModelScope.launch {
            var createdCount = 0
            selected.forEach { candidate ->
                routineRepository.createRoutine(
                    RoutineCreate(
                        name = candidate.name,
                        device_id = candidate.device_id,
                        action = candidate.action,
                        value = candidate.value,
                        trigger_time = candidate.trigger_time,
                        days_of_week = candidate.days_of_week
                    )
                ).onSuccess { createdCount++ }
                    .onFailure { _error.value = it.message }
            }
            _mlMessage.value = if (createdCount > 0) "$createdCount routine(s) created" else null
            _showMlCandidatesDialog.value = false
            _mlCandidates.value = emptyList()
            loadRoutines()
        }
    }

    /** Sterge mesajul de detectie ML curent. */
    fun clearMlMessage() { _mlMessage.value = null }
}
