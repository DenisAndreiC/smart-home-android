/**
 * NavRoutes.kt - Navigation routes for the SmartHome app.
 *
 * Defines all possible destinations in the Compose navigation graph
 * as a sealed class. Each inner object represents a screen with its
 * unique route string.
 *
 * Project: SmartHome IoT - Licenta CSIE-ASE 2025
 * Author: Denis Andrei C.
 */
package com.denis.smarthome.ui.navigation

/**
 * Sealed class centralizing all navigation routes.
 * Using a sealed class prevents incorrect routes at compile time
 * and provides a single source of truth for all destinations.
 *
 * @param route The string used by NavHost to identify the screen.
 */
sealed class NavRoutes(val route: String) {
    /** Authentication screen — default start destination. */
    object Login : NavRoutes("login")

    /** New account registration screen. */
    object Register : NavRoutes("register")

    /** Main dashboard screen with stats and recent activity. */
    object Home : NavRoutes("home")

    /** Device list screen with room filter chips. */
    object Devices : NavRoutes("devices")

    /** Scenes screen — execute and delete automation scenes. */
    object Scenes : NavRoutes("scenes")

    /** Settings screen — logout and user preferences. */
    object Settings : NavRoutes("settings")

    /** Notifications screen. */
    object Notifications : NavRoutes("notifications")

    /**
     * Device control screen for a specific device.
     * Receives the device ID as an integer argument in the route.
     */
    object DeviceControl : NavRoutes("device_control/{deviceId}") {
        /**
         * Builds the concrete route with the device ID interpolated.
         * @param deviceId Unique ID of the device to control.
         */
        fun createRoute(deviceId: Int) = "device_control/$deviceId"
    }

    /** Change-password screen, accessible from Settings. */
    object ChangePassword : NavRoutes("change_password")

    /**
     * Scene editor screen — create or edit an existing scene.
     * [sceneId] is optional: absent = new scene, present = edit existing.
     */
    object SceneEditor : NavRoutes("scene_editor?sceneId={sceneId}") {
        /**
         * Builds the editor route.
         * @param sceneId ID of the scene to edit, or null for a new scene.
         */
        fun createRoute(sceneId: Int? = null) =
            // If sceneId is null, navigate without the query parameter
            if (sceneId != null) "scene_editor?sceneId=$sceneId" else "scene_editor"
    }
}

/** Routes that show the bottom navigation bar. */
val bottomNavRoutes = listOf(
    NavRoutes.Home.route,
    NavRoutes.Devices.route,
    NavRoutes.Scenes.route,
    NavRoutes.Settings.route
)
