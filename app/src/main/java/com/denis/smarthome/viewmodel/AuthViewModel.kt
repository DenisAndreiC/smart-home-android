/**
 * AuthViewModel.kt - ViewModel pentru autentificare si inregistrare
 *
 * Gestioneaza starea de autentificare a utilizatorului (login si register).
 * Comunica cu AuthRepository pentru apeluri API si salveaza token-ul JWT in DataStore.
 * Expune un StateFlow<AuthState> pe care UI-ul il observa pentru a reactiona la schimbari.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.local.TokenManager
import com.denis.smarthome.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class ce reprezinta toate starile posibile ale procesului de autentificare.
 *
 * - [Idle]: starea initiala, nicio operatie in curs
 * - [Loading]: cerere API in desfasurare, UI-ul afiseaza un indicator de incarcare
 * - [Success]: autentificare reusita; contine token-ul JWT returnat de server
 * - [Error]: autentificare esuata; contine mesajul de eroare afisat utilizatorului
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel pentru ecranele de Login si Register.
 *
 * Foloseste [AuthRepository] pentru a trimite cereri catre API-ul FastAPI.
 * Login-ul foloseste OAuth2 form-encoded (username/password ca form fields, nu JSON).
 * Dupa autentificare reusita, token-ul este salvat in DataStore prin [TokenManager].
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    val tokenManager = TokenManager(application)
    private val repository = AuthRepository(RetrofitClient.apiService, tokenManager)

    // Starea curenta a autentificarii, expusa ca flux imutabil catre UI
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Initiaza procesul de login cu email si parola.
     *
     * Flow: valideaza campurile → seteaza Loading → apeleaza repository.login()
     * → in caz de succes seteaza Success(token), altfel seteaza Error(mesaj).
     * API-ul FastAPI asteapta form-encoded cu campul "username" (nu "email").
     */
    fun login(email: String, password: String) {
        // Valideaza ca ambele campuri sunt completate inainte de a trimite cererea
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.login(email, password).fold(
                onSuccess = { _authState.value = AuthState.Success(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Login failed") }
            )
        }
    }

    /**
     * Initiaza procesul de inregistrare cu nume, email si parola.
     *
     * Flow similar cu login: valideaza → Loading → apel API register →
     * Success cu token (backend-ul logheaza automat dupa register) sau Error.
     */
    fun register(name: String, email: String, password: String) {
        // Toate cele trei campuri sunt obligatorii pentru inregistrare
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.register(name, email, password).fold(
                onSuccess = { _authState.value = AuthState.Success(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    /**
     * Reseteaza starea la [AuthState.Idle].
     * Apelat de UI dupa ce a procesat un Success sau dupa navigare,
     * pentru a evita re-procesarea aceleiasi stari la recompunere.
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
