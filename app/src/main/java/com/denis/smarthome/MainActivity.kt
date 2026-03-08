package com.denis.smarthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.denis.smarthome.data.api.RetrofitClient
import com.denis.smarthome.data.local.TokenManager
import com.denis.smarthome.ui.navigation.SmartHomeApp
import com.denis.smarthome.ui.theme.SmartHomeTheme
import com.denis.smarthome.ui.theme.ThemeState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        RetrofitClient.init(tokenManager)

        // Citeste preferinta de tema din DataStore inainte de primul frame
        lifecycleScope.launch {
            ThemeState.isDark = tokenManager.getTheme().firstOrNull() ?: true
        }

        setContent {
            SmartHomeTheme {
                SmartHomeApp(tokenManager = tokenManager)
            }
        }
    }
}
