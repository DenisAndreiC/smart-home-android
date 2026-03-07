package com.denis.smarthome.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.denis.smarthome.data.local.TokenManager
import com.denis.smarthome.ui.screens.auth.LoginScreen
import com.denis.smarthome.ui.screens.auth.RegisterScreen
import com.denis.smarthome.ui.screens.devices.DevicesListScreen
import com.denis.smarthome.ui.screens.home.HomeScreen
import com.denis.smarthome.ui.screens.scenes.ScenesScreen
import com.denis.smarthome.ui.screens.settings.SettingsScreen
import com.denis.smarthome.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(NavRoutes.Devices.route, "Devices", Icons.Filled.Devices, Icons.Outlined.Devices),
    BottomNavItem(NavRoutes.Scenes.route, "Scenes", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    BottomNavItem(NavRoutes.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun SmartHomeApp(tokenManager: TokenManager) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes

    LaunchedEffect(Unit) {
        val isLoggedIn = tokenManager.isLoggedIn().firstOrNull() ?: false
        if (isLoggedIn) {
            navController.navigate(NavRoutes.Home.route) {
                popUpTo(NavRoutes.Login.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                SmartHomeBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.Login.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(NavRoutes.Login.route) {
                    LoginScreen(navController = navController)
                }
                composable(NavRoutes.Register.route) {
                    RegisterScreen(navController = navController)
                }
                composable(NavRoutes.Home.route) {
                    HomeScreen(navController = navController)
                }
                composable(NavRoutes.Devices.route) {
                    DevicesListScreen(navController = navController)
                }
                composable(NavRoutes.Scenes.route) {
                    ScenesScreen(navController = navController)
                }
                composable(NavRoutes.Settings.route) {
                    SettingsScreen(navController = navController)
                }
            }
        }
    }
}

@Composable
private fun SmartHomeBottomBar(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp,
        modifier = Modifier.height(72.dp)
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    indicatorColor = PrimaryContainer,
                    unselectedIconColor = OnSurface,
                    unselectedTextColor = OnSurface
                )
            )
        }
    }
}
