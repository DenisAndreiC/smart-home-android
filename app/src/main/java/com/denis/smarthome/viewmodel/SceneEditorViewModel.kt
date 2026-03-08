/**
 * SceneEditorViewModel.kt - ViewModel pentru editorul de scene IoT
 *
 * Gestioneaza crearea unei scene noi prin un flux in 3 pasi: selectarea dispozitivului,
 * selectarea actiunii/comenzii si configurarea delay-ului optional.
 * Scena finala este trimisa la API prin POST /scenes dupa maparea
 * SceneActionForm -> SceneAction -> SceneRequest.
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
import com.denis.smarthome.data.model.SceneAction
import com.denis.smarthome.data.model.SceneRequest
import com.denis.smarthome.data.repository.DeviceRepository
import com.denis.smarthome.data.repository.SceneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Formular intermediar pentru o actiune din scena, inainte de trimiterea la API.
 *
 * Reprezinta un pas in scena: ce dispozitiv, ce comanda si cu ce intarziere.
 * Este o versiune mai user-friendly decat [SceneAction] din model, continand
 * si numele dispozitivului pentru afisare in UI (nu doar ID-ul).
 *
 * Fluxul de creare: utilizatorul parcurge 3 pasi:
 * 1. Selecteaza dispozitivul (din [availableDevices])
 * 2. Selecteaza actiunea (command_type + command_data specifice tipului de dispozitiv)
 * 3. Configureaza optional un delay in secunde fata de actiunea anterioara
 *
 * @param deviceId ID-ul dispozitivului tinta
 * @param deviceName numele dispozitivului (pentru afisare in lista de actiuni)
 * @param commandType tipul comenzii (ex: "power", "ir", "set_color")
 * @param commandData valoarea comenzii (ex: "on", "vol_up", "#FF0000")
 * @param delaySeconds intarzierea in secunde fata de actiunea anterioara din scena
 */
data class SceneActionForm(
    val deviceId: Int,
    val deviceName: String,
    val commandType: String,
    val commandData: String?,
    val delaySeconds: Int = 0
)

/**
 * ViewModel pentru SceneEditorScreen.
 *
 * Coordoneaza crearea unei scene noi: incarca dispozitivele disponibile,
 * gestioneaza lista de actiuni adaugate si salveaza scena prin API.
 *
 * La salvare ([saveScene]), lista de [SceneActionForm] este mapata la lista de [SceneAction]
 * si impachetata intr-un [SceneRequest], care este trimis la POST /scenes.
 */
class SceneEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val sceneRepo = SceneRepository(RetrofitClient.apiService)
    private val deviceRepo = DeviceRepository(RetrofitClient.apiService)

    // Numele scenei introdus de utilizator (obligatoriu)
    private val _sceneName = MutableStateFlow("")
    val sceneName: StateFlow<String> = _sceneName.asStateFlow()

    // Iconita selectata pentru scena (ex: "Movie", "Sleep", "Morning")
    private val _selectedIcon = MutableStateFlow("Movie")
    val selectedIcon: StateFlow<String> = _selectedIcon.asStateFlow()

    // Lista de actiuni adaugate scenei, in ordinea de executie
    private val _actions = MutableStateFlow<List<SceneActionForm>>(emptyList())
    val actions: StateFlow<List<SceneActionForm>> = _actions.asStateFlow()

    // Lista de dispozitive disponibile pentru selectie in pasul 1
    private val _availableDevices = MutableStateFlow<List<DeviceResponse>>(emptyList())
    val availableDevices: StateFlow<List<DeviceResponse>> = _availableDevices.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Flag care devine true dupa salvarea reusita, folosit de UI pentru navigare inapoi
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadDevices() }

    /**
     * Incarca lista de dispozitive disponibile pentru selectie in editor.
     */
    fun loadDevices() {
        viewModelScope.launch {
            deviceRepo.getDevices()
                .onSuccess { _availableDevices.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    /** Actualizeaza numele scenei. */
    fun setName(name: String) { _sceneName.value = name }

    /** Actualizeaza iconita selectata pentru scena. */
    fun setIcon(icon: String) { _selectedIcon.value = icon }

    /**
     * Adauga o actiune noua la sfarsitul listei de actiuni ale scenei.
     *
     * @param action actiunea configurata de utilizator (pasii 1-3 din flux)
     */
    fun addAction(action: SceneActionForm) {
        _actions.value = _actions.value + action
    }

    /**
     * Elimina actiunea de la pozitia specificata din lista.
     *
     * @param index indexul actiunii de eliminat
     */
    fun removeAction(index: Int) {
        _actions.value = _actions.value.toMutableList().also { it.removeAt(index) }
    }

    /**
     * Valideaza si salveaza scena prin API.
     *
     * Mapare: [SceneActionForm] → [SceneAction] → [SceneRequest] → POST /scenes
     * Fiecare [SceneActionForm] din lista este convertit la [SceneAction] prin
     * extragerea campurilor necesare API-ului (eliminand deviceName care e doar pentru UI).
     * Dupa salvare reusita, [saveSuccess] devine true, semnalizand UI-ului sa navigheze inapoi.
     */
    fun saveScene() {
        if (_sceneName.value.isBlank()) { _error.value = "Scene name is required"; return }
        viewModelScope.launch {
            _isLoading.value = true
            sceneRepo.createScene(
                SceneRequest(
                    name = _sceneName.value,
                    icon = _selectedIcon.value,
                    // Mapam fiecare SceneActionForm la SceneAction pentru API
                    actions = _actions.value.map { form ->
                        SceneAction(
                            device_id = form.deviceId,
                            command_type = form.commandType,
                            command_data = form.commandData,
                            delay_seconds = form.delaySeconds
                        )
                    }
                )
            ).onSuccess {
                // Semnalam UI-ului ca salvarea a reusit (trigger pentru navigare)
                _saveSuccess.value = true
            }.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    /** Sterge mesajul de eroare curent. */
    fun clearError() { _error.value = null }

    /** Reseteaza flag-ul de succes dupa ce UI-ul a procesat navigarea. */
    fun clearSaveSuccess() { _saveSuccess.value = false }
}
