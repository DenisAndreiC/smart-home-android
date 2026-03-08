/**
 * LoginScreen.kt - Ecranul de autentificare al aplicatiei SmartHome
 *
 * Afiseaza un formular cu camp email si parola, un buton de login si un link
 * catre ecranul de inregistrare. Gestioneaza starea de incarcare si erorile
 * provenite din AuthViewModel. Dupa autentificare reusita navigheaza automat
 * catre ecranul principal.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.ui.navigation.NavRoutes
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.AuthState
import com.denis.smarthome.viewmodel.AuthViewModel

/**
 * Ecranul de autentificare cu email si parola.
 *
 * Colecteaza [authState] din ViewModel folosind [collectAsState], iar
 * un [LaunchedEffect] asculta schimbarile starii: cand ajunge [AuthState.Success],
 * navigheaza la Home si curata stiva de navigare (popUpTo cu inclusive = true)
 * pentru a preveni intoarcerea la Login cu butonul Back.
 *
 * @param navController Controlerul de navigare Compose
 * @param authViewModel ViewModel-ul care gestioneaza logica de autentificare
 */
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // collectAsState transforma StateFlow din ViewModel intr-o valoare Compose reactiva
    val authState by authViewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading
    val errorMessage = (authState as? AuthState.Error)?.message

    // LaunchedEffect ruleaza un bloc suspend ori de cate ori se schimba authState.
    // Cand login-ul reuseste, navigheaza la Home si elimina Login din stiva.
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate(NavRoutes.Home.route) {
                popUpTo(NavRoutes.Login.route) { inclusive = true }
            }
            authViewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Background, Surface),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header: logo circular cu iconita Home si numele aplicatiei ──
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "SmartHome",
                    tint = Primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "SmartHome",
                style = MaterialTheme.typography.titleLarge,
                color = Primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                color = OnBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Control your home from anywhere",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Card decorativ cu iconita Wifi ──
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Formular: camp email cu tastatura de tip Email ──
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("name@example.com") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary,
                    focusedLeadingIconColor = Primary,
                    unfocusedBorderColor = Outline,
                    unfocusedLabelColor = OnSurface,
                    unfocusedLeadingIconColor = OnSurface,
                    cursorColor = Primary,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Formular: camp parola cu toggle vizibilitate ──
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = OnSurface
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary,
                    focusedLeadingIconColor = Primary,
                    unfocusedBorderColor = Outline,
                    unfocusedLabelColor = OnSurface,
                    unfocusedLeadingIconColor = OnSurface,
                    cursorColor = Primary,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Link pentru recuperare parola (UI only, fara logica) ──
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "Forgot password?",
                    color = Primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Mesaj de eroare animat (vizibil doar cand exista eroare) ──
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = it, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Butonul de Login: dezactivat in timpul incarcarii, afiseaza spinner ──
            Button(
                onClick = { authViewModel.login(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Login",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Link catre ecranul de inregistrare ──
            Row(horizontalArrangement = Arrangement.Center) {
                Text(
                    text = "Don't have an account? ",
                    color = OnSurface,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Register",
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        authViewModel.resetState()
                        navController.navigate(NavRoutes.Register.route)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Separator orizontal cu eticheta SECURE ACCESS ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Outline)
                Text(
                    text = "  SECURE ACCESS  ",
                    color = OnSurface,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Outline)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Iconite biometrice decorative (fingerprint + face ID, UI only) ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint",
                    tint = OnSurface,
                    modifier = Modifier.size(28.dp)
                )
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Face ID",
                    tint = OnSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
