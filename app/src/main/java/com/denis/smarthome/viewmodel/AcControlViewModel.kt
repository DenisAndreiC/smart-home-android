/**
 * AcControlViewModel.kt - ViewModel pentru controlul aparatului de aer conditionat
 *
 * Gestioneaza starea AC-ului: pornit/oprit, temperatura (16-30 grade), modul de functionare,
 * viteza ventilatorului, oscilatie si timer. Toate comenzile sunt trimise prin IR.
 * Functia privata sendCommand() este punctul central prin care trec toate operatiile.
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
 * Modurile de functionare ale aparatului de aer conditionat.
 *
 * - [COOL]: racire — cel mai frecvent mod folosit vara
 * - [HEAT]: incalzire — pompare caldura in interior
 * - [FAN]: ventilare — circula aerul fara racire/incalzire
 * - [DRY]: dezumidificare — reduce umiditatea fara racire semnificativa
 * - [AUTO]: mod automat — AC-ul alege singur racire sau incalzire
 */
enum class AcMode { COOL, HEAT, FAN, DRY, AUTO }

/**
 * Vitezele disponibile ale ventilatorului intern al AC-ului.
 *
 * - [LOW]: viteza mica — silentios, consum redus
 * - [MED]: viteza medie — echilibru intre zgomot si eficienta
 * - [HIGH]: viteza mare — racire/incalzire rapida
 * - [AUTO]: viteza automat ajustata de AC in functie de temperatura dorita
 */
enum class FanSpeed { LOW, MED, HIGH, AUTO }

/**
 * ViewModel pentru ecranul de control al aparatului de aer conditionat.
 *
 * Toate functiile publice (togglePower, increaseTemperature, setMode etc.) apeleaza
 * intern functia privata [sendCommand] care construieste [CommandRequest] cu
 * command_type="ir" si trimite POST /commands/send prin repository.
 *
 * Temperatura este limitata la intervalul 16-30 grade Celsius, limitele standard
 * ale majoritatii aparatelor de aer conditionat de uz casnic.
 *
 * @param application contextul aplicatiei
 * @param deviceId ID-ul dispozitivului IR asociat AC-ului
 */
class AcControlViewModel(
    application: Application,
    private val deviceId: Int
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    // Starea de pornit/oprit a AC-ului, gestionata local (IR nu are feedback)
    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    // Temperatura setata, initiata la 22 grade (valoarea implicita confortabila)
    private val _temperature = MutableStateFlow(22)
    val temperature: StateFlow<Int> = _temperature.asStateFlow()

    // Modul de functionare, implicit COOL
    private val _mode = MutableStateFlow(AcMode.COOL)
    val mode: StateFlow<AcMode> = _mode.asStateFlow()

    // Viteza ventilatorului, implicit AUTO
    private val _fanSpeed = MutableStateFlow(FanSpeed.AUTO)
    val fanSpeed: StateFlow<FanSpeed> = _fanSpeed.asStateFlow()

    // Oscilatie (swing) a grilei de aer, implicit oprita
    private val _swingEnabled = MutableStateFlow(false)
    val swingEnabled: StateFlow<Boolean> = _swingEnabled.asStateFlow()

    // Timer in ore: 0 = dezactivat, >0 = AC se va opri dupa atatea ore
    private val _timerHours = MutableStateFlow(0)
    val timerHours: StateFlow<Int> = _timerHours.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Comuta starea de pornit/oprit si trimite comanda IR corespunzatoare.
     */
    fun togglePower() {
        _isOn.value = !_isOn.value
        sendCommand("power:${if (_isOn.value) "on" else "off"}")
    }

    /**
     * Creste temperatura cu 1 grad, maximum 30 grade Celsius.
     * Nu trimite comanda daca temperatura este deja la maxim.
     */
    fun increaseTemperature() {
        // Limita superioara: 30 grade Celsius
        if (_temperature.value < 30) {
            _temperature.value++
            sendCommand("temp:${_temperature.value}")
        }
    }

    /**
     * Scade temperatura cu 1 grad, minimum 16 grade Celsius.
     * Nu trimite comanda daca temperatura este deja la minim.
     */
    fun decreaseTemperature() {
        // Limita inferioara: 16 grade Celsius
        if (_temperature.value > 16) {
            _temperature.value--
            sendCommand("temp:${_temperature.value}")
        }
    }

    /**
     * Schimba modul de functionare al AC-ului si trimite comanda IR.
     *
     * @param acMode noul mod selectat de utilizator
     */
    fun setMode(acMode: AcMode) {
        _mode.value = acMode
        // Transmitem numele modului cu litere mici (ex: "cool", "heat")
        sendCommand("mode:${acMode.name.lowercase()}")
    }

    /**
     * Schimba viteza ventilatorului si trimite comanda IR.
     *
     * @param speed noua viteza selectata
     */
    fun setFanSpeed(speed: FanSpeed) {
        _fanSpeed.value = speed
        sendCommand("fan:${speed.name.lowercase()}")
    }

    /**
     * Comuta oscilatia grilei de aer si trimite comanda IR.
     */
    fun toggleSwing() {
        _swingEnabled.value = !_swingEnabled.value
        sendCommand("swing:${if (_swingEnabled.value) "on" else "off"}")
    }

    /**
     * Seteaza timer-ul AC-ului in ore.
     * Nu trimite comanda daca timer-ul este setat la 0 (dezactivat).
     *
     * @param hours numarul de ore dupa care AC-ul se va opri automat
     */
    fun setTimer(hours: Int) {
        _timerHours.value = hours
        // Nu trimitem comanda pentru dezactivarea timerului (hours == 0)
        if (hours > 0) sendCommand("timer:$hours")
    }

    /**
     * Functie privata centrala pentru trimiterea tuturor comenzilor IR.
     *
     * Toate functiile publice din ViewModel apeleaza aceasta metoda,
     * care construieste [CommandRequest] cu command_type="ir" si
     * delega catre repository. Astfel, logica de comunicare este centralizata.
     *
     * @param data payload-ul comenzii IR (ex: "power:on", "temp:24", "mode:cool")
     */
    private fun sendCommand(data: String) {
        viewModelScope.launch {
            repository.sendCommand(
                CommandRequest(
                    device_id = deviceId,
                    command_type = "ir",  // Toate comenzile AC sunt de tip IR
                    command_data = data
                )
            ).onFailure { _error.value = it.message }
        }
    }

    /** Sterge mesajul de eroare curent. */
    fun clearError() { _error.value = null }

    /**
     * Factory pentru injectarea [deviceId] fara framework DI.
     *
     * Arhitectura simpla a proiectului de licenta nu necesita Hilt sau Koin.
     * Factory-ul permite transmiterea parametrului la crearea ViewModel-ului.
     */
    class Factory(private val application: Application, private val deviceId: Int) :
        ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AcControlViewModel(application, deviceId) as T
        }
    }
}
