/**
 * NavRoutes.kt - Rutele de navigare ale aplicatiei SmartHome
 *
 * Defineste toate destinatiile posibile din graficul de navigare Compose
 * sub forma unei clase sigilate (sealed class). Fiecare obiect interior
 * reprezinta un ecran cu ruta sa unica ca sir de caractere.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.navigation

/**
 * Clasa sigilata care centralizeaza toate rutele de navigare.
 * Folosirea unei sealed class previne rutele incorecte la compile time
 * si ofera un singur punct de adevar pentru toate destinatiile.
 *
 * @param route Sirul de caractere utilizat de NavHost pentru identificarea ecranului.
 */
sealed class NavRoutes(val route: String) {
    /** Ecranul de autentificare — destinatia de start implicita. */
    object Login : NavRoutes("login")

    /** Ecranul de inregistrare cont nou. */
    object Register : NavRoutes("register")

    /** Ecranul principal (dashboard) cu statistici si activitate recenta. */
    object Home : NavRoutes("home")

    /** Ecranul listei de dispozitive cu filtrare dupa camera. */
    object Devices : NavRoutes("devices")

    /** Ecranul scenelor automate — executie si stergere. */
    object Scenes : NavRoutes("scenes")

    /** Ecranul setarilor — deconectare si preferinte utilizator. */
    object Settings : NavRoutes("settings")

    /**
     * Ecranul de control al unui dispozitiv specific.
     * Primeste ID-ul dispozitivului ca argument de tip intreg in ruta.
     */
    object DeviceControl : NavRoutes("device_control/{deviceId}") {
        /**
         * Construieste ruta concreta cu ID-ul dispozitivului interpolat.
         * @param deviceId ID-ul unic al dispozitivului de controlat.
         */
        fun createRoute(deviceId: Int) = "device_control/$deviceId"
    }

    /**
     * Ecranul editorului de scena — creare sau editare scena existenta.
     * [sceneId] este optional: absent = scena noua, prezent = editare.
     */
    object SceneEditor : NavRoutes("scene_editor?sceneId={sceneId}") {
        /**
         * Construieste ruta pentru editor.
         * @param sceneId ID-ul scenei de editat, sau null pentru scena noua.
         */
        fun createRoute(sceneId: Int? = null) =
            // Daca sceneId este null, navigam fara parametru de interogare
            if (sceneId != null) "scene_editor?sceneId=$sceneId" else "scene_editor"
    }
}

/** Lista rutelor care apar in bara de navigare de jos (bottom navigation bar). */
val bottomNavRoutes = listOf(
    NavRoutes.Home.route,
    NavRoutes.Devices.route,
    NavRoutes.Scenes.route,
    NavRoutes.Settings.route
)
