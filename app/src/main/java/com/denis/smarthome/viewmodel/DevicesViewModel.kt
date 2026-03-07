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

enum class DeviceFilter { ALL, IR, RELAY, ROOM }

data class AddDeviceFormState(
    val name: String = "",
    val deviceType: String = "relay",
    val room: String = "",
    val mqttTopic: String = "smarthome/devices/"
)

class DevicesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceRepository(RetrofitClient.apiService)

    private val _allDevices = MutableStateFlow<List<DeviceResponse>>(emptyList())

    private val _selectedFilter = MutableStateFlow(DeviceFilter.ALL)
    val selectedFilter: StateFlow<DeviceFilter> = _selectedFilter.asStateFlow()

    private val _selectedRoom = MutableStateFlow<String?>(null)
    val selectedRoom: StateFlow<String?> = _selectedRoom.asStateFlow()

    val filteredDevices: StateFlow<List<DeviceResponse>> = combine(
        _allDevices, _selectedFilter, _selectedRoom
    ) { devices, filter, room ->
        when (filter) {
            DeviceFilter.ALL -> devices
            DeviceFilter.IR -> devices.filter { it.device_type.lowercase() == "ir" }
            DeviceFilter.RELAY -> devices.filter { it.device_type.lowercase() == "relay" }
            DeviceFilter.ROOM -> room?.let { r -> devices.filter { it.room == r } } ?: devices
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rooms: StateFlow<List<String>> = _allDevices
        .map { devices -> devices.map { it.room }.distinct().sorted() }
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

    fun loadDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getDevices()
                .onSuccess { _allDevices.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun setFilter(filter: DeviceFilter) {
        _selectedFilter.value = filter
        if (filter != DeviceFilter.ROOM) _selectedRoom.value = null
    }

    fun setRoomFilter(room: String) {
        _selectedFilter.value = DeviceFilter.ROOM
        _selectedRoom.value = room
    }

    fun toggleDevice(deviceId: Int, isOn: Boolean) {
        // Optimistic update
        _allDevices.value = _allDevices.value.map {
            if (it.id == deviceId) it.copy(is_active = isOn) else it
        }
        viewModelScope.launch {
            repository.sendCommand(
                CommandRequest(
                    device_id = deviceId,
                    command_type = "power",
                    command_data = if (isOn) "on" else "off"
                )
            ).onFailure {
                // Revert on failure
                _allDevices.value = _allDevices.value.map { device ->
                    if (device.id == deviceId) device.copy(is_active = !isOn) else device
                }
                _error.value = it.message
            }
        }
    }

    fun showAddDialog() { _showAddDialog.value = true }

    fun hideAddDialog() {
        _showAddDialog.value = false
        _addForm.value = AddDeviceFormState()
    }

    fun updateAddForm(state: AddDeviceFormState) { _addForm.value = state }

    fun addDevice() {
        val form = _addForm.value
        if (form.name.isBlank()) { _error.value = "Device name required"; return }
        viewModelScope.launch {
            repository.createDevice(
                DeviceRequest(
                    name = form.name,
                    device_type = form.deviceType,
                    room = form.room.ifBlank { "Default" },
                    mqtt_topic = form.mqttTopic.ifBlank { "smarthome/devices/${form.name.lowercase().replace(" ", "_")}" }
                )
            ).onSuccess {
                hideAddDialog()
                loadDevices()
            }.onFailure { _error.value = it.message }
        }
    }

    fun deleteDevice(id: Int) {
        viewModelScope.launch {
            repository.deleteDevice(id).onSuccess { loadDevices() }
        }
    }

    fun clearError() { _error.value = null }
}
