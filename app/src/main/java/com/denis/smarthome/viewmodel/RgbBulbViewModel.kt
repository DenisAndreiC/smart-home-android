/**
 * RgbBulbViewModel.kt - ViewModel pentru controlul becului RGB cu telecomanda IR
 *
 * Gestioneaza starea becului RGB controlat prin infrarosu: pornit/oprit,
 * luminozitate (step up/down) si culoare discreta (red, green, blue etc.).
 *
 * IMPORTANT: Becul RGB este non-smart, controlat prin telecomanda IR (NEC protocol).
 * Nu suporta culori hex arbitrare sau procente de luminozitate.
 * Comenzile disponibile corespund butoanelor fizice de pe telecomanda 44-key / 24-key:
 * - Culori fixe: red, green, blue, warm_white, cool_white
 * - Luminozitate: brightness_up, brightness_down (un pas pe apasare)
 * - Efecte: flash, fade (44-key si 24-key)
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
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
 * Model pentru un buton de culoare IR predefinit.
 *
 * Fiecare buton corespunde unui buton fizic de pe telecomanda IR a becului RGB.
 * [irCommand] este comanda exacta trimisa catre ESP32 prin MQTT.
 *
 * @param name numele afisat in UI (ex: "Red", "Warm White")
 * @param color culoarea Compose pentru preview vizual in UI
 * @param irCommand comanda IR trimisa catre ESP32 (ex: "red", "warm_white")
 */
data class IrColorButton(
    val name: String,
    val color: Color,
    val irCommand: String
)

/**
 * Lista de culori disponibile pe telecomanda IR a becului RGB.
 * Aceste culori sunt comune atat telecomenzilor 44-key cat si 24-key.
 * Fiecare intrare mapeaza direct la o comanda IR acceptata de firmware-ul ESP32.
 */
val rgbIrColors = listOf(
    IrColorButton("Red",        Color(0xFFFF0000), "red"),
    IrColorButton("Green",      Color(0xFF00FF00), "green"),
    IrColorButton("Blue",       Color(0xFF0000FF), "blue"),
    IrColorButton("Warm White", Color(0xFFFFD700), "warm_white"),
    IrColorButton("Cool White", Color(0xFFE0E8FF), "cool_white")
)

/**
 * ViewModel pentru ecranul de control al becului RGB prin IR.
 *
 * Spre deosebire de un bec smart (WiFi/Bluetooth), becul IR are control limitat:
 * - Culori: doar cele disponibile pe telecomanda (5-20 culori fixe)
 * - Luminozitate: doar step up / step down (fara slider procentual)
 * - Efecte: flash, fade (disponibile pe 44-key si 24-key)
 *
 * Toate comenzile sunt trimise ca action + value catre backend,
 * care le mapeaza la comanda IR corecta prin MQTT → ESP32 → IR LED → bec.
 *
 * @param application contextul aplicatiei
 * @param deviceId ID-ul becului RGB din sistemul SmartHome
 */
class RgbBulbViewModel(
    application: Application,
    private val deviceId: Int
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    private val _device = MutableStateFlow<DeviceResponse?>(null)
    val device: StateFlow<DeviceResponse?> = _device.asStateFlow()

    // Starea de pornit/oprit sincronizata cu is_active din API la incarcare
    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    // Culoarea curenta selectata — reflecta ultima culoare IR trimisa
    private val _selectedColor = MutableStateFlow(Color(0xFFFF0000))
    val selectedColor: StateFlow<Color> = _selectedColor.asStateFlow()

    // Numele culorii active (comanda IR) — folosit pentru highlight in UI
    private val _activeColorCommand = MutableStateFlow("red")
    val activeColorCommand: StateFlow<String> = _activeColorCommand.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadDevice() }

    /**
     * Incarca detaliile becului din API si initializeaza starea isOn.
     */
    fun loadDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getDevices()
                .onSuccess { devices ->
                    _device.value = devices.find { it.id == deviceId }
                    // Sincronizam starea locala cu starea reala din API
                    _isOn.value = _device.value?.is_online ?: false
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    /**
     * Comuta starea de pornit/oprit a becului si trimite comanda IR power.
     */
    fun togglePower() {
        _isOn.value = !_isOn.value
        sendCommand("power", if (_isOn.value) "on" else "off")
    }

    /**
     * Trimite comanda IR pentru o culoare specifica.
     * Actualizeaza starea locala (culoarea selectata si comanda activa).
     *
     * @param irColor butonul de culoare IR selectat din lista [rgbIrColors]
     */
    fun selectColor(irColor: IrColorButton) {
        _selectedColor.value = irColor.color
        _activeColorCommand.value = irColor.irCommand
        sendCommand("color", irColor.irCommand)
    }

    /**
     * Trimite comanda IR brightness_up — creste luminozitatea cu un pas.
     * Becul IR nu suporta procente, doar increment/decrement pe butonul fizic.
     */
    fun brightnessUp() {
        sendCommand("brightness_up", "up")
    }

    /**
     * Trimite comanda IR brightness_down — scade luminozitatea cu un pas.
     */
    fun brightnessDown() {
        sendCommand("brightness_down", "down")
    }

    /**
     * Trimite comanda IR pentru efect flash.
     */
    fun effectFlash() {
        sendCommand("flash", "flash")
    }

    /**
     * Trimite comanda IR pentru efect fade.
     */
    fun effectFade() {
        sendCommand("fade", "fade")
    }

    /**
     * Trimite o comanda catre becul RGB prin repository.
     *
     * @param type tipul comenzii (ex: "power", "color", "brightness_up")
     * @param data valoarea comenzii (ex: "on", "red", "up")
     */
    private fun sendCommand(type: String, data: String) {
        viewModelScope.launch {
            repository.sendCommand(
                CommandRequest(device_id = deviceId, action = type, value = data)
            ).onFailure { _error.value = it.message }
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
     * Factory pentru injectarea [deviceId] fara framework DI.
     *
     * Necesar deoarece AndroidViewModel nu suporta parametri suplimentari
     * fara un Factory explicit.
     */
    class Factory(private val application: Application, private val deviceId: Int) :
        ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return RgbBulbViewModel(application, deviceId) as T
        }
    }
}
