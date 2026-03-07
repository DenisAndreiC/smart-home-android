package com.denis.smarthome.ui.screens.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.navigation.NavController
import com.denis.smarthome.data.model.SceneResponse
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.ScenesViewModel

@Composable
fun ScenesScreen(
    navController: NavController,
    viewModel: ScenesViewModel = viewModel()
) {
    val scenes by viewModel.scenes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val executeMessage by viewModel.executeMessage.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(executeMessage) {
        if (executeMessage != null) {
            delay(2000)
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Scenes", style = MaterialTheme.typography.headlineSmall, color = OnBackground, fontWeight = FontWeight.Bold)
                        Text("Automate your routines", style = MaterialTheme.typography.bodySmall, color = OnSurface)
                    }
                    IconButton(onClick = { viewModel.loadScenes() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Primary)
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (scenes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OnSurface, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No scenes yet", color = OnSurface, style = MaterialTheme.typography.bodyLarge)
                        Text("Create scenes to automate your home", color = OnSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(scenes, key = { it.id }) { scene ->
                        SceneCard(
                            scene = scene,
                            onExecute = { viewModel.executeScene(scene.id) },
                            onDelete = { viewModel.deleteScene(scene.id) }
                        )
                    }
                }
            }
        }

        // Execute success snackbar
        executeMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = PrimaryContainer,
                contentColor = Primary
            ) {
                Text(msg)
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { /* navigate to scene editor */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Primary,
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add scene")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneCard(
    scene: SceneResponse,
    onExecute: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = scene.icon ?: "🏠",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(scene.name, color = OnBackground, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${scene.actions.size} action${if (scene.actions.size != 1) "s" else ""}",
                    color = OnSurface,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onExecute,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PrimaryContainer)
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Primary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
