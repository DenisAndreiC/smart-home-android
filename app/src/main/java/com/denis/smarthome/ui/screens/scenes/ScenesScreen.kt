/**
 * ScenesScreen.kt - Ecran pentru gestionarea scenelor de automatizare smart home
 *
 * Afiseaza o grila de scene disponibile, fiecare putand fi executata sau stearsa.
 * Include logica de vizualizare dinamica (icon si culori) in functie de numele scenei.
 * Navigheaza catre SceneEditorScreen pentru crearea de scene noi.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.screens.scenes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
 * Ecranul principal al sectiunii Scene.
 * Afiseaza o grila de carduri cu scene, permite executarea si stergerea lor.
 * Un buton flotant (FAB) navigheaza catre editorul de scene pentru creare noua.
 *
 * @param navController controllerul de navigare Compose
 * @param viewModel ScenesViewModel furnizat prin injectare Compose
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenesScreen(
    navController: NavController,
    viewModel: ScenesViewModel = viewModel()
) {
    val scenes          by viewModel.scenes.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val executingId     by viewModel.executingSceneId.collectAsState()

    // sceneToDelete retine scena selectata pentru confirmare inainte de stergere
    var sceneToDelete by remember { mutableStateOf<SceneResponse?>(null) }

    // Dialog de confirmare stergere scena — apare doar cand sceneToDelete != null
    sceneToDelete?.let { scene ->
        AlertDialog(
            onDismissRequest = { sceneToDelete = null },
            containerColor = Surface,
            title = { Text("Delete Scene", color = OnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("Delete \"${scene.name}\"? This cannot be undone.", color = OnSurface) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteScene(scene.id); sceneToDelete = null }) {
                    Text("Delete", color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sceneToDelete = null }) { Text("Cancel", color = OnSurface) }
            }
        )
    }

    Scaffold(
        containerColor = Background,
        // FAB pentru navigare catre editorul de scene (creare noua)
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(NavRoutes.SceneEditor.createRoute()) },
                containerColor = Primary,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Scene")
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
                        "Scenes",
                        color = OnBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Automate your home with one tap",
                        color = OnSurface,
                        fontSize = 14.sp
                    )
                }
                IconButton(
                    onClick = { viewModel.loadScenes() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Primary)
                }
            }

            // ── Continut principal: loading / lista goala / grila de scene ────
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
                                onClick = { navController.navigate(NavRoutes.SceneEditor.createRoute()) },
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
                                onExecute = { viewModel.executeScene(scene.id) },
                                onDelete = { sceneToDelete = scene },
                                onEdit = { navController.navigate(NavRoutes.SceneEditor.createRoute(scene.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
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
