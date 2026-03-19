/**
 * TokenManager.kt - Persistent management of the JWT authentication token.
 *
 * Uses Jetpack DataStore Preferences for secure JWT token storage.
 * DataStore is the modern replacement for SharedPreferences: it operates
 * asynchronously with coroutines and Flow, eliminating the ANR risk caused
 * by synchronous I/O on the main thread.
 *
 * Project: SmartHome IoT - Licenta CSIE-ASE 2025
 * Author: Denis Andrei C.
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

// Context extension that creates (or returns) the DataStore instance named "smarthome_prefs".
// `by preferencesDataStore` is a Kotlin delegate that guarantees a single instance per
// process, similar to a Singleton — regardless of how many times Context.dataStore is accessed.
// The persistence file is stored in the app's private directory on the device.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smarthome_prefs")

/**
 * Responsible for saving, reading, and clearing the JWT token from DataStore.
 *
 * DataStore vs SharedPreferences:
 * - SharedPreferences: synchronous API, can block the main thread, not thread-safe.
 * - DataStore Preferences: async API backed by coroutines and Flow, thread-safe,
 *   supports cancellation and handles errors more reliably.
 *
 * @param context Application context required for DataStore access.
 */
class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val THEME_KEY = booleanPreferencesKey("is_dark_theme")
    }

    /**
     * Saves the JWT token to DataStore after a successful authentication.
     *
     * Suspend function — executes asynchronously without blocking the main thread.
     * DataStore.edit() guarantees atomic and consistent writes.
     *
     * @param token JWT string received from the server after login or register.
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    /**
     * Returns a Flow that emits the current token (or null if none exists).
     *
     * Flow<String?> is reactive — any DataStore change (save/clear) automatically
     * emits the new value to all active collectors.
     * Not a suspend function because it does not perform I/O immediately —
     * it just returns a stream that the caller collects asynchronously.
     *
     * @return [Flow] emitting the JWT token, or null if the user is not authenticated.
     */
    fun getToken(): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[TOKEN_KEY] }

    /**
     * Removes the JWT token from DataStore — called on logout.
     *
     * After this call, getToken() emits null and isLoggedIn() emits false,
     * triggering navigation to the login screen in all active collectors.
     */
    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
        }
    }

    /**
     * Returns a Flow<Boolean> indicating whether the user is authenticated.
     *
     * Derived from [getToken] by mapping: emits `true` if a non-blank token exists,
     * `false` otherwise. Used in NavGraph to determine the start destination
     * (login vs home) and in SettingsScreen for logout logic.
     *
     * @return [Flow] emitting true if a valid JWT token is stored locally.
     */
    fun isLoggedIn(): Flow<Boolean> =
        getToken().map { it != null && it.isNotBlank() }

    suspend fun saveTheme(isDark: Boolean) {
        context.dataStore.edit { prefs -> prefs[THEME_KEY] = isDark }
    }

    fun getTheme(): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[THEME_KEY] ?: true }
}
