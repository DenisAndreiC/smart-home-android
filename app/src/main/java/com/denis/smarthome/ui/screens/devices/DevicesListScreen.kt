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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.ui.components.DeviceListItem
import com.denis.smarthome.ui.components.SmartFilterChip
import com.denis.smarthome.ui.navigation.NavRoutes
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.AddDeviceFormState
import com.denis.smarthome.viewmodel.DeviceFilter
import com.denis.smarthome.viewmodel.DevicesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesListScreen(
    navController: NavController,
    viewModel: DevicesViewModel = viewModel()
) {
    val filteredDevices by viewModel.filteredDevices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val addForm by viewModel.addForm.collectAsState()

    var showRoomDropdown by remember { mutableStateOf(false) }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background)
        ) {
            // ── Top Bar ──────────────────────────────────────────────────────
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
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = OnSurface)
                    }
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
                    IconButton(onClick = { viewModel.showAddDialog() }) {
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

            // ── Filter Chips ─────────────────────────────────────────────────
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SmartFilterChip(
                        label = "All",
                        selected = selectedFilter == DeviceFilter.ALL,
                        onClick = { viewModel.setFilter(DeviceFilter.ALL) }
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
                                        viewModel.setRoomFilter(room)
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
                        onClick = { viewModel.setFilter(DeviceFilter.IR) }
                    )
                }
                item {
                    SmartFilterChip(
                        label = "Relay",
                        selected = selectedFilter == DeviceFilter.RELAY,
                        onClick = { viewModel.setFilter(DeviceFilter.RELAY) }
                    )
                }
            }

            // ── Device List ──────────────────────────────────────────────────
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
                        Text("No devices found", color = OnSurface, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { viewModel.loadDevices() }) {
                            Text("Retry", color = Primary)
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
                            onToggle = { isOn -> viewModel.toggleDevice(device.id, isOn) },
                            onClick = {
                                navController.navigate(NavRoutes.DeviceControl.createRoute(device.id))
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Add Device Dialog ────────────────────────────────────────────────────
    if (showAddDialog) {
        AddDeviceDialog(
            form = addForm,
            rooms = rooms,
            onFormChange = viewModel::updateAddForm,
            onConfirm = { viewModel.addDevice() },
            onDismiss = { viewModel.hideAddDialog() }
        )
    }
}

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
    val deviceTypes = listOf("relay", "ir", "wol")

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

                // Device Type dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = form.deviceType,
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
                        deviceTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.uppercase(), color = OnBackground) },
                                onClick = {
                                    onFormChange(form.copy(deviceType = type))
                                    typeExpanded = false
                                }
                            )
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

                OutlinedTextField(
                    value = form.mqttTopic,
                    onValueChange = { onFormChange(form.copy(mqttTopic = it)) },
                    label = { Text("MQTT Topic") },
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
