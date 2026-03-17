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
import com.denis.smarthome.data.model.ChangePasswordRequest
import com.denis.smarthome.data.model.ForgotPasswordRequest
import com.denis.smarthome.data.model.LoginRequest
import com.denis.smarthome.data.model.MessageResponse
import com.denis.smarthome.data.model.RegisterRequest
import com.denis.smarthome.data.model.UpdateUserRequest
import com.denis.smarthome.data.model.UserResponse
import okhttp3.MultipartBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

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
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = apiService.login(LoginRequest(email = email, password = password))
            tokenManager.saveToken(response.access_token)
            Result.success(response.access_token)
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inregistreaza un utilizator nou si il autentifica imediat.
     *
     * Apeleaza POST /auth/register cu JSON body (nu form-encoded ca login).
     * Backend-ul returneaza UserResponse la register, deci facem login automat dupa.
     * Prinde erorile HTTP (400 email duplicat, 422 validare) si expune mesajul real.
     *
     * @param name username-ul utilizatorului
     * @param email adresa de email unica pentru cont
     * @param password parola aleasa de utilizator (min 6 caractere)
     * @return Result.success(token) daca inregistrarea a reusit,
     *         Result.failure(exceptie) daca email-ul exista deja sau alta eroare
     */
    suspend fun register(name: String, email: String, password: String): Result<String> {
        return try {
            // Daca register esueaza cu HTTP 400/422, aruncam exceptie cu mesajul real
            apiService.register(RegisterRequest(username = name, email = email, password = password))
            val tokenResponse = apiService.login(LoginRequest(email = email, password = password))
            tokenManager.saveToken(tokenResponse.access_token)
            Result.success(tokenResponse.access_token)
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extrage mesajul de eroare real din body-ul HTTP returnat de FastAPI.
     *
     * FastAPI returneaza erori in format:
     *   {"detail": "Email-ul este deja inregistrat"}  — eroare simpla (string)
     *   {"detail": [{"msg": "..."}, ...]}              — eroare de validare (lista)
     *
     * @param e exceptia HTTP de la Retrofit
     * @return mesajul de eroare lizibil pentru utilizator
     */
    private fun parseHttpError(e: HttpException): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string() ?: return e.message()
            val json = JSONObject(errorBody)
            val detail = json.opt("detail")
            when (detail) {
                is String -> detail
                is JSONArray -> {
                    // Eroare de validare Pydantic — extragem primul mesaj
                    val first = detail.optJSONObject(0)
                    first?.optString("msg") ?: e.message()
                }
                else -> e.message()
            }
        } catch (_: Exception) {
            e.message()
        }
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

    suspend fun updateUser(displayName: String): Result<UserResponse> = runCatching {
        apiService.updateUser(UpdateUserRequest(displayName))
    }

    suspend fun uploadAvatar(part: MultipartBody.Part): Result<UserResponse> = runCatching {
        apiService.uploadAvatar(part)
    }

    suspend fun changePassword(current: String, new: String): Result<MessageResponse> = runCatching {
        apiService.changePassword(ChangePasswordRequest(current, new))
    }

    suspend fun forgotPassword(email: String): Result<MessageResponse> = runCatching {
        apiService.forgotPassword(ForgotPasswordRequest(email))
    }
}
