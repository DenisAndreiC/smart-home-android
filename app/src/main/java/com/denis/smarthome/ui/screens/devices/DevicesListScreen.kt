/**
 * DevicesListScreen.kt - Ecranul de lista a dispozitivelor SmartHome
 *
 * Afiseaza toate dispozitivele cu filtrare dupa tip (All, By Room, IR, Relay).
 * Permite adaugarea unui dispozitiv nou prin dialog cu ExposedDropdownMenuBox
 * pentru selectarea tipului. Suporta pull-to-refresh si stare goala cu call-to-action.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.components.DeviceListItem
import com.denis.smarthome.ui.components.SmartFilterChip
import com.denis.smarthome.ui.navigation.NavRoutes
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.AddDeviceFormState
import com.denis.smarthome.viewmodel.DeviceFilter
import com.denis.smarthome.viewmodel.DevicesViewModel

/**
 * Ecranul principal de gestiune a dispozitivelor — wrapper subtire peste ViewModel.
 *
 * Colecteaza starea din [DevicesViewModel] si o paseaza catre [DevicesListScreenContent],
 * care contine tot UI-ul si nu depinde de ViewModel (poate fi randat direct in @Preview).
 *
 * @param navController Controlerul de navigare Compose
 * @param viewModel ViewModel-ul care gestioneaza lista si filtrele dispozitivelor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesListScreen(
    navController: NavController,
    viewModel: DevicesViewModel = viewModel()
) {
    val filteredDevices by viewModel.filteredDevices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val addForm by viewModel.addForm.collectAsState()

    // Reload devices when this screen becomes active again (e.g. navigating back
    // from TvRemoteScreen after sending a power command) so last_status is fresh.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadDevices()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DevicesListScreenContent(
        filteredDevices = filteredDevices,
        isLoading = isLoading,
        selectedFilter = selectedFilter,
        selectedRoom = selectedRoom,
        rooms = rooms,
        showAddDialog = showAddDialog,
        addForm = addForm,
        onSetFilter = { viewModel.setFilter(it) },
        onSetRoomFilter = { viewModel.setRoomFilter(it) },
        onRefresh = { viewModel.loadDevices() },
        onDeviceClick = { device -> navController.navigate(NavRoutes.DeviceControl.createRoute(device.id)) },
        onDeleteDevice = { viewModel.deleteDevice(it) },
        onShowAddDialog = { viewModel.showAddDialog() },
        onHideAddDialog = { viewModel.hideAddDialog() },
        onAddFormChange = { viewModel.updateAddForm(it) },
        onAddConfirm = { viewModel.addDevice() }
    )
}

/**
 * UI-ul ecranului de gestiune a dispozitivelor, fara dependinta de ViewModel.
 * Primeste toata starea ca parametri simpli si callback-uri lambda, ceea ce permite
 * randare in @Preview fara apeluri de retea.
 *
 * Starea pur locala de UI (dropdown-ul de camere, dispozitivul selectat pentru
 * stergere) ramane gestionata intern prin `remember`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesListScreenContent(
    filteredDevices: List<DeviceResponse>,
    isLoading: Boolean,
    selectedFilter: DeviceFilter,
    selectedRoom: String?,
    rooms: List<String>,
    showAddDialog: Boolean,
    addForm: AddDeviceFormState,
    onSetFilter: (DeviceFilter) -> Unit,
    onSetRoomFilter: (String) -> Unit,
    onRefresh: () -> Unit,
    onDeviceClick: (DeviceResponse) -> Unit,
    onDeleteDevice: (Int) -> Unit,
    onShowAddDialog: () -> Unit,
    onHideAddDialog: () -> Unit,
    onAddFormChange: (AddDeviceFormState) -> Unit,
    onAddConfirm: () -> Unit
) {
    var showRoomDropdown by remember { mutableStateOf(false) }
    var deviceToDelete by remember { mutableStateOf<Int?>(null) }

    // Dialog confirmare stergere dispozitiv
    if (deviceToDelete != null) {
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            containerColor = Surface,
            titleContentColor = OnBackground,
            textContentColor = OnSurface,
            title = { Text("Delete Device", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this device?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDevice(deviceToDelete!!)
                        deviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) {
                    Text("Cancel", color = OnSurface)
                }
            }
        )
    }

    Scaffold(
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background)
        ) {
            // ── Top Bar: titlu, numarul de dispozitive active si buton de adaugare ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Devices",
                            color = OnBackground,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredDevices.size} Devices Active",
                            color = OnSurface,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = onShowAddDialog) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add device",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ── Filtre chip-uri: LazyRow cu SmartFilterChip pentru All, By Room, IR, Relay ──
            // Chipul "By Room" deschide un DropdownMenu cu lista camerelor disponibile
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SmartFilterChip(
                        label = "All",
                        selected = selectedFilter == DeviceFilter.ALL,
                        onClick = { onSetFilter(DeviceFilter.ALL) }
                    )
                }
                item {
                    Box {
                        SmartFilterChip(
                            label = if (selectedFilter == DeviceFilter.ROOM && selectedRoom != null)
                                selectedRoom!! else "By Room",
                            selected = selectedFilter == DeviceFilter.ROOM,
                            onClick = { showRoomDropdown = !showRoomDropdown },
                            trailingContent = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (selectedFilter == DeviceFilter.ROOM) Color.Black else Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = showRoomDropdown,
                            onDismissRequest = { showRoomDropdown = false },
                            modifier = Modifier.background(Surface)
                        ) {
                            rooms.forEach { room ->
                                DropdownMenuItem(
                                    text = {
                                        Text(room, color = OnBackground, style = MaterialTheme.typography.bodyMedium)
                                    },
                                    onClick = {
                                        onSetRoomFilter(room)
                                        showRoomDropdown = false
                                    }
                                )
                            }
                            if (rooms.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No rooms found", color = OnSurface) },
                                    onClick = { showRoomDropdown = false }
                                )
                            }
                        }
                    }
                }
                item {
                    SmartFilterChip(
                        label = "IR Devices",
                        selected = selectedFilter == DeviceFilter.IR,
                        onClick = { onSetFilter(DeviceFilter.IR) }
                    )
                }
                item {
                    SmartFilterChip(
                        label = "Relay",
                        selected = selectedFilter == DeviceFilter.RELAY,
                        onClick = { onSetFilter(DeviceFilter.RELAY) }
                    )
                }
            }

            // ── Lista dispozitive cu PullToRefreshBox ──
            // Afiseaza: indicator de incarcare / stare goala cu CTA / lista cu DeviceListItem
            @OptIn(ExperimentalMaterial3Api::class)
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (filteredDevices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DevicesOther, contentDescription = null, tint = OnSurface, modifier = Modifier.size(64.dp))
                            Text("No devices yet", color = OnSurface, style = MaterialTheme.typography.bodyLarge)
                            Text("Add your first device using the + button", color = OnSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onShowAddDialog) {
                                Text("Add Device", color = Primary)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredDevices, key = { it.id }) { device ->
                            DeviceListItem(
                                device = device,
                                onClick = { onDeviceClick(device) },
                                onDelete = { deviceToDelete = device.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Dialog de adaugare dispozitiv: afisata conditionat din showAddDialog ──
    if (showAddDialog) {
        AddDeviceDialog(
            form = addForm,
            rooms = rooms,
            onFormChange = onAddFormChange,
            onConfirm = onAddConfirm,
            onDismiss = onHideAddDialog
        )
    }
}

/**
 * Dialog pentru adaugarea unui dispozitiv nou.
 *
 * Foloseste [ExposedDropdownMenuBox] din Material3 pentru selectarea tipului
 * de dispozitiv (relay / ir / wol) dintr-o lista predefinita. Campul este
 * read-only si se deschide prin interactiunea cu ExposedDropdownMenuDefaults.TrailingIcon.
 *
 * @param form Starea curenta a formularului de adaugare
 * @param rooms Lista camerelor disponibile pentru placeholder
 * @param onFormChange Callback apelat la orice modificare a formularului
 * @param onConfirm Callback apelat la apasarea butonului de confirmare
 * @param onDismiss Callback apelat la inchiderea dialogului
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceDialog(
    form: AddDeviceFormState,
    rooms: List<String>,
    onFormChange: (AddDeviceFormState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var typeExpanded by remember { mutableStateOf(false) }
    var brandExpanded by remember { mutableStateOf(false) }
    var remoteTypeExpanded by remember { mutableStateOf(false) }
    val deviceTypes = listOf(
        "TV Remote"           to "ir_tv",
        "Air Conditioner"     to "ir_ac",
        "RGB Bulb"            to "ir_rgb",
        "Smart Relay"         to "relay",
        "Wake on LAN"         to "wol"
    )
    val remoteTypes = listOf("22-key", "44-key", "24-key")
    val tvBrands = listOf("Samsung", "LG", "Philips", "Sony", "Panasonic")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = OnBackground,
        textContentColor = OnSurface,
        title = {
            Text(
                "Add Device",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary,
                    unfocusedBorderColor = Outline,
                    unfocusedLabelColor = OnSurface,
                    cursorColor = Primary,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground
                )

                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onFormChange(form.copy(name = it)) },
                    label = { Text("Device Name") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Dropdown Device Type
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = deviceTypes.firstOrNull { it.second == form.deviceType }?.first ?: form.deviceType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Device Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.background(Surface)
                    ) {
                        deviceTypes.forEach { (label, typeValue) ->
                            DropdownMenuItem(
                                text = { Text(label, color = OnBackground) },
                                onClick = {
                                    val newTopic = if (typeValue.startsWith("ir"))
                                        "smarthome/devices/ir/command"
                                    else
                                        "smarthome/devices/relay/command"
                                    onFormChange(form.copy(deviceType = typeValue, mqttTopic = newTopic, brand = ""))
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Dropdown Brand — visible only for ir_tv
                if (form.deviceType == "ir_tv") {
                    ExposedDropdownMenuBox(
                        expanded = brandExpanded,
                        onExpandedChange = { brandExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = form.brand,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Brand") },
                            placeholder = { Text("Select brand") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = brandExpanded,
                            onDismissRequest = { brandExpanded = false },
                            modifier = Modifier.background(Surface)
                        ) {
                            tvBrands.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand, color = OnBackground) },
                                    onClick = {
                                        onFormChange(form.copy(brand = brand))
                                        brandExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Dropdown Remote Type — visible only for ir_rgb
                if (form.deviceType == "ir_rgb") {
                    ExposedDropdownMenuBox(
                        expanded = remoteTypeExpanded,
                        onExpandedChange = { remoteTypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = form.remoteType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Remote Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = remoteTypeExpanded) },
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = remoteTypeExpanded,
                            onDismissRequest = { remoteTypeExpanded = false },
                            modifier = Modifier.background(Surface)
                        ) {
                            remoteTypes.forEach { rt ->
                                DropdownMenuItem(
                                    text = { Text(rt, color = OnBackground) },
                                    onClick = {
                                        onFormChange(form.copy(remoteType = rt))
                                        remoteTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = form.room,
                    onValueChange = { onFormChange(form.copy(room = it)) },
                    label = { Text("Room") },
                    placeholder = { Text(rooms.firstOrNull() ?: "Living Room") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Device", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurface)
            }
        }
    )
}

private val previewDevices = listOf(
    DeviceResponse(
        id = 1,
        name = "Living Room Lamp",
        device_type = "relay",
        room = "Living Room",
        room_id = 1,
        mqtt_topic = "smarthome/devices/relay/command",
        is_online = true,
        last_status = "on",
        mac_address = null,
        ir_codes = null,
        ir_remote_type = null,
        owner_id = 1,
        created_at = "2026-01-01T00:00:00"
    ),
    DeviceResponse(
        id = 2,
        name = "Living Room TV",
        device_type = "ir_tv",
        room = "Living Room",
        room_id = 1,
        mqtt_topic = "smarthome/devices/ir/command",
        is_online = false,
        last_status = "off",
        mac_address = null,
        ir_codes = "samsung",
        ir_remote_type = null,
        owner_id = 1,
        created_at = "2026-01-01T00:00:00"
    ),
    DeviceResponse(
        id = 3,
        name = "Living Room Bulb",
        device_type = "ir_rgb",
        room = "Living Room",
        room_id = 1,
        mqtt_topic = "smarthome/devices/ir/command",
        is_online = true,
        last_status = "on",
        mac_address = null,
        ir_codes = null,
        ir_remote_type = "24-key",
        owner_id = 1,
        created_at = "2026-01-01T00:00:00"
    ),
    DeviceResponse(
        id = 4,
        name = "Bedroom AC",
        device_type = "ir_ac",
        room = "Bedroom",
        room_id = 2,
        mqtt_topic = "smarthome/devices/ir/command",
        is_online = false,
        last_status = "off",
        mac_address = null,
        ir_codes = null,
        ir_remote_type = null,
        owner_id = 1,
        created_at = "2026-01-01T00:00:00"
    ),
    DeviceResponse(
        id = 5,
        name = "Bedroom Socket",
        device_type = "relay",
        room = "Bedroom",
        room_id = 2,
        mqtt_topic = "smarthome/devices/relay/command",
        is_online = true,
        last_status = "on",
        mac_address = null,
        ir_codes = null,
        ir_remote_type = null,
        owner_id = 1,
        created_at = "2026-01-01T00:00:00"
    )
)

@Preview(showBackground = true)
@Composable
fun DevicesListScreenPreview() {
    SmartHomeTheme {
        DevicesListScreenContent(
            filteredDevices = previewDevices,
            isLoading = false,
            selectedFilter = DeviceFilter.ALL,
            selectedRoom = null,
            rooms = listOf("Living Room", "Bedroom"),
            showAddDialog = false,
            addForm = AddDeviceFormState(),
            onSetFilter = {},
            onSetRoomFilter = {},
            onRefresh = {},
            onDeviceClick = {},
            onDeleteDevice = {},
            onShowAddDialog = {},
            onHideAddDialog = {},
            onAddFormChange = {},
            onAddConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DevicesListScreenEmptyPreview() {
    SmartHomeTheme {
        DevicesListScreenContent(
            filteredDevices = emptyList(),
            isLoading = false,
            selectedFilter = DeviceFilter.ALL,
            selectedRoom = null,
            rooms = emptyList(),
            showAddDialog = false,
            addForm = AddDeviceFormState(),
            onSetFilter = {},
            onSetRoomFilter = {},
            onRefresh = {},
            onDeviceClick = {},
            onDeleteDevice = {},
            onShowAddDialog = {},
            onHideAddDialog = {},
            onAddFormChange = {},
            onAddConfirm = {}
        )
    }
}
