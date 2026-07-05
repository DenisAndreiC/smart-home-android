/**
 * RegisterScreen.kt - Ecranul de inregistrare al aplicatiei SmartHome
 *
 * Afiseaza un formular cu campuri pentru nume, email, parola si confirmare parola.
 * Valideaza ca parolele coincid si ca termenii au fost acceptati inainte de a
 * trimite cererea catre AuthViewModel. Dupa inregistrare reusita navigheaza la Home.
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.ui.navigation.NavRoutes
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.AuthState
import com.denis.smarthome.viewmodel.AuthViewModel

/**
 * Ecranul de creare cont nou — wrapper subtire peste ViewModel.
 *
 * Starea formularului este tinuta local cu [remember]. [collectAsState] transforma
 * StateFlow-ul din ViewModel intr-o valoare reactiva Compose. [LaunchedEffect] cu
 * cheia [authState] detecteaza succesul si navigheaza la Home, curatand Login din stiva.
 * Starea este pasata catre [RegisterScreenContent], care contine tot UI-ul si nu
 * depinde de ViewModel (poate fi randat direct in @Preview).
 *
 * @param navController Controlerul de navigare Compose
 * @param authViewModel ViewModel-ul care gestioneaza logica de autentificare
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }

    // collectAsState transforma StateFlow in valoare reactiva pentru Compose
    val authState by authViewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading
    val errorMessage = (authState as? AuthState.Error)?.message

    // Navigheaza la Home cand inregistrarea reuseste si elimina Login din stiva
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate(NavRoutes.Home.route) {
                popUpTo(NavRoutes.Login.route) { inclusive = true }
            }
            authViewModel.resetState()
        }
    }

    RegisterScreenContent(
        name = name,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        passwordVisible = passwordVisible,
        confirmPasswordVisible = confirmPasswordVisible,
        termsAccepted = termsAccepted,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onNameChange = { name = it },
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onConfirmPasswordChange = { confirmPassword = it },
        onPasswordVisibleToggle = { passwordVisible = !passwordVisible },
        onConfirmPasswordVisibleToggle = { confirmPasswordVisible = !confirmPasswordVisible },
        onTermsAcceptedChange = { termsAccepted = it },
        onRegisterClick = {
            when {
                password != confirmPassword -> authViewModel.resetState().also { /* mismatch handled visually */ }
                !termsAccepted -> { /* show toast - terms not accepted */ }
                else -> authViewModel.register(name, email, password)
            }
        },
        onBack = {
            authViewModel.resetState()
            navController.popBackStack()
        },
        onLoginClick = {
            authViewModel.resetState()
            navController.popBackStack()
        }
    )
}

/**
 * UI-ul ecranului de inregistrare, fara dependinta de ViewModel.
 * Primeste toata starea ca parametri simpli, ceea ce permite randare in @Preview
 * fara apeluri de retea.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreenContent(
    name: String,
    email: String,
    password: String,
    confirmPassword: String,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    termsAccepted: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibleToggle: () -> Unit,
    onConfirmPasswordVisibleToggle: () -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onRegisterClick: () -> Unit,
    onBack: () -> Unit,
    onLoginClick: () -> Unit
) {
    var showLicenseDialog by remember { mutableStateOf(false) }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            containerColor = Surface,
            title = { Text("Terms & Privacy", color = OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This application is a thesis project. Data is stored locally and is not shared with third parties.",
                    color = OnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text("OK", color = Primary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(Background, Surface))
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Create Account", color = OnBackground) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = OnBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Join SmartHome",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign up to start managing your devices and automate your daily routines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary,
                    focusedLeadingIconColor = Primary,
                    unfocusedBorderColor = Outline,
                    unfocusedLabelColor = OnSurface,
                    unfocusedLeadingIconColor = OnSurface,
                    cursorColor = Primary,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                )

                // ── Formular: camp Nume complet ──
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Formular: camp Email ──
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address") },
                    placeholder = { Text("name@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Formular: camp Parola cu toggle vizibilitate ──
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = onPasswordVisibleToggle) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = OnSurface
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Formular: camp confirmare parola cu validare vizuala isError ──
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = onConfirmPasswordVisibleToggle) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = OnSurface
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors,
                    isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                            Text("Passwords do not match", color = ErrorColor)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Checkbox pentru acceptarea Termenilor si Politicii de confidentialitate ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = onTermsAcceptedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Primary,
                            uncheckedColor = Outline,
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "I agree to the ",
                        color = OnSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Terms of Service",
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { showLicenseDialog = true }
                    )
                    Text(
                        text = " and ",
                        color = OnSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Privacy Policy",
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { showLicenseDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Mesaj de eroare animat (apare doar cand exista eroare de la API) ──
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = it, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Butonul de creare cont: enabled doar cand termenii sunt acceptati si parolele coincid ──
                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = !isLoading && termsAccepted && password == confirmPassword
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Create Account", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Link catre ecranul de login pentru utilizatorii cu cont existent ──
                Row(horizontalArrangement = Arrangement.Center) {
                    Text("Already have an account? ", color = OnSurface, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Log In",
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable(onClick = onLoginClick)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    SmartHomeTheme {
        RegisterScreenContent(
            name = "",
            email = "",
            password = "",
            confirmPassword = "",
            passwordVisible = false,
            confirmPasswordVisible = false,
            termsAccepted = false,
            isLoading = false,
            errorMessage = null,
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onPasswordVisibleToggle = {},
            onConfirmPasswordVisibleToggle = {},
            onTermsAcceptedChange = {},
            onRegisterClick = {},
            onBack = {},
            onLoginClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenErrorPreview() {
    SmartHomeTheme {
        RegisterScreenContent(
            name = "Ana Popescu",
            email = "ana@example.com",
            password = "parola123",
            confirmPassword = "parola124",
            passwordVisible = false,
            confirmPasswordVisible = false,
            termsAccepted = true,
            isLoading = false,
            errorMessage = "Email already registered",
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onPasswordVisibleToggle = {},
            onConfirmPasswordVisibleToggle = {},
            onTermsAcceptedChange = {},
            onRegisterClick = {},
            onBack = {},
            onLoginClick = {}
        )
    }
}
