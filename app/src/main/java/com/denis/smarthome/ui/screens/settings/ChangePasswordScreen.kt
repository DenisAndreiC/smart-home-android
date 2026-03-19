package com.denis.smarthome.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.ChangePasswordUiState
import com.denis.smarthome.viewmodel.ChangePasswordViewModel
import com.denis.smarthome.viewmodel.VerificationMethod

@Composable
fun ChangePasswordScreen(
    navController: NavController,
    viewModel: ChangePasswordViewModel = viewModel()
) {
    val uiState  by viewModel.uiState.collectAsState()
    val codeSent by viewModel.codeSent.collectAsState()
    val method   by viewModel.method.collectAsState()

    var currentPassword by remember { mutableStateOf("") }
    var emailCode       by remember { mutableStateOf("") }
    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentVisible  by remember { mutableStateOf(false) }
    var newVisible      by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }

    // Navigate back automatically 2 s after a successful change
    LaunchedEffect(uiState) {
        if (uiState is ChangePasswordUiState.Success) {
            kotlinx.coroutines.delay(2000)
            navController.popBackStack()
            viewModel.resetState()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = Primary,
        focusedLabelColor    = Primary,
        unfocusedBorderColor = Outline,
        cursorColor          = Primary,
        focusedTextColor     = OnBackground,
        unfocusedTextColor   = OnBackground
    )

    Scaffold(containerColor = Background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnSurface
                    )
                }
                Text(
                    text = "Change Password",
                    color = OnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Verification method toggle ────────────────────────────────
                Text(
                    text = "Verification method",
                    color = OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Outline, RoundedCornerShape(12.dp))
                        .height(44.dp)
                ) {
                    MethodTab(
                        label = "Current Password",
                        selected = method == VerificationMethod.PASSWORD,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setMethod(VerificationMethod.PASSWORD) }
                    )
                    MethodTab(
                        label = "Email Code",
                        selected = method == VerificationMethod.EMAIL_CODE,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setMethod(VerificationMethod.EMAIL_CODE) }
                    )
                }

                // ── Verification input (switches based on method) ─────────────
                AnimatedVisibility(
                    visible = method == VerificationMethod.PASSWORD,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { currentVisible = !currentVisible }) {
                                Icon(
                                    if (currentVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = OnSurface
                                )
                            }
                        },
                        visualTransformation = if (currentVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AnimatedVisibility(
                    visible = method == VerificationMethod.EMAIL_CODE,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Send Code button
                        OutlinedButton(
                            onClick = { viewModel.requestCode() },
                            enabled = !codeSent && uiState !is ChangePasswordUiState.Loading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (codeSent) Outline else Primary)
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = if (codeSent) OnSurface else Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (codeSent) "Code sent!" else "Send Code to Email",
                                color = if (codeSent) OnSurface else Primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Confirmation hint
                        AnimatedVisibility(visible = codeSent) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Check your email for the 6-digit code",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // OTP input
                        OutlinedTextField(
                            value = emailCode,
                            onValueChange = { if (it.length <= 6) emailCode = it },
                            label = { Text("Enter 6-digit code") },
                            leadingIcon = { Icon(Icons.Default.Password, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── New password ──────────────────────────────────────────────
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                    trailingIcon = {
                        IconButton(onClick = { newVisible = !newVisible }) {
                            Icon(
                                if (newVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = OnSurface
                            )
                        }
                    },
                    visualTransformation = if (newVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Confirm new password ──────────────────────────────────────
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = OnSurface
                            )
                        }
                    },
                    visualTransformation = if (confirmVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Error message ─────────────────────────────────────────────
                if (uiState is ChangePasswordUiState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = ErrorColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = (uiState as ChangePasswordUiState.Error).message,
                                color = ErrorColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // ── Success message ───────────────────────────────────────────
                if (uiState is ChangePasswordUiState.Success) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A2A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Password changed successfully",
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Submit button ─────────────────────────────────────────────
                Button(
                    onClick = {
                        viewModel.changePassword(
                            currentPassword = currentPassword,
                            emailCode       = emailCode,
                            newPassword     = newPassword,
                            confirmPassword = confirmPassword
                        )
                    },
                    enabled = uiState !is ChangePasswordUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (uiState is ChangePasswordUiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Change Password",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/** Single tab in the verification method toggle row. */
@Composable
private fun MethodTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .background(
                if (selected) PrimaryContainer else Color.Transparent,
                RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Primary else OnSurface,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
