package com.denis.smarthome.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.ui.navigation.NavRoutes
import com.denis.smarthome.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.denis.smarthome.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context               = LocalContext.current
    val userName          by viewModel.userName.collectAsState()
    val userEmail         by viewModel.userEmail.collectAsState()
    val isServerConnected by viewModel.isServerConnected.collectAsState()
    val darkTheme         by viewModel.darkTheme.collectAsState()
    val notifications     by viewModel.notifications.collectAsState()
    val lastSyncTime      by viewModel.lastSyncTime.collectAsState()
    val avatarUrl         by viewModel.avatarUrl.collectAsState()
    val isUploading       by viewModel.isUploadingAvatar.collectAsState()
    val isVerified           by viewModel.isVerified.collectAsState()
    val mlMinOccurrences     by viewModel.mlMinOccurrences.collectAsState()
    val mlMinDays            by viewModel.mlMinDays.collectAsState()
    val verificationSent     by viewModel.verificationSent.collectAsState()
    val settingsError        by viewModel.settingsError.collectAsState()

    // Gallery launcher — opens image picker, passes selected URI to the ViewModel
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadAvatar(it) } }

    val baseUrl = RetrofitClient.rootUrl

    val snackbarHostState = remember { SnackbarHostState() }

    var showLogoutDialog      by remember { mutableStateOf(false) }
    var showUrlDialog         by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showTosDialog         by remember { mutableStateOf(false) }
    var newUsernameInput      by remember { mutableStateOf("") }

    // Show error snackbar when settingsError is set
    LaunchedEffect(settingsError) {
        settingsError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSettingsError()
        }
    }

    // Pre-fill display name input when dialog opens
    LaunchedEffect(showEditProfileDialog) {
        if (showEditProfileDialog) newUsernameInput = userName
    }

    // Logout confirmation
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Surface,
            title = { Text("Logout", color = OnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to logout?", color = OnSurface) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    showLogoutDialog = false
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Logout", color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = OnSurface)
                }
            }
        )
    }

    // Edit Profile dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = Surface,
            title = { Text("Edit Profile", color = OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(userEmail.ifBlank { "—" }, color = OnSurface, fontSize = 13.sp)
                    OutlinedTextField(
                        value = newUsernameInput,
                        onValueChange = { newUsernameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary,
                            unfocusedBorderColor = Outline,
                            cursorColor = Primary,
                            focusedTextColor = OnBackground,
                            unfocusedTextColor = OnBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newUsernameInput.isNotBlank()) viewModel.updateUsername(newUsernameInput)
                    showEditProfileDialog = false
                }) { Text("Save", color = Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel", color = OnSurface) }
            }
        )
    }

    // Terms of Service dialog
    if (showTosDialog) {
        AlertDialog(
            onDismissRequest = { showTosDialog = false },
            containerColor = Surface,
            title = { Text("Terms of Service", color = OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "By using SmartHome, you agree to use this application only for lawful purposes. " +
                    "The app communicates with your local smart home server. " +
                    "We do not collect or share your personal data with third parties. " +
                    "This software is provided as-is for educational purposes.",
                    color = OnSurface, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showTosDialog = false }) { Text("Close", color = Primary) }
            }
        )
    }

    // Backend URL dialog
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            containerColor = Surface,
            title = { Text("Backend URL", color = OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Current URL:", color = OnSurface, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(RetrofitClient.BASE_URL, color = Primary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("To change the backend URL, update RetrofitClient.BASE_URL and rebuild the app.", color = OnSurface, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("OK", color = Primary) }
            }
        )
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).padding(innerPadding),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier.fillMaxWidth().background(Surface)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text("Settings", color = OnBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Profile Card ──────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Clickable avatar — tap to open gallery picker
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A3A4A))
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val fullUrl = avatarUrl?.let { baseUrl + it }
                            if (fullUrl != null) {
                                AsyncImage(
                                    model = fullUrl,
                                    contentDescription = "Profile avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default avatar",
                                    modifier = Modifier.size(40.dp),
                                    tint = Primary
                                )
                            }
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Primary,
                                    strokeWidth = 3.dp
                                )
                            }
                            // Camera badge in bottom-right corner
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName.ifBlank { "Your Account" },
                                color = OnBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmail.ifBlank { "Not loaded" },
                                color = OnSurface,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            // Verified / Not verified badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isVerified) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Verified",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable(enabled = !verificationSent) {
                                            viewModel.resendVerification()
                                        }
                                    ) {
                                        if (verificationSent) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                "Verification email sent!",
                                                color = Color(0xFF4CAF50),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = "Not verified",
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                "Not verified — tap to resend email",
                                                color = Color(0xFFFF9800),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Edit Profile", color = Primary, fontSize = 13.sp)
                        }
                        // Navigate to Change Password screen
                        OutlinedButton(
                            onClick = { navController.navigate(NavRoutes.ChangePassword.route) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Change Password", color = Primary, fontSize = 13.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Server Configuration ──────────────────────────────────────────────
        item {
            SettingsSectionTitle("SERVER")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Cloud,
                        label = "Backend URL",
                        value = RetrofitClient.BASE_URL,
                        onClick = { showUrlDialog = true }
                    )
                    HorizontalDivider(color = Outline, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsRow(
                        icon = Icons.Default.Router,
                        label = "MQTT Status",
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape)
                                        .background(if (isServerConnected) Color(0xFF4CAF50) else ErrorColor)
                                )
                                Text(
                                    if (isServerConnected) "Connected" else "Disconnected",
                                    color = if (isServerConnected) Color(0xFF4CAF50) else ErrorColor,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                    HorizontalDivider(color = Outline, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsRow(
                        icon = Icons.Default.Sync,
                        label = "Last Sync",
                        value = lastSyncTime
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Preferences ───────────────────────────────────────────────────────
        item {
            SettingsSectionTitle("PREFERENCES")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        label = "Dark Theme",
                        trailingContent = {
                            Switch(
                                checked = darkTheme,
                                onCheckedChange = { viewModel.toggleDarkTheme() },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Primary, checkedThumbColor = Color.White,
                                    uncheckedTrackColor = SurfaceVariant, uncheckedThumbColor = OnSurface
                                )
                            )
                        }
                    )
                    HorizontalDivider(color = Outline, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        label = "Push Notifications",
                        trailingContent = {
                            Switch(
                                checked = notifications,
                                onCheckedChange = { viewModel.toggleNotifications() },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Primary, checkedThumbColor = Color.White,
                                    uncheckedTrackColor = SurfaceVariant, uncheckedThumbColor = OnSurface
                                )
                            )
                        }
                    )
                    HorizontalDivider(color = Outline, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsRow(
                        icon = Icons.Default.Language,
                        label = "Language",
                        value = "English"
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Smart Recommendations (ML Settings) ───────────────────────────────
        item {
            SettingsSectionTitle("SMART RECOMMENDATIONS")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Minimum pattern occurrences",
                                color = OnBackground,
                                fontSize = 15.sp
                            )
                            Text(
                                "How many times a pattern must repeat before suggesting a routine",
                                color = OnSurface,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            mlMinOccurrences.toString(),
                            color = Primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = mlMinOccurrences.toFloat(),
                        onValueChange = { newVal ->
                            viewModel.updateMLMinOccurrences(newVal.toInt())
                        },
                        valueRange = 3f..20f,
                        steps = 16,
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary,
                            inactiveTrackColor = Outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("3", color = OnSurface, fontSize = 11.sp)
                        Text("20", color = OnSurface, fontSize = 11.sp)
                    }

                    HorizontalDivider(
                        color = Outline,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Second slider: minimum distinct days
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Minimum different days",
                                color = OnBackground,
                                fontSize = 15.sp
                            )
                            Text(
                                "Pattern must repeat on at least this many different days",
                                color = OnSurface,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            mlMinDays.toString(),
                            color = Primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = mlMinDays.toFloat(),
                        onValueChange = { newVal ->
                            viewModel.updateMLMinDays(newVal.toInt())
                        },
                        valueRange = 2f..7f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary,
                            inactiveTrackColor = Outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("2", color = OnSurface, fontSize = 11.sp)
                        Text("7", color = OnSurface, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── App Info ──────────────────────────────────────────────────────────
        item {
            SettingsSectionTitle("ABOUT")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
            ) {
                Column {
                    SettingsRow(icon = Icons.Default.Info, label = "Version", value = "1.0.0")
                    HorizontalDivider(color = Outline, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsRow(
                        icon = Icons.Default.BugReport,
                        label = "Report a Bug",
                        showChevron = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("cucudenis24@stud.ase.ro"))
                                putExtra(Intent.EXTRA_SUBJECT, "[SmartHome Bug Report]")
                                putExtra(Intent.EXTRA_TEXT,
                                    "Device: ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nApp version: 1.0")
                            }
                            context.startActivity(Intent.createChooser(intent, "Send Email"))
                        }
                    )
                    HorizontalDivider(color = Outline, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsRow(icon = Icons.Default.Description,  label = "Terms of Service", showChevron = true, onClick = { showTosDialog = true })
                    HorizontalDivider(color = Outline, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsRow(icon = Icons.Default.PrivacyTip,   label = "Privacy Policy",   showChevron = true, onClick = { showTosDialog = true })
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Logout Button ─────────────────────────────────────────────────────
        item {
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1A1A)),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Logout", color = ErrorColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    } // end LazyColumn
    } // end Scaffold
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = OnSurface,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(PrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = OnBackground, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (trailingContent != null) {
            trailingContent()
        } else {
            if (value != null) {
                Text(
                    value,
                    color = OnSurface,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
            }
            if (showChevron) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurface, modifier = Modifier.size(18.dp))
            }
        }
    }
}
