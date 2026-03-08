/**
 * TvRemoteViewModel.kt - ViewModel pentru telecomanda TV prin infrarosu
 *
 * Trimite comenzi IR catre dispozitivul TV prin POST /commands/send cu command_type="ir".
 * Gestioneaza starea locala isOn si isMuted cu optimistic update, fara confirmare din API.
 * Fiecare buton al telecomenzii corespunde unei comenzi IR distincte.
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
import com.denis.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pentru ecranul de telecomanda TV.
 *
 * Comenzile IR suportate: power, vol_up, vol_down, ch_up, ch_down,
 * mute, ok, menu, back, home, source.
 * Fiecare comanda trimite POST /commands/send cu command_type="ir" si
 * command_data egal cu numele comenzii (ex: "vol_up", "ch_down").
 *
 * Starea isOn si isMuted este gestionata local (optimistic) — nu se interogheaza
 * starea reala a televizorului, deoarece dispozitivele IR nu au feedback bidiractional.
 *
 * @param application contextul aplicatiei
 * @param deviceId ID-ul dispozitivului IR asociat televizorului
 */
class TvRemoteViewModel(
    application: Application,
    private val deviceId: Int
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    // Stare locala optimista: nu stim starea reala a TV-ului (IR e unidirectional)
    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    // Starea de mute gestionata local, comutata la fiecare apasare a butonului mute
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // Ultima comanda trimisa, folosita de UI pentru animatii/feedback vizual
    private val _commandSent = MutableStateFlow<String?>(null)
    val commandSent: StateFlow<String?> = _commandSent.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Trimite o comanda IR catre dispozitivul TV.
     *
     * Comenzile "power" si "mute" comuta si starea locala inainte de apelul API.
     * Toate celelalte comenzi (vol_up, ch_down etc.) sunt trimise direct fara
     * a modifica starea locala.
     *
     * @param command numele comenzii IR (ex: "power", "vol_up", "mute", "ok")
     */
    fun sendCommand(command: String) {
        viewModelScope.launch {
            // Actualizam starea locala pentru comenzile care au toggle logic
            when (command) {
                "power" -> _isOn.value = !_isOn.value
                "mute" -> _isMuted.value = !_isMuted.value
            }
            // Notificam UI-ul ca o comanda a fost trimisa (pentru feedback vizual)
            _commandSent.value = command
            repository.sendCommand(
                CommandRequest(
                    device_id = deviceId,
                    command_type = "ir",    // Toate comenzile TV sunt de tip IR
                    command_data = command  // Numele comenzii devine payload-ul IR
                )
            ).onFailure { _error.value = it.message }
        }
    }

    /** Sterge eroarea curenta afisata in UI. */
    fun clearError() { _error.value = null }

    /** Reseteaza ultima comanda trimisa dupa ce UI-ul a procesat feedback-ul. */
    fun clearCommand() { _commandSent.value = null }

    /**
     * Factory pentru injectarea [deviceId] fara framework DI.
     *
     * Arhitectura proiectului de licenta nu foloseste Hilt/Koin pentru a mentine
     * simplicitatea. Factory-ul permite transmiterea manuala a parametrului.
     */
    class Factory(private val application: Application, private val deviceId: Int) :
        ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TvRemoteViewModel(application, deviceId) as T
        }
    }
}
