package com.denis.smarthome.ui.screens.control

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.ui.theme.OnSurface
import com.denis.smarthome.ui.theme.Primary
import com.denis.smarthome.viewmodel.DeviceControlViewModel

private fun isTvDevice(deviceType: String, name: String, irProtocol: String?): Boolean {
    val n = name.lowercase()
    val t = deviceType.lowercase()
    return t == "ir" && (n.contains("tv") || n.contains("television") ||
            irProtocol?.lowercase()?.contains("tv") == true)
}

private fun isAcDevice(deviceType: String, name: String, irProtocol: String?): Boolean {
    val n = name.lowercase()
    val t = deviceType.lowercase()
    return t == "ir" && (n.contains("ac") || n.contains("air") || n.contains("conditioner") ||
            irProtocol?.lowercase()?.contains("ac") == true ||
            irProtocol?.lowercase()?.contains("nec") == true)
}

@Composable
fun DeviceControlRouter(
    navController: NavController,
    deviceId: Int
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: DeviceControlViewModel = viewModel(
        factory = DeviceControlViewModel.Factory(app, deviceId)
    )
    val device by viewModel.device.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        error != null || device == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error ?: "Device not found", color = OnSurface)
            }
        }
        else -> {
            val d = device!!
            when {
                isTvDevice(d.device_type, d.name, d.ir_protocol) -> {
                    TvRemoteScreen(
                        navController = navController,
                        device = d,
                        deviceId = deviceId
                    )
                }
                isAcDevice(d.device_type, d.name, d.ir_protocol) -> {
                    AcControlScreen(
                        navController = navController,
                        device = d,
                        deviceId = deviceId
                    )
                }
                else -> {
                    // Generic relay/wol device — show simple toggle screen
                    GenericDeviceScreen(
                        navController = navController,
                        device = d,
                        deviceId = deviceId
                    )
                }
            }
        }
    }
}
