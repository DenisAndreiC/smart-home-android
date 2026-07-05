/**
 * ScenesScreen.kt - Ecran pentru gestionarea scenelor si rutinelor de automatizare
 *
 * Contine doua sectiuni distincte, separate prin tab-uri:
 * - Scene: seturi de actiuni declansate manual de utilizator (buton "Activate").
 * - Rutine: automatizari declansate automat de scheduler-ul backend la o ora si
 *   zile programate, fie create manual, fie sugerate de algoritmul ML (DBSCAN).
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.screens.scenes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.data.model.RoutineCandidate
import com.denis.smarthome.data.model.RoutineResponse
import com.denis.smarthome.data.model.SceneAction
import com.denis.smarthome.data.model.SceneResponse
import com.denis.smarthome.ui.navigation.NavRoutes
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.ScenesViewModel

/**
 * Date holder pentru iconita, culoarea iconitei si culoarea de fundal ale unei scene.
 * Utilizat intern de functia sceneVisuals() pentru a determina aspectul cardului.
 */
private data class SceneVisuals(val icon: ImageVector, val iconTint: Color, val bgColor: Color)

/**
 * Returneaza un obiect SceneVisuals corespunzator numelui scenei.
 * Detecteaza cuvinte cheie (movie, morning, sleep etc.) si atribuie
 * iconita si culorile potrivite contextului scenei.
 *
 * @param name numele scenei, folosit pentru detectia cuvintelor cheie
 * @param iconStr campul icon optional din raspunsul API (neutilizat momentan)
 */
private fun sceneVisuals(name: String, iconStr: String?): SceneVisuals {
    val n = name.lowercase()
    return when {
        n.contains("movie") || n.contains("cinema") ->
            SceneVisuals(Icons.Default.Movie,         Color(0xFF00BCD4), Color(0xFF1A2A3A))
        n.contains("morning") || n.contains("wake") || n.contains("good morning") ->
            SceneVisuals(Icons.Default.WbSunny,       Color(0xFFFFD700), Color(0xFF2A2A1A))
        n.contains("away")  ->
            SceneVisuals(Icons.Default.DirectionsRun, Color(0xFF00BCD4), Color(0xFF1A2A2A))
        n.contains("party") ->
            SceneVisuals(Icons.Default.MusicNote,     Color(0xFFE040FB), Color(0xFF2A1A2A))
        n.contains("sleep") || n.contains("night") || n.contains("bed") ->
            SceneVisuals(Icons.Default.DarkMode,      Color(0xFF7986CB), Color(0xFF1A1A2A))
        n.contains("work")  || n.contains("office") ->
            SceneVisuals(Icons.Default.Computer,      Color(0xFF66BB6A), Color(0xFF1A2A1A))
        else ->
            SceneVisuals(Icons.Default.AutoAwesome,   Primary,           Color(0xFF1A2A35))
    }
}

/**
 * Formateaza sirul "days_of_week" (ex: "1,2,3,4,5") intr-un text lizibil.
 * Recunoaste tiparele comune "Every day" si "Weekdays"/"Weekend", altfel
 * listeaza zilele abreviate in ordine.
 */
private fun formatDaysOfWeek(daysOfWeek: String): String {
    val dayLabels = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    val days = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    return when (days) {
        setOf(1, 2, 3, 4, 5, 6, 7) -> "Every day"
        setOf(1, 2, 3, 4, 5) -> "Weekdays"
        setOf(6, 7) -> "Weekend"
        else -> days.sorted().mapNotNull { dayLabels[it] }.joinToString(", ")
    }
}

/**
 * Ecranul principal al sectiunii Scene & Rutine — wrapper subtire peste ViewModel.
 * Citeste starea din [ScenesViewModel] si o paseaza catre [ScenesScreenContent], care
 * contine tot UI-ul si nu depinde de ViewModel (poate fi randat direct in @Preview).
 *
 * @param navController controllerul de navigare Compose
 * @param viewModel ScenesViewModel furnizat prin injectare Compose
 */
@Composable
fun ScenesScreen(
    navController: NavController,
    viewModel: ScenesViewModel = viewModel()
) {
    val scenes          by viewModel.scenes.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val executingId     by viewModel.executingSceneId.collectAsState()

    val routines            by viewModel.routines.collectAsState()
    val devices             by viewModel.devices.collectAsState()
    val isLoadingRoutines   by viewModel.isLoadingRoutines.collectAsState()
    val mlMessage           by viewModel.mlMessage.collectAsState()
    val mlCandidates        by viewModel.mlCandidates.collectAsState()
    val showMlCandidatesDialog by viewModel.showMlCandidatesDialog.collectAsState()

    ScenesScreenContent(
        scenes = scenes,
        isLoadingScenes = isLoading,
        executingSceneId = executingId,
        routines = routines,
        devices = devices,
        isLoadingRoutines = isLoadingRoutines,
        mlMessage = mlMessage,
        mlCandidates = mlCandidates,
        showMlCandidatesDialog = showMlCandidatesDialog,
        onRefreshScenes = { viewModel.loadScenes() },
        onRefreshRoutines = { viewModel.loadRoutines() },
        onExecuteScene = { viewModel.executeScene(it) },
        onDeleteScene = { viewModel.deleteScene(it) },
        onEditScene = { navController.navigate(NavRoutes.SceneEditor.createRoute(it)) },
        onCreateScene = { navController.navigate(NavRoutes.SceneEditor.createRoute()) },
        onToggleRoutine = { id, active -> viewModel.toggleRoutine(id, active) },
        onDeleteRoutine = { viewModel.deleteRoutine(it) },
        onGenerateMlRoutines = { viewModel.generateMlRoutines() },
        onCreateRoutine = { name, deviceId, action, value, triggerTime, daysOfWeek ->
            viewModel.createRoutine(name, deviceId, action, value, triggerTime, daysOfWeek)
        },
        onConfirmMlCandidates = { viewModel.createSelectedRoutineCandidates(it) },
        onDismissMlCandidatesDialog = { viewModel.dismissMlCandidatesDialog() },
        onClearMlMessage = { viewModel.clearMlMessage() }
    )
}

/**
 * UI-ul ecranului Scene & Rutine, fara dependinta de ViewModel.
 * Primeste toata starea ca parametri simpli, ceea ce permite randare in @Preview
 * fara apeluri de retea.
 *
 * Foloseste un TabRow pentru a separa clar cele doua concepte:
 * - tab 0: Scene (grila existenta, executie manuala)
 * - tab 1: Rutine (lista de automatizari cu toggle activ/inactiv)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenesScreenContent(
    scenes: List<SceneResponse>,
    isLoadingScenes: Boolean,
    executingSceneId: Int?,
    routines: List<RoutineResponse>,
    devices: List<DeviceResponse>,
    isLoadingRoutines: Boolean,
    mlMessage: String?,
    mlCandidates: List<RoutineCandidate>,
    showMlCandidatesDialog: Boolean,
    onRefreshScenes: () -> Unit,
    onRefreshRoutines: () -> Unit,
    onExecuteScene: (Int) -> Unit,
    onDeleteScene: (Int) -> Unit,
    onEditScene: (Int) -> Unit,
    onCreateScene: () -> Unit,
    onToggleRoutine: (Int, Boolean) -> Unit,
    onDeleteRoutine: (Int) -> Unit,
    onGenerateMlRoutines: () -> Unit,
    onCreateRoutine: (name: String, deviceId: Int, action: String, value: String?, triggerTime: String, daysOfWeek: String) -> Unit,
    onConfirmMlCandidates: (List<RoutineCandidate>) -> Unit,
    onDismissMlCandidatesDialog: () -> Unit,
    onClearMlMessage: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    // sceneToDelete retine scena selectata pentru confirmare inainte de stergere
    var sceneToDelete by remember { mutableStateOf<SceneResponse?>(null) }
    var routineToDelete by remember { mutableStateOf<RoutineResponse?>(null) }
    var showCreateRoutineDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mlMessage) {
        mlMessage?.let { snackbarHostState.showSnackbar(it); onClearMlMessage() }
    }

    // Dialog de confirmare stergere scena — apare doar cand sceneToDelete != null
    sceneToDelete?.let { scene ->
        AlertDialog(
            onDismissRequest = { sceneToDelete = null },
            containerColor = Surface,
            title = { Text("Delete Scene", color = OnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("Delete \"${scene.name}\"? This cannot be undone.", color = OnSurface) },
            confirmButton = {
                TextButton(onClick = { onDeleteScene(scene.id); sceneToDelete = null }) {
                    Text("Delete", color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sceneToDelete = null }) { Text("Cancel", color = OnSurface) }
            }
        )
    }

    // Dialog de confirmare stergere rutina
    routineToDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            containerColor = Surface,
            title = { Text("Delete Routine", color = OnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("Delete \"${routine.name}\"? This cannot be undone.", color = OnSurface) },
            confirmButton = {
                TextButton(onClick = { onDeleteRoutine(routine.id); routineToDelete = null }) {
                    Text("Delete", color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) { Text("Cancel", color = OnSurface) }
            }
        )
    }

    if (showCreateRoutineDialog) {
        CreateRoutineDialog(
            devices = devices,
            onConfirm = { name, deviceId, action, value, triggerTime, daysOfWeek ->
                onCreateRoutine(name, deviceId, action, value, triggerTime, daysOfWeek)
                showCreateRoutineDialog = false
            },
            onDismiss = { showCreateRoutineDialog = false }
        )
    }

    // Dialog de selectie a candidatilor ML — GET /routines/detect nu salveaza nimic,
    // utilizatorul bifeaza care sugestii sa devina rutine reale
    if (showMlCandidatesDialog) {
        RoutineCandidatesDialog(
            candidates = mlCandidates,
            onConfirm = { selected -> onConfirmMlCandidates(selected) },
            onDismiss = onDismissMlCandidatesDialog
        )
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // FAB context-sensibil: creeaza o scena noua pe tab-ul Scenes, o rutina manuala pe tab-ul Routines
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) onCreateScene()
                    else showCreateRoutineDialog = true
                },
                containerColor = Primary,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "New Scene" else "New Routine")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        "Scenes & Routines",
                        color = OnBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (selectedTab == 0) "Automate your home with one tap"
                        else "Automations that run on their own schedule",
                        color = OnSurface,
                        fontSize = 13.sp
                    )
                }
                IconButton(
                    onClick = { if (selectedTab == 0) onRefreshScenes() else onRefreshRoutines() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Primary)
                }
            }

            // ── Tabs: Scene / Rutine ─────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = Primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Scenes", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = Primary,
                    unselectedContentColor = OnSurface
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Routines", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = Primary,
                    unselectedContentColor = OnSurface
                )
            }

            when (selectedTab) {
                0 -> ScenesTab(
                    scenes = scenes,
                    isLoading = isLoadingScenes,
                    executingId = executingSceneId,
                    onExecute = onExecuteScene,
                    onDelete = { sceneToDelete = it },
                    onEdit = onEditScene,
                    onCreate = onCreateScene
                )
                1 -> RoutinesTab(
                    routines = routines,
                    devices = devices,
                    isLoading = isLoadingRoutines,
                    onToggle = onToggleRoutine,
                    onDelete = { routineToDelete = it },
                    onGenerateMl = onGenerateMlRoutines,
                    onCreateManual = { showCreateRoutineDialog = true }
                )
            }
        }
    }
}

/**
 * Continutul tab-ului "Scenes": grila existenta de carduri cu executie manuala.
 */
@Composable
private fun ScenesTab(
    scenes: List<SceneResponse>,
    isLoading: Boolean,
    executingId: Int?,
    onExecute: (Int) -> Unit,
    onDelete: (SceneResponse) -> Unit,
    onEdit: (Int) -> Unit,
    onCreate: () -> Unit
) {
    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        scenes.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = OnSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text("No scenes yet", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Create your first automation", color = OnSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                    Button(
                        onClick = onCreate,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create Scene", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        else -> {
            // Grila 2 coloane cu carduri de scene
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(scenes, key = { it.id }) { scene ->
                    SceneCard(
                        scene = scene,
                        isExecuting = executingId == scene.id,
                        onExecute = { onExecute(scene.id) },
                        onDelete = { onDelete(scene) },
                        onEdit = { onEdit(scene.id) }
                    )
                }
            }
        }
    }
}

/**
 * Continutul tab-ului "Routines": text explicativ, actiuni de generare/creare
 * si lista de rutine cu toggle activ/inactiv.
 */
@Composable
private fun RoutinesTab(
    routines: List<RoutineResponse>,
    devices: List<DeviceResponse>,
    isLoading: Boolean,
    onToggle: (Int, Boolean) -> Unit,
    onDelete: (RoutineResponse) -> Unit,
    onGenerateMl: () -> Unit,
    onCreateManual: () -> Unit
) {
    val deviceNames = remember(devices) { devices.associate { it.id to it.name } }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Routines run automatically at a scheduled time and days — created manually " +
                    "or picked from ML suggestions based on your usage patterns. Routines are " +
                    "active as soon as they're created; use the switch below to pause one.",
                color = OnSurface,
                fontSize = 12.sp
            )
            Button(
                onClick = onGenerateMl,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate Routine (ML)", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            routines.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = OnSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text("No routines yet", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Create one manually or generate from ML", color = OnSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                        Button(
                            onClick = onCreateManual,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Routine", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(routines, key = { it.id }) { routine ->
                        RoutineCard(
                            routine = routine,
                            deviceName = deviceNames[routine.device_id] ?: "Unknown device",
                            onToggle = { active -> onToggle(routine.id, active) },
                            onDelete = { onDelete(routine) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card compozabil pentru o singura rutina.
 * Afiseaza numele, dispozitivul tinta, ora si zilele de declansare, si un switch
 * activ/inactiv legat direct de PUT /routines/{id}/toggle prin callback-ul onToggle.
 */
@Composable
private fun RoutineCard(
    routine: RoutineResponse,
    deviceName: String,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (routine.is_ml_suggested) Color(0xFF2A1A2A) else Color(0xFF1A2A35)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (routine.is_ml_suggested) Icons.Default.AutoAwesome else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (routine.is_ml_suggested) Color(0xFFE040FB) else Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(routine.name, color = OnBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (routine.is_ml_suggested) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2A1A2A))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text("ML", color = Color(0xFFE040FB), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    "$deviceName • ${routine.action}${routine.value?.let { " $it" } ?: ""}",
                    color = OnSurface,
                    fontSize = 12.sp
                )
                Text(
                    "${routine.trigger_time} • ${formatDaysOfWeek(routine.days_of_week)}",
                    color = OnSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorColor, modifier = Modifier.size(18.dp))
            }
            Switch(
                checked = routine.is_active,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Primary,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = SurfaceVariant,
                    uncheckedThumbColor = OnSurface
                )
            )
        }
    }
}

/**
 * Dialog pentru crearea manuala a unei rutine.
 * Restrans la actiunea power on/off pentru simplitate (cel mai comun caz de utilizare
 * pentru automatizari programate); ora este validata cu regex HH:MM (24h) conform
 * schemei RoutineCreate de pe backend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRoutineDialog(
    devices: List<DeviceResponse>,
    onConfirm: (name: String, deviceId: Int, action: String, value: String?, triggerTime: String, daysOfWeek: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedDevice by remember { mutableStateOf<DeviceResponse?>(devices.firstOrNull()) }
    var deviceExpanded by remember { mutableStateOf(false) }
    var isOnAction by remember { mutableStateOf(true) }
    var triggerTime by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }

    val timeRegex = remember { Regex("^([01]\\d|2[0-3]):[0-5]\\d$") }
    val isTimeValid = timeRegex.matches(triggerTime)
    val canSave = name.isNotBlank() && selectedDevice != null && isTimeValid && selectedDays.isNotEmpty()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        focusedLabelColor = Primary,
        unfocusedBorderColor = Outline,
        unfocusedLabelColor = OnSurface,
        cursorColor = Primary,
        focusedTextColor = OnBackground,
        unfocusedTextColor = OnBackground
    )

    val dayOptions = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = OnBackground,
        textContentColor = OnSurface,
        title = { Text("New Routine", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine Name") },
                    placeholder = { Text("e.g. Turn off lamp at night") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (devices.isEmpty()) {
                    Text("No devices available. Add a device first.", color = OnSurface)
                } else {
                    ExposedDropdownMenuBox(expanded = deviceExpanded, onExpandedChange = { deviceExpanded = it }) {
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
                                    text = { Text(device.name, color = OnBackground) },
                                    onClick = { selectedDevice = device; deviceExpanded = false }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Action", color = OnSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isOnAction) "Turn ON" else "Turn OFF", color = Primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isOnAction,
                            onCheckedChange = { isOnAction = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Primary,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = SurfaceVariant,
                                uncheckedThumbColor = OnSurface
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = triggerTime,
                    onValueChange = { if (it.length <= 5) triggerTime = it },
                    label = { Text("Trigger Time (24h, e.g. 07:30)") },
                    isError = triggerTime.isNotEmpty() && !isTimeValid,
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Repeat on", color = OnSurface, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayOptions.forEach { (day, label) ->
                        val isSelected = day in selectedDays
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryContainer else SurfaceVariant)
                                .border(1.dp, if (isSelected) Primary else Outline, CircleShape)
                                .clickable {
                                    selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label.take(2),
                                color = if (isSelected) Primary else OnSurface,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val device = selectedDevice ?: return@Button
                    onConfirm(
                        name,
                        device.id,
                        "power",
                        if (isOnAction) "on" else "off",
                        triggerTime,
                        selectedDays.sorted().joinToString(",")
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurface) }
        }
    )
}

/**
 * Dialog de selectie a candidatilor de rutina detectati de ML.
 *
 * GET /routines/detect este read-only si returneaza doar sugestii (fara sa salveze
 * nimic). Utilizatorul bifeaza checkbox-urile pentru candidatii doriti, iar la
 * "Create Selected" se trimite cate un POST /routines/ pentru fiecare bifat.
 * Toti candidatii sunt bifati implicit pentru a pastra fluxul rapid, dar userul
 * poate debifa oricare inainte de a confirma.
 */
@Composable
private fun RoutineCandidatesDialog(
    candidates: List<RoutineCandidate>,
    onConfirm: (List<RoutineCandidate>) -> Unit,
    onDismiss: () -> Unit
) {
    // Cheia e pozitia in lista, nu candidate.candidate_index: acel camp e nullable si
    // absent din /ml/recommendations (vezi Models.kt), deci nu e sigur de folosit ca
    // identificator unic daca acest dialog ar primi vreodata date din acel endpoint.
    val selected = remember(candidates) {
        mutableStateMapOf<Int, Boolean>().apply {
            candidates.indices.forEach { put(it, true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = OnBackground,
        textContentColor = OnSurface,
        title = { Text("Select Routines to Create", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "ML found these repeating patterns in your command history. " +
                        "Pick which ones to save as routines.",
                    color = OnSurface,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                candidates.forEachIndexed { index, candidate ->
                    val isChecked = selected[index] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selected[index] = !isChecked }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { selected[index] = it },
                            colors = CheckboxDefaults.colors(checkedColor = Primary, uncheckedColor = Outline)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(candidate.name, color = OnBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "${candidate.trigger_time} • ${(candidate.confidence * 100).toInt()}% confidence",
                                color = OnSurface,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(candidates.filterIndexed { index, _ -> selected[index] == true })
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Selected", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurface) }
        }
    )
}

/**
 * Card compozabil pentru o singura scena din grila.
 * Afiseaza iconita, numele, numarul de actiuni si un buton de activare.
 * Include un meniu contextual (MoreVert) cu optiunile Edit si Delete.
 *
 * @param scene datele scenei (id, name, actions, icon)
 * @param isExecuting true daca scena este in curs de executie (schimba aspectul butonului)
 * @param onExecute callback apelat la apasarea butonului Activate
 * @param onDelete callback apelat la selectarea optiunii Delete din meniu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneCard(
    scene: SceneResponse,
    isExecuting: Boolean,
    onExecute: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    // Determina iconita si culorile cardului pe baza numelui scenei
    val visuals = sceneVisuals(scene.name, scene.icon)
    // Controleaza vizibilitatea meniului dropdown (MoreVert)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Icon + overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(visuals.bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visuals.icon,
                        contentDescription = null,
                        tint = visuals.iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = OnSurface, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit", color = OnBackground) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = ErrorColor) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = scene.name,
                color = OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "${scene.actions.size} device${if (scene.actions.size != 1) "s" else ""}",
                color = OnSurface,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Buton de activare: arata "Active" cu icon check daca isExecuting = true
            AnimatedVisibility(visible = true) {
                Button(
                    onClick = { if (!isExecuting) onExecute() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExecuting) PrimaryContainer else Primary
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (isExecuting) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Active", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Activate", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Preview fake data ────────────────────────────────────────────────────────

private val previewScenesDevices = listOf(
    DeviceResponse(
        id = 1, name = "Living Room Lamp", device_type = "smart_plug", room = "Living Room",
        room_id = 1, mqtt_topic = null, is_online = true, last_status = "on", mac_address = null,
        ir_codes = null, ir_remote_type = null, owner_id = 1, created_at = "2026-01-01T00:00:00"
    ),
    DeviceResponse(
        id = 2, name = "Bedroom AC", device_type = "ir_ac", room = "Bedroom",
        room_id = 2, mqtt_topic = null, is_online = true, last_status = "off", mac_address = null,
        ir_codes = null, ir_remote_type = "44-key", owner_id = 1, created_at = "2026-01-01T00:00:00"
    )
)

private val previewScenes = listOf(
    SceneResponse(
        id = 1,
        name = "Good Morning",
        icon = null,
        actions = listOf(
            SceneAction(device_id = 1, command_type = "power", command_data = "on", delay_seconds = 0),
            SceneAction(device_id = 2, command_type = "power", command_data = "off", delay_seconds = 5)
        ),
        is_active = true
    ),
    SceneResponse(
        id = 2,
        name = "Movie Night",
        icon = null,
        actions = listOf(
            SceneAction(device_id = 1, command_type = "power", command_data = "off", delay_seconds = 0)
        ),
        is_active = true
    ),
    SceneResponse(
        id = 3,
        name = "Away Mode",
        icon = null,
        actions = listOf(
            SceneAction(device_id = 1, command_type = "power", command_data = "off", delay_seconds = 0),
            SceneAction(device_id = 2, command_type = "power", command_data = "off", delay_seconds = 0)
        ),
        is_active = true
    )
)

private val previewRoutines = listOf(
    RoutineResponse(
        id = 1,
        user_id = 1,
        name = "Turn on lamp at sunset",
        device_id = 1,
        action = "power",
        value = "on",
        trigger_time = "19:30",
        days_of_week = "1,2,3,4,5,6,7",
        is_active = true,
        is_ml_suggested = false,
        confidence = null,
        created_at = "2026-01-01T00:00:00"
    ),
    RoutineResponse(
        id = 2,
        user_id = 1,
        name = "Weekday AC off",
        device_id = 2,
        action = "power",
        value = "off",
        trigger_time = "08:00",
        days_of_week = "1,2,3,4,5",
        is_active = false,
        is_ml_suggested = true,
        confidence = 0.82f,
        created_at = "2026-01-01T00:00:00"
    )
)

private val previewMlCandidates = listOf(
    RoutineCandidate(
        device_id = 1,
        device_name = "Living Room Lamp",
        action = "power",
        value = "on",
        trigger_time = "07:00",
        days_of_week = "1,2,3,4,5",
        occurrences = 12,
        distinct_days = 5,
        confidence = 0.91f,
        name = "Turn on lamp weekday mornings",
        candidate_index = 0
    ),
    RoutineCandidate(
        device_id = 2,
        device_name = "Bedroom AC",
        action = "power",
        value = "off",
        trigger_time = "23:00",
        days_of_week = "1,2,3,4,5,6,7",
        occurrences = 20,
        distinct_days = 7,
        confidence = 0.76f,
        name = "Turn off AC at night",
        candidate_index = 1
    )
)

@Preview(showBackground = true)
@Composable
fun ScenesScreenScenesTabPreview() {
    SmartHomeTheme {
        ScenesScreenContent(
            scenes = previewScenes,
            isLoadingScenes = false,
            executingSceneId = null,
            routines = previewRoutines,
            devices = previewScenesDevices,
            isLoadingRoutines = false,
            mlMessage = null,
            mlCandidates = emptyList(),
            showMlCandidatesDialog = false,
            onRefreshScenes = {},
            onRefreshRoutines = {},
            onExecuteScene = {},
            onDeleteScene = {},
            onEditScene = {},
            onCreateScene = {},
            onToggleRoutine = { _, _ -> },
            onDeleteRoutine = {},
            onGenerateMlRoutines = {},
            onCreateRoutine = { _, _, _, _, _, _ -> },
            onConfirmMlCandidates = {},
            onDismissMlCandidatesDialog = {},
            onClearMlMessage = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScenesScreenRoutinesTabPreview() {
    SmartHomeTheme {
        // Content porneste implicit pe tab-ul "Scenes"; RoutinesTab e afisat direct
        // pentru a previzualiza cardurile de rutine fara interactiune manuala cu TabRow.
        RoutinesTab(
            routines = previewRoutines,
            devices = previewScenesDevices,
            isLoading = false,
            onToggle = { _, _ -> },
            onDelete = {},
            onGenerateMl = {},
            onCreateManual = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RoutineCandidatesDialogPreview() {
    SmartHomeTheme {
        RoutineCandidatesDialog(
            candidates = previewMlCandidates,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
