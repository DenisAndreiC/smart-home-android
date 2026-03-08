/**
 * AuthRepository.kt - Repository pentru operatiile de autentificare
 *
 * Intermediaza comunicarea intre ViewModels si ApiService pentru autentificare,
 * gestionand totodata persistenta token-ului JWT prin TokenManager.
 * Toate operatiile returneaza Result<T> pentru gestionarea uniforma a erorilor.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.data.repository

import com.denis.smarthome.data.api.ApiService
import com.denis.smarthome.data.local.TokenManager
import com.denis.smarthome.data.model.LoginRequest
import com.denis.smarthome.data.model.RegisterRequest
import com.denis.smarthome.data.model.UserResponse

/**
 * Repository care expune operatiile de autentificare catre stratul ViewModel.
 *
 * Pattern-ul `runCatching { }` este folosit consistent in toate functiile:
 * - Executa blocul lamdba si returneaza Result.success(valoare) daca nu apar exceptii
 * - Captureaza orice Throwable (inclusiv exceptii de retea, HTTP 4xx/5xx) si
 *   returneaza Result.failure(exceptie) fara a crasha aplicatia
 * - ViewModel-ul poate inspecta rezultatul cu .onSuccess { } / .onFailure { }
 * - Elimina necesitatea blocurilor try-catch repetitive in fiecare functie
 *
 * @param apiService interfata Retrofit pentru apeluri HTTP
 * @param tokenManager manager pentru persistenta JWT in DataStore
 */
class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    /**
     * Autentifica utilizatorul cu email si parola.
     *
     * Apeleaza POST /auth/login (form-encoded, OAuth2PasswordRequestForm).
     * La succes, salveaza automat token-ul JWT in DataStore pentru sessiunile ulterioare.
     *
     * @param email adresa de email a utilizatorului (trimisa ca camp "username" OAuth2)
     * @param password parola utilizatorului
     * @return Result.success(token) daca autentificarea a reusit,
     *         Result.failure(exceptie) in caz de credentiale invalide sau eroare de retea
     */
    suspend fun login(email: String, password: String): Result<String> = runCatching {
        // "username" este numele campului standard OAuth2, dar valoarea este adresa de email
        val response = apiService.login(LoginRequest(email = email, password = password))
        // Salveaza token-ul imediat dupa autentificare — va fi citit de AuthInterceptor la urmatoarele request-uri
        tokenManager.saveToken(response.access_token)
        response.access_token
    }

    /**
     * Inregistreaza un utilizator nou si il autentifica imediat.
     *
     * Apeleaza POST /auth/register cu JSON body (nu form-encoded ca login).
     * Backend-ul creeaza contul si returneaza direct un token JWT — nu e nevoie
     * de un pas separat de login dupa inregistrare.
     *
     * @param name numele complet al utilizatorului
     * @param email adresa de email unica pentru cont
     * @param password parola aleasa de utilizator
     * @return Result.success(token) daca inregistrarea a reusit,
     *         Result.failure(exceptie) daca email-ul exista deja sau alta eroare
     */
    suspend fun register(name: String, email: String, password: String): Result<String> = runCatching {
        // Backend-ul returneaza UserResponse la register (nu token), deci facem login automat dupa
        apiService.register(RegisterRequest(username = name, email = email, password = password))
        val tokenResponse = apiService.login(LoginRequest(email = email, password = password))
        tokenManager.saveToken(tokenResponse.access_token)
        tokenResponse.access_token
    }

    /**
     * Obtine datele profilului utilizatorului curent autentificat.
     *
     * Apeleaza GET /auth/me — endpoint protejat cu JWT.
     * Token-ul este adaugat automat de AuthInterceptor.
     *
     * @return Result.success(UserResponse) cu datele profilului,
     *         Result.failure(exceptie) daca token-ul a expirat (401) sau eroare de retea
     */
    suspend fun getMe(): Result<UserResponse> = runCatching {
        apiService.getMe()
    }

    /**
     * Deconecteaza utilizatorul prin stergerea token-ului JWT local.
     *
     * Nu apeleaza niciun endpoint API — logout-ul este strict local (token stateless JWT).
     * Dupa apel, TokenManager.isLoggedIn() va emite false, declansand
     * navigarea catre LoginScreen in NavGraph.
     */
    suspend fun logout() {
        // Sterge token-ul din DataStore — urmatoarele request-uri vor fi neautentificate
        tokenManager.clearToken()
    }
}
