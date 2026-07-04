/**
 * LampControlViewModel.kt - ViewModel pentru controlul lampii inteligente (releu)
 *
 * Gestioneaza starea lampii: pornit/oprit cu optimistic update si revert on failure,
 * plus o lista de programari (schedules) gestionate local.
 * Nu afiseaza date de consum energetic — backend-ul nu are inca un calcul real al
 * acestora (kW hardcodat), asa ca nu sunt expuse in UI pentru a nu induce in eroare.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.CommandRequest
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Model de date pentru o programare (scheduler) a lampii.
 *
 * Programarile sunt gestionate doar local in ViewModel (nu sunt persistate pe server).
 * Backend-ul nu are un endpoint dedicat pentru schedule-uri in aceasta versiune.
 *
 * @param name numele descriptiv al programarii (ex: "Sunset On")
 * @param time ora la care se declanseaza (ex: "6:45 PM")
 * @param repeat frecventa: "Daily", "Weekdays" etc.
 * @param isOn daca programarea este activa sau dezactivata
 * @param icon iconita afisata in UI: "sunset" sau "night"
 */
data class Schedule(
    val name: String,
    val time: String,
    val repeat: String,
    val isOn: Boolean,
    val icon: String  // "sunset" | "night"
)

/**
 * ViewModel pentru ecranul de control al lampii simple (tip releu).
 *
 * Spre deosebire de becul RGB, lampa are control binar (on/off) fara optiuni de culoare.
 * Contine si o sectiune de programari (schedules) pentru automatizare locala.
 *
 * @param application contextul aplicatiei
 * @param deviceId ID-ul dispozitivului releu asociat lampii
 * @param initialStatus last_status-ul dispozitivului la momentul navigarii (de pe lista de
 *   dispozitive), folosit pentru afisarea imediata a starii corecte inainte de reincarcare
 */
class LampControlViewModel(
    application: Application,
    private val deviceId: Int,
    initialStatus: String? = null
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    private val _device = MutableStateFlow<DeviceResponse?>(null)
    val device: StateFlow<DeviceResponse?> = _device.asStateFlow()

    // Starea de pornit/oprit, sincronizata cu last_status din API la incarcare.
    // Initializata din initialStatus (transmis de la ecranul de lista) pentru a evita
    // un flash vizual de "OFF" cat timp loadDevice() reincarca datele din retea.
    private val _isOn = MutableStateFlow(initialStatus?.lowercase() == "on")
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    /**
     * Lista de programari initializata cu doua intrari implicite.
     *
     * "Sunset On" porneste lampa la apusul soarelui (6:45 PM).
     * "Night Off" opreste lampa noaptea tarziu (11:30 PM).
     * Programarile sunt gestionate doar in memorie, nu sunt persistate.
     */
    private val _schedules = MutableStateFlow(
        listOf(
            Schedule("Sunset On",  "6:45 PM",  "Daily", true,  "sunset"),
            Schedule("Night Off",  "11:30 PM", "Daily", false, "night")
        )
    )
    val schedules: StateFlow<List<Schedule>> = _schedules.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadDevice() }

    /**
     * Incarca detaliile lampii din API si initializeaza starea isOn.
     */
    fun loadDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getDevices()
                .onSuccess { devices ->
                    val d = devices.find { it.id == deviceId }
                    _device.value = d
                    // Sincronizam starea locala cu last_status (aceeasi conventie ca in
                    // HomeViewModel): "on" explicit, sau online fara status raportat inca
                    _isOn.value = d?.let {
                        it.last_status?.lowercase() == "on" || (it.last_status == null && it.is_online)
                    } ?: false
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    /**
     * Comuta starea lampii cu optimistic update si revert on failure.
     *
     * Strategia: actualizam starea local inainte de confirmarea API pentru
     * raspuns vizual imediat. Daca API-ul returneaza eroare, revenim la starea anterioara.
     */
    fun togglePower() {
        // Optimistic update: comutam starea local inainte de raspunsul API
        _isOn.value = !_isOn.value
        viewModelScope.launch {
            repository.sendCommand(
                CommandRequest(
                    device_id = deviceId,
                    action = "power",
                    value = if (_isOn.value) "on" else "off"
                )
            ).onFailure {
                // Revert: restauram starea anterioara daca API-ul a esuat
                _isOn.value = !_isOn.value
                _error.value = it.message
            }
        }
    }

    /**
     * Comuta starea activ/inactiv a unei programari dupa indexul sau din lista.
     *
     * @param index pozitia programarii in lista [_schedules]
     */
    fun toggleSchedule(index: Int) {
        // Actualizam doar elementul de la indexul dat, pastrand restul neschimbat
        _schedules.value = _schedules.value.mapIndexed { i, s ->
            if (i == index) s.copy(isOn = !s.isOn) else s
        }
    }

    /**
     * Adauga o noua programare la sfarsitul listei.
     *
     * @param schedule programarea noua de adaugat
     */
    fun addSchedule(schedule: Schedule) {
        _schedules.value = _schedules.value + schedule
    }

    /** Sterge mesajul de eroare curent. */
    fun clearError() { _error.value = null }

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    fun deleteDevice() {
        viewModelScope.launch {
            repository.deleteDevice(deviceId)
                .onSuccess { _isDeleted.value = true }
                .onFailure { _error.value = it.message }
        }
    }

    /**
     * Factory pentru injectarea [deviceId] si [initialStatus] fara framework DI.
     */
    class Factory(
        private val application: Application,
        private val deviceId: Int,
        private val initialStatus: String? = null
    ) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LampControlViewModel(application, deviceId, initialStatus) as T
        }
    }
}
