/**
 * DeviceControlViewModel.kt - ViewModel de baza pentru controlul unui dispozitiv specific
 *
 * Incarca detaliile unui dispozitiv dupa ID din lista completa returnata de API.
 * Serveste ca punct de plecare pentru ViewModel-urile specializate (TV, AC, RGB etc.).
 * Contine Factory inner class pentru a injecta deviceId fara framework DI.
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
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pentru ecranul de control al unui dispozitiv individual.
 *
 * Nu exista endpoint GET /devices/{id} — dispozitivul este gasit prin filtrarea
 * listei complete GET /devices cu [find] dupa [deviceId].
 * Aceasta abordare este suficienta pentru proiectul de licenta, unde numarul de
 * dispozitive este mic.
 *
 * @param application contextul aplicatiei necesar pentru AndroidViewModel
 * @param deviceId ID-ul dispozitivului de incarcat si controlat
 */
class DeviceControlViewModel(
    application: Application,
    val deviceId: Int
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    private val _device = MutableStateFlow<DeviceResponse?>(null)
    val device: StateFlow<DeviceResponse?> = _device.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadDevice()
    }

    /**
     * Incarca dispozitivul din API prin cautarea ID-ului in lista completa.
     *
     * Daca dispozitivul cu [deviceId] nu este gasit in lista, seteaza mesaj de eroare.
     */
    fun loadDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getDevices()
                .onSuccess { devices ->
                    // Cautam dispozitivul dupa ID in lista completa returnata de API
                    _device.value = devices.find { it.id == deviceId }
                    if (_device.value == null) _error.value = "Device not found"
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    /**
     * Factory pentru crearea ViewModel-ului cu parametrul [deviceId].
     *
     * Deoarece proiectul nu foloseste un framework DI (Hilt/Koin) — arhitectura
     * a fost aleasa simpla pentru scopul lucrarii de licenta — parametrul deviceId
     * nu poate fi injectat automat. Factory-ul permite transmiterea manuala a
     * parametrului la crearea ViewModel-ului prin [ViewModelProvider].
     */
    class Factory(private val application: Application, private val deviceId: Int) :
        ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DeviceControlViewModel(application, deviceId) as T
        }
    }
}
