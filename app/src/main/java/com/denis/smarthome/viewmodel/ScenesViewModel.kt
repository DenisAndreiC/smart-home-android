/**
 * ScenesViewModel.kt - ViewModel pentru ecranul de gestionare a scenelor
 *
 * Incarca lista de scene din API, permite executia si stergerea acestora.
 * Foloseste executingSceneId pentru a oferi feedback vizual pe butonul "Activate"
 * in timp ce scena se executa, cu un delay de 2 secunde inainte de resetare.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.model.SceneResponse
import com.denis.smarthome.data.repository.SceneRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pentru ScenesScreen.
 *
 * Gestioneaza lista de scene IoT si operatiile pe acestea: incarcare, executie, stergere.
 * Scenele sunt secvente de actiuni pe mai multe dispozitive, executate in ordinea definita.
 */
class ScenesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SceneRepository(RetrofitClient.apiService)

    private val _scenes = MutableStateFlow<List<SceneResponse>>(emptyList())
    val scenes: StateFlow<List<SceneResponse>> = _scenes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * ID-ul scenei care se executa in momentul curent.
     *
     * Folosit de UI pentru feedback vizual: butonul "Activate" al scenei cu acest ID
     * afiseaza un indicator de incarcare sau o animatie. Valoarea este null cand nicio
     * scena nu se executa si revine la null dupa 2 secunde de la finalizare.
     */
    private val _executingSceneId = MutableStateFlow<Int?>(null)
    val executingSceneId: StateFlow<Int?> = _executingSceneId.asStateFlow()

    init { loadScenes() }

    /**
     * Incarca lista completa de scene disponibile din API.
     */
    fun loadScenes() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getScenes()
                .onSuccess { _scenes.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    /**
     * Executa o scena si ofera feedback vizual pe durata executiei.
     *
     * Flow: seteaza executingSceneId = id → apeleaza API → afiseaza mesaj succes/eroare
     * → delay(2000ms) pentru a mentine feedback-ul vizibil → reseteaza executingSceneId la null.
     * Delay-ul de 2 secunde permite utilizatorului sa observe ca butonul a reactionat
     * inainte ca el sa revina la starea normala.
     *
     * @param id ID-ul scenei de executat
     */
    fun executeScene(id: Int) {
        viewModelScope.launch {
            // Marcam scena ca "in executie" pentru feedback vizual in UI
            _executingSceneId.value = id
            repository.executeScene(id)
                .onFailure { _error.value = it.message }
            // Asteptam 2 secunde ca utilizatorul sa vada feedback-ul vizual
            delay(2000)
            // Resetam ID-ul pentru a reveni butonul la starea normala
            _executingSceneId.value = null
        }
    }

    /**
     * Sterge o scena dupa ID si reincarca lista.
     *
     * @param id ID-ul scenei de sters
     */
    fun deleteScene(id: Int) {
        viewModelScope.launch {
            repository.deleteScene(id)
                .onSuccess { loadScenes() }  // Reincarcam lista dupa stergere reusita
                .onFailure { _error.value = it.message }
        }
    }

    /** Sterge mesajul de eroare curent. */
    fun clearError() { _error.value = null }
}
