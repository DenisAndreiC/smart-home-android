/**
 * LampControlViewModel.kt - ViewModel pentru controlul lampii inteligente (releu)
 *
 * Gestioneaza starea lampii: pornit/oprit cu optimistic update si revert on failure.
 * Nu afiseaza date de consum energetic — backend-ul nu are inca un calcul real al
 * acestora (kW hardcodat), asa ca nu sunt expuse in UI pentru a nu induce in eroare.
 * Automatizarea programata se face prin Routines (ecranul Scenes), nu prin schedules
 * locale — backend-ul nu are un endpoint de schedules per-dispozitiv.
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
 * ViewModel pentru ecranul de control al lampii simple (tip releu).
 *
 * Spre deosebire de becul RGB, lampa are control binar (on/off) fara optiuni de culoare.
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
