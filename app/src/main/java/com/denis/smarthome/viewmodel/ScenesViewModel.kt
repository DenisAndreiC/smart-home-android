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
     * Tiparele noi detectate sunt salvate pe backend ca rutine inactive (is_active=false);
     * utilizatorul le poate revizui si activa din lista de rutine.
     */
    fun generateMlRoutines() {
        viewModelScope.launch {
            _isLoadingRoutines.value = true
            routineRepository.detectRoutines()
                .onSuccess { result ->
                    _mlMessage.value = if (result.routines_saved > 0)
                        "${result.routines_saved} new routine(s) detected"
                    else
                        "No new routines detected"
                    loadRoutines()
                }
                .onFailure { _error.value = it.message }
            _isLoadingRoutines.value = false
        }
    }

    /** Sterge mesajul de detectie ML curent. */
    fun clearMlMessage() { _mlMessage.value = null }
}
