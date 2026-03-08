/**
 * TokenManager.kt - Gestiunea persistenta a token-ului JWT de autentificare
 *
 * Foloseste Jetpack DataStore Preferences pentru stocarea securizata a token-ului JWT.
 * DataStore este alternativa moderna la SharedPreferences: opereaza asincron cu
 * coroutine si Flow, eliminand riscul de ANR (Application Not Responding) cauzat
 * de I/O sincron pe firul principal.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensie pe Context care creeaza (sau returneaza) instanta DataStore cu numele "smarthome_prefs".
// `by preferencesDataStore` este un delegate Kotlin care garanteaza o singura instanta
// per proces, similara cu un Singleton — indiferent de cate ori se acceseaza Context.dataStore.
// Fisierul de persistenta este stocat in directorul privat al aplicatiei pe dispozitiv.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smarthome_prefs")

/**
 * Clasa responsabila cu salvarea, citirea si stergerea token-ului JWT din DataStore.
 *
 * Comparatie DataStore vs SharedPreferences:
 * - SharedPreferences: API sincron, poate bloca firul principal, nu e thread-safe
 * - DataStore Preferences: API asincron bazat pe coroutine si Flow, thread-safe,
 *   suporta anularea operatiei (cancellation) si gestioneaza erorile mai bine.
 *
 * @param context contextul aplicatiei (Application context), necesar pentru accesul la DataStore
 */
class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val THEME_KEY = booleanPreferencesKey("is_dark_theme")
    }

    /**
     * Salveaza token-ul JWT in DataStore dupa autentificare cu succes.
     *
     * Functia este `suspend` — se executa asincron, fara a bloca firul principal.
     * DataStore.edit() garanteaza ca scrierea este atomica si consistenta.
     *
     * @param token string-ul JWT primit de la server dupa login sau register
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    /**
     * Returneaza un Flow care emite token-ul curent (sau null daca nu exista).
     *
     * Flow<String?> este "reactive" — orice modificare in DataStore (save/clear)
     * va emite automat noua valoare catre toti colectorii activi.
     * Nu este o functie suspend, deoarece nu face I/O imediat — returneaza doar
     * un stream de date care va fi colectat asincron de apelant.
     *
     * @return [Flow] care emite token-ul JWT sau null daca utilizatorul nu e autentificat
     */
    fun getToken(): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[TOKEN_KEY] }

    /**
     * Sterge token-ul JWT din DataStore — folosit la logout.
     *
     * Dupa apelul acestei functii, getToken() va emite null si isLoggedIn() va emite false,
     * declansand navigarea catre ecranul de login in toate colectorii activi.
     */
    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
        }
    }

    /**
     * Returneaza un Flow<Boolean> care indica daca utilizatorul este autentificat.
     *
     * Derivat din [getToken] prin transformare: emite `true` daca token-ul exista
     * si nu este gol, `false` in caz contrar. Folosit in NavGraph pentru a decide
     * ruta de start (login vs home) si in SettingsScreen pentru logica de logout.
     *
     * @return [Flow] care emite true daca exista un token JWT valid stocat local
     */
    fun isLoggedIn(): Flow<Boolean> =
        getToken().map { it != null && it.isNotBlank() }

    suspend fun saveTheme(isDark: Boolean) {
        context.dataStore.edit { prefs -> prefs[THEME_KEY] = isDark }
    }

    fun getTheme(): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[THEME_KEY] ?: true }
}
