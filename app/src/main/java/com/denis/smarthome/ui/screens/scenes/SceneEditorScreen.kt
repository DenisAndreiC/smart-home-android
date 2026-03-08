package com.denis.smarthome.ui.screens.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.SceneActionForm
import com.denis.smarthome.viewmodel.SceneEditorViewModel

private data class IconOption(val name: String, val icon: ImageVector)

private val iconOptions = listOf(
    IconOption("Movie",        Icons.Default.Movie),
    IconOption("WbSunny",      Icons.Default.WbSunny),
    IconOption("DarkMode",     Icons.Default.DarkMode),
    IconOption("DirectionsRun",Icons.Default.DirectionsRun),
    IconOption("MusicNote",    Icons.Default.MusicNote),
    IconOption("Computer",     Icons.Default.Computer),
    IconOption("Restaurant",   Icons.Default.Restaurant),
    IconOption("LocalCafe",    Icons.Default.LocalCafe),
    IconOption("Home",         Icons.Default.Home),
    IconOption("AutoAwesome",  Icons.Default.AutoAwesome)
)

private fun commandOptionsForDevice(device: DeviceResponse): List<Pair<String, String?>> {
    return when (device.device_type.lowercase()) {
        "ir" -> listOf(
            "power" to "on",
            "power" to "off",
            "vol_up" to null,
            "vol_down" to null,
            "ch_up" to null,
            "ch_down" to null,
            "mute" to null
        ).map { (type, data) ->
            val label = when {
                type == "power" && data == "on"  -> "Power On"
                type == "power" && data == "off" -> "Power Off"
                type == "vol_up"   -> "Volume Up"
                type == "vol_down" -> "Volume Down"
                type == "ch_up"    -> "Channel Up"
                type == "ch_down"  -> "Channel Down"
                type == "mute"     -> "Mute"
                else               -> type
            }
            label to "$type${data?.let { "|$it" } ?: ""}"
        }
        else -> listOf(
            "Turn On"  to "power|on",
            "Turn Off" to "power|off"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneEditorScreen(
    navController: NavController,
    sceneId: Int = -1,
    viewModel: SceneEditorViewModel = viewModel()
) {
    val sceneName       by viewModel.sceneName.collectAsState()
    val selectedIcon    by viewModel.selectedIcon.collectAsState()
    val actions         by viewModel.actions.collectAsState()
    val availableDevices by viewModel.availableDevices.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val saveSuccess     by viewModel.saveSuccess.collectAsState()
    val error           by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddActionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) { navController.popBackStack(); viewModel.clearSaveSuccess() }
    }
    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    if (showAddActionDialog) {
        AddActionDialog(
            devices = availableDevices,
            onConfirm = { action -> viewModel.addAction(action); showAddActionDialog = false },
            onDismiss = { showAddActionDialog = false }
        )
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        focusedLabelColor = Primary,
        unfocusedBorderColor = Outline,
        unfocusedLabelColor = OnSurface,
        cursorColor = Primary,
        focusedTextColor = OnBackground,
        unfocusedTextColor = OnBackground
    )

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Top Bar ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text("Cancel", color = OnSurface)
                }
                Text(
                    text = if (sceneId == -1) "New Scene" else "Edit Scene",
                    color = OnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                TextButton(
                    onClick = { viewModel.saveScene() },
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Primary, strokeWidth = 2.dp)
                    } else {
                        Text("Save", color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Scene Name ───────────────────────────────────────────────
                item {
                    OutlinedTextField(
                        value = sceneName,
                        onValueChange = { viewModel.setName(it) },
                        label = { Text("Scene Name") },
                        placeholder = { Text("e.g., Movie Night") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Primary)
                        },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Icon Selector ─────────────────────────────────────────────
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "CHOOSE ICON",
                            color = OnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(iconOptions.size) { index ->
                                val opt = iconOptions[index]
                                val isSelected = selectedIcon == opt.name
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF0D2B35) else SurfaceVariant)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Primary else Outline,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setIcon(opt.name) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        opt.icon,
                                        contentDescription = opt.name,
                                        tint = if (isSelected) Primary else OnSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Actions ───────────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Actions", color = OnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showAddActionDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Action", color = Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (actions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No actions yet. Tap \"Add Action\" to begin.", color = OnSurface, fontSize = 13.sp)
                        }
                    }
                }

                itemsIndexed(actions) { index, action ->
                    ActionRow(
                        action = action,
                        onDelete = { viewModel.removeAction(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(action: SceneActionForm, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Devices, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(action.deviceName, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "${action.commandType}${action.commandData?.let { " → $it" } ?: ""}",
                    color = OnSurface,
                    fontSize = 12.sp
                )
                if (action.delaySeconds > 0) {
                    Text("Delay: ${action.delaySeconds}s", color = OnSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = ErrorColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddActionDialog(
    devices: List<DeviceResponse>,
    onConfirm: (SceneActionForm) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(0) }  // 0=device, 1=action, 2=delay
    var selectedDevice by remember { mutableStateOf<DeviceResponse?>(null) }
    var selectedCommandLabel by remember { mutableStateOf("") }
    var selectedCommandKey by remember { mutableStateOf("") }
    var delayText by remember { mutableStateOf("0") }
    var deviceExpanded by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary, focusedLabelColor = Primary,
        unfocusedBorderColor = Outline, unfocusedLabelColor = OnSurface,
        cursorColor = Primary, focusedTextColor = OnBackground, unfocusedTextColor = OnBackground
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = OnBackground,
        textContentColor = OnSurface,
        title = {
            Text(
                when (step) { 0 -> "Select Device"; 1 -> "Select Action"; else -> "Set Delay" },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (step) {
                    // Step 0 – device picker
                    0 -> {
                        if (devices.isEmpty()) {
                            Text("No devices available. Add devices first.", color = OnSurface)
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = deviceExpanded,
                                onExpandedChange = { deviceExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedDevice?.name ?: "Choose a device...",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Device") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(deviceExpanded) },
                                    colors = fieldColors,
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = deviceExpanded,
                                    onDismissRequest = { deviceExpanded = false },
                                    modifier = Modifier.background(Surface)
                                ) {
                                    devices.forEach { device ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(device.name, color = OnBackground)
                                                    Text("${device.room} • ${device.device_type}", color = OnSurface, fontSize = 12.sp)
                                                }
                                            },
                                            onClick = { selectedDevice = device; deviceExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Step 1 – action picker
                    1 -> {
                        val options = selectedDevice?.let { commandOptionsForDevice(it) } ?: emptyList()
                        ExposedDropdownMenuBox(
                            expanded = actionExpanded,
                            onExpandedChange = { actionExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCommandLabel.ifBlank { "Choose action..." },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Action") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(actionExpanded) },
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = actionExpanded,
                                onDismissRequest = { actionExpanded = false },
                                modifier = Modifier.background(Surface)
                            ) {
                                options.forEach { (label, key) ->
                                    DropdownMenuItem(
                                        text = { Text(label, color = OnBackground) },
                                        onClick = {
                                            selectedCommandLabel = label
                                            selectedCommandKey = key ?: ""
                                            actionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // Step 2 – delay
                    else -> {
                        Text("Optional delay before this action (0–60 seconds):", color = OnSurface, fontSize = 13.sp)
                        OutlinedTextField(
                            value = delayText,
                            onValueChange = { if (it.all(Char::isDigit) && (it.toIntOrNull() ?: 0) <= 60) delayText = it },
                            label = { Text("Delay (seconds)") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (step) {
                        0 -> if (selectedDevice != null) step = 1
                        1 -> if (selectedCommandLabel.isNotBlank()) step = 2
                        else -> {
                            val device = selectedDevice ?: return@Button
                            val parts = selectedCommandKey.split("|")
                            val cmdType = parts.getOrNull(0) ?: "power"
                            val cmdData = parts.getOrNull(1)
                            onConfirm(
                                SceneActionForm(
                                    deviceId = device.id,
                                    deviceName = device.name,
                                    commandType = cmdType,
                                    commandData = cmdData,
                                    delaySeconds = delayText.toIntOrNull() ?: 0
                                )
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (step < 2) "Next" else "Add", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { if (step > 0) step-- else onDismiss() }) {
                Text(if (step > 0) "Back" else "Cancel", color = OnSurface)
            }
        }
    )
}
