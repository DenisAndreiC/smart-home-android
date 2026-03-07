package com.denis.smarthome.ui.navigation

sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")
    object Home : NavRoutes("home")
    object Devices : NavRoutes("devices")
    object Scenes : NavRoutes("scenes")
    object Settings : NavRoutes("settings")
    object DeviceControl : NavRoutes("device_control/{deviceId}") {
        fun createRoute(deviceId: Int) = "device_control/$deviceId"
    }
    object SceneEditor : NavRoutes("scene_editor?sceneId={sceneId}") {
        fun createRoute(sceneId: Int? = null) =
            if (sceneId != null) "scene_editor?sceneId=$sceneId" else "scene_editor"
    }
}

val bottomNavRoutes = listOf(
    NavRoutes.Home.route,
    NavRoutes.Devices.route,
    NavRoutes.Scenes.route,
    NavRoutes.Settings.route
)
