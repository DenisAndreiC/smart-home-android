/**
 * RgbBulbViewModel.kt - ViewModel pentru controlul becului RGB inteligent
 *
 * Gestioneaza starea becului RGB: pornit/oprit, luminozitate, culoare selectata
 * si preseturile de culoare. Culoarea poate fi aleasa de pe o roata de culori
 * (ColorWheel composable) sau dintr-o lista de preseturi predefinite.
 * Culoarea este trimisa catre API in format hex (#RRGGBB).
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
 * Model pentru un preset de culoare predefinit.
 *
 * Fiecare preset contine culoarea Compose, numele afisat si unghiul pe roata de culori.
 * Unghiul ([angle]) este folosit pentru a pozitiona indicatorul pe ColorWheel
 * atunci cand utilizatorul selecteaza un preset, sincronizand vizual roata cu alegerea.
 *
 * @param name numele presetului afisat in UI (ex: "Warm", "Cool")
 * @param color culoarea Compose corespunzatoare
 * @param angle unghiul in grade pe roata de culori (0-360)
 */
data class ColorPreset(
    val name: String,
    val color: Color,
    val angle: Float
)

/**
 * Lista de preseturi de culoare predefinite cu unghiurile corespunzatoare pe roata de culori.
 *
 * Unghiurile sunt aproximative, bazate pe pozitia culorilor in modelul HSV:
 * - Warm (galben auriu): ~50 grade
 * - Cool (alb-albastru rece): ~215 grade
 * - Red (rosu pur): 0 grade (inceputul cercului)
 * - Blue (albastru pur): ~240 grade
 * - Green (verde pur): ~120 grade
 * - Purple (mov): ~290 grade
 */
val rgbPresets = listOf(
    ColorPreset("Warm",   Color(0xFFFFD700), 50f),
    ColorPreset("Cool",   Color(0xFFE0E8FF), 215f),
    ColorPreset("Red",    Color(0xFFFF0000), 0f),
    ColorPreset("Blue",   Color(0xFF0000FF), 240f),
    ColorPreset("Green",  Color(0xFF00FF00), 120f),
    ColorPreset("Purple", Color(0xFF9C27B0), 290f)
)

/**
 * ViewModel pentru ecranul de control al becului RGB.
 *
 * Ofera doua modalitati de selectare a culorii:
 * 1. Roata de culori (ColorWheel composable) — apeleaza [setColorFromWheel]
 * 2. Preseturi predefinite din [rgbPresets] — apeleaza [selectPreset]
 *
 * [selectedAngle] sincronizeaza pozitia indicatorului pe roata de culori cu culoarea
 * selectata, indiferent daca a fost aleasa din preset sau direct de pe roata.
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

    // Luminozitate procentuala (0-100), initiata la 85%
    private val _brightness = MutableStateFlow(85)
    val brightness: StateFlow<Int> = _brightness.asStateFlow()

    // Culoarea curenta selectata, initiata la culoarea primara a aplicatiei (teal)
    private val _selectedColor = MutableStateFlow(Color(0xFF00BCD4))
    val selectedColor: StateFlow<Color> = _selectedColor.asStateFlow()

    /**
     * Unghiul curent al indicatorului pe roata de culori.
     *
     * Actualizat atat la selectia directa de pe roata cat si la selectia unui preset,
     * asigurand sincronizarea vizuala intre cele doua metode de alegere a culorii.
     */
    private val _selectedAngle = MutableStateFlow(185f)
    val selectedAngle: StateFlow<Float> = _selectedAngle.asStateFlow()

    // Numele presetului activ sau null daca culoarea a fost aleasa liber de pe roata
    private val _selectedPreset = MutableStateFlow<String?>(null)
    val selectedPreset: StateFlow<String?> = _selectedPreset.asStateFlow()

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
     * Comuta starea de pornit/oprit a becului si trimite comanda API.
     */
    fun togglePower() {
        _isOn.value = !_isOn.value
        sendCommand("power", if (_isOn.value) "on" else "off")
    }

    /**
     * Seteaza luminozitatea becului (0-100) si trimite comanda API.
     *
     * @param value valoarea luminozitatii in procente
     */
    fun setBrightness(value: Int) {
        _brightness.value = value
        sendCommand("set_brightness", "$value")
    }

    /**
     * Actualizeaza culoarea din selectia directa pe roata de culori.
     *
     * Primeste culoarea si unghiul de la composable-ul ColorWheel.
     * Reseteaza presetul activ, deoarece utilizatorul a ales o culoare personalizata.
     *
     * @param color culoarea selectata pe roata
     * @param angle unghiul corespunzator pe roata (0-360 grade)
     */
    fun setColorFromWheel(color: Color, angle: Float) {
        _selectedColor.value = color
        _selectedAngle.value = angle
        // Deselectam presetul activ cand utilizatorul alege manual de pe roata
        _selectedPreset.value = null
        sendColorCommand(color)
    }

    /**
     * Aplica un preset de culoare predefinit.
     *
     * Actualizeaza culoarea, unghiul roatei si marcheaza presetul ca activ.
     *
     * @param preset presetul selectat din [rgbPresets]
     */
    fun selectPreset(preset: ColorPreset) {
        _selectedColor.value = preset.color
        // Pozitionam indicatorul roatei la unghiul corespunzator presetului
        _selectedAngle.value = preset.angle
        _selectedPreset.value = preset.name
        sendColorCommand(preset.color)
    }

    /**
     * Converteste o culoare Compose la format hex si trimite comanda set_color.
     *
     * Conversia: extrage componentele RGB (0.0-1.0), le inmulteste cu 255,
     * le converteste la Int si le formateaza ca hex cu 2 cifre (#RRGGBB).
     * Formatul hex este cerut de API-ul backend pentru controlul LED-urilor RGB.
     *
     * @param color culoarea Compose de convertit si trimis
     */
    private fun sendColorCommand(color: Color) {
        // Formatul #RRGGBB: fiecare componenta normalizata (0.0-1.0) inmultita cu 255
        val hex = "#%02X%02X%02X".format(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        sendCommand("set_color", hex)
    }

    /**
     * Trimite o comanda catre becul RGB prin repository.
     *
     * @param type tipul comenzii (ex: "power", "set_color", "set_brightness")
     * @param data valoarea comenzii (ex: "on", "#FF0000", "85")
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
