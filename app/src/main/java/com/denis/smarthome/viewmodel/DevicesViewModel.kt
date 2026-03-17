/**
 * DevicesViewModel.kt - ViewModel pentru lista de dispozitive
 *
 * Gestioneaza lista completa de dispozitive, filtrarea acestora dupa tip sau camera,
 * si operatiile CRUD (adaugare, stergere, pornire/oprire).
 * Foloseste combine() pentru a reactiva filtrarea ori de cate ori se schimba
 * lista de dispozitive, filtrul selectat sau camera selectata.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.CommandRequest
import com.denis.smarthome.data.model.DeviceRequest
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Enum pentru tipurile de filtre disponibile in DevicesListScreen.
 *
 * - [ALL]: afiseaza toate dispozitivele, indiferent de tip sau camera
 * - [IR]: afiseaza doar dispozitivele de tip infrarosu (telecomanda, AC, TV)
 * - [RELAY]: afiseaza doar dispozitivele de tip releu (prize, becuri simple)
 * - [ROOM]: filtreaza dupa camera selectata in [DevicesViewModel.selectedRoom]
 */
enum class DeviceFilter { ALL, IR, RELAY, ROOM }

/**
 * Starea formularului de adaugare dispozitiv nou.
 *
 * Pastrata intr-un StateFlow separat pentru a nu polua starea principala a ViewModel-ului.
 * Resetata la valorile implicite dupa inchiderea dialogului.
 *
 * @param name numele afisabil al dispozitivului
 * @param deviceType tipul: "relay" sau "ir"
 * @param room camera in care se afla dispozitivul
 * @param mqttTopic topicul MQTT pe care dispozitivul asculta comenzi
 */
data class AddDeviceFormState(
    val name: String = "",
    val deviceType: String = "relay",
    val room: String = "",
    val mqttTopic: String = "smarthome/devices/relay/command",
    val brand: String = ""
)

/**
 * ViewModel pentru DevicesListScreen.
 *
 * Lista filtrata [filteredDevices] este calculata automat cu [combine] din 3 fluxuri:
 * lista completa de dispozitive, filtrul activ si camera selectata.
 * Lista de camere [rooms] este derivata din [_allDevices] prin map+distinct+sorted,
 * fara a necesita un apel API separat.
 */
class DevicesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    // Lista completa de dispozitive incarcata din API; nu este expusa direct catre UI
    private val _allDevices = MutableStateFlow<List<DeviceResponse>>(emptyList())

    private val _selectedFilter = MutableStateFlow(DeviceFilter.ALL)
    val selectedFilter: StateFlow<DeviceFilter> = _selectedFilter.asStateFlow()

    private val _selectedRoom = MutableStateFlow<String?>(null)
    val selectedRoom: StateFlow<String?> = _selectedRoom.asStateFlow()

    /**
     * Lista de dispozitive filtrata, recalculata automat la orice schimbare.
     *
     * Foloseste [combine] pentru a reactiva din 3 fluxuri simultan:
     * _allDevices, _selectedFilter si _selectedRoom.
     * [SharingStarted.WhileSubscribed] cu 5000ms pastreaza fluxul activ
     * 5 secunde dupa ce ultimul subscriber dispare (ex: rotire ecran).
     */
    val filteredDevices: StateFlow<List<DeviceResponse>> = combine(
        _allDevices, _selectedFilter, _selectedRoom
    ) { devices, filter, room ->
        when (filter) {
            DeviceFilter.ALL -> devices
            // Comparatie case-insensitive pentru robustete
            DeviceFilter.IR -> devices.filter { it.device_type.lowercase().startsWith("ir") }
            DeviceFilter.RELAY -> devices.filter { it.device_type.lowercase() == "relay" }
            // Daca filtrul este ROOM dar nu e selectata o camera, returnam toate dispozitivele
            DeviceFilter.ROOM -> room?.let { r -> devices.filter { it.room == r } } ?: devices
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Lista de camere unice, sortata alfabetic.
     *
     * Derivata din [_allDevices] prin extragerea campului room, eliminarea duplicatelor
     * si sortarea — fara apel API suplimentar.
     */
    val rooms: StateFlow<List<String>> = _allDevices
        .map { devices -> devices.mapNotNull { it.room }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _addForm = MutableStateFlow(AddDeviceFormState())
    val addForm: StateFlow<AddDeviceFormState> = _addForm.asStateFlow()

    init {
        loadDevices()
    }

    /**
     * Incarca lista completa de dispozitive din API si actualizeaza [_allDevices].
     */
    fun loadDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getDevices()
                .onSuccess { _allDevices.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    /**
     * Schimba filtrul activ. Daca noul filtru nu este ROOM, reseteaza camera selectata.
     */
    fun setFilter(filter: DeviceFilter) {
        _selectedFilter.value = filter
        // Resetam camera selectata cand nu mai e activ filtrul ROOM
        if (filter != DeviceFilter.ROOM) _selectedRoom.value = null
    }

    /**
     * Activeaza filtrul ROOM si seteaza camera dupa care se filtreaza.
     */
    fun setRoomFilter(room: String) {
        _selectedFilter.value = DeviceFilter.ROOM
        _selectedRoom.value = room
    }

    /**
     * Comuta starea de pornit/oprit a unui dispozitiv cu optimistic update.
     *
     * Strategia optimistic update: actualizeaza starea local in [_allDevices] inainte
     * de a primi raspunsul de la API, pentru o interfata mai reactiva.
     * Daca cererea API esueaza, revine la starea anterioara (revert).
     *
     * @param deviceId ID-ul dispozitivului de comutat
     * @param isOn noua stare dorita (true = pornit, false = oprit)
     */
    fun toggleDevice(deviceId: Int, isOn: Boolean) {
        // Optimistic update: actualizam UI inainte de confirmarea API
        _allDevices.value = _allDevices.value.map {
            if (it.id == deviceId) it.copy(is_online = isOn, last_status = if (isOn) "on" else "off") else it
        }
        viewModelScope.launch {
            repository.sendCommand(
                CommandRequest(
                    device_id = deviceId,
                    action = "power",
                    value = if (isOn) "on" else "off"
                )
            ).onFailure {
                // Revert on failure: restauram starea anterioara daca API-ul refuza comanda
                _allDevices.value = _allDevices.value.map { device ->
                    if (device.id == deviceId) device.copy(is_online = !isOn, last_status = if (!isOn) "on" else "off") else device
                }
                _error.value = it.message
            }
        }
    }

    /** Afiseaza dialogul de adaugare dispozitiv. */
    fun showAddDialog() { _showAddDialog.value = true }

    /**
     * Ascunde dialogul si reseteaza formularul la valorile implicite.
     */
    fun hideAddDialog() {
        _showAddDialog.value = false
        _addForm.value = AddDeviceFormState()
    }

    /** Actualizeaza starea formularului de adaugare cu noile valori introduse de utilizator. */
    fun updateAddForm(state: AddDeviceFormState) { _addForm.value = state }

    /**
     * Valideaza si trimite cererea de creare a unui dispozitiv nou.
     *
     * Daca topic-ul MQTT nu e completat, il genereaza automat din numele dispozitivului.
     */
    fun addDevice() {
        val form = _addForm.value
        if (form.name.isBlank()) { _error.value = "Device name required"; return }
        val isIr = form.deviceType.startsWith("ir")
        val mqtt = if (isIr) "smarthome/devices/ir/command" else "smarthome/devices/relay/command"
        val irCodes = if (isIr && form.brand.isNotBlank())
            mapOf("brand" to form.brand.lowercase()) else null
        viewModelScope.launch {
            repository.createDevice(
                DeviceRequest(
                    name = form.name,
                    device_type = form.deviceType,
                    room = form.room.ifBlank { "Default" },
                    mqtt_topic = mqtt,
                    ir_codes = irCodes
                )
            ).onSuccess {
                if (form.deviceType == "ir_tv" && form.brand.isNotBlank()) {
                    repository.setBrand(form.brand.lowercase())
                }
                hideAddDialog()
                loadDevices()
            }.onFailure { _error.value = it.message }
        }
    }

    /**
     * Sterge un dispozitiv dupa ID si reincarca lista.
     */
    fun deleteDevice(id: Int) {
        viewModelScope.launch {
            repository.deleteDevice(id).onSuccess { loadDevices() }
        }
    }

    /** Sterge mesajul de eroare curent. */
    fun clearError() { _error.value = null }
}
