package com.meteosa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.meteosa.app.ui.navigation.MeteoSaNavHost
import com.meteosa.app.ui.theme.MeteoSaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MeteoSaApplication).container

        setContent {
            val darkTheme by container.themePreferences.isDarkTheme.collectAsState(initial = false)
            MeteoSaTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MeteoSaNavHost(container = container)
                }
            }
        }
    }
}
