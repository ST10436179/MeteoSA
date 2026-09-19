package com.meteosa.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.meteosa.app.di.AppContainer
import com.meteosa.app.ui.screens.home.HomeScreen
import com.meteosa.app.ui.screens.reports.ReportsScreen
import com.meteosa.app.ui.screens.settings.SettingsScreen

private enum class MainTab(val label: String) { HOME("Home"), REPORTS("Reports"), SETTINGS("Settings") }

@Composable
fun MainScreen(container: AppContainer, onLoggedOut: () -> Unit) {
    var tab by remember { mutableStateOf(MainTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.HOME,
                    onClick = { tab = MainTab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == MainTab.REPORTS,
                    onClick = { tab = MainTab.REPORTS },
                    icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                    label = { Text("Reports") }
                )
                NavigationBarItem(
                    selected = tab == MainTab.SETTINGS,
                    onClick = { tab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                MainTab.HOME -> HomeScreen(weatherRepository = container.weatherRepository)
                MainTab.REPORTS -> ReportsScreen(
                    reportsRepository = container.reportsRepository,
                    onPointsAwarded = { }
                )
                MainTab.SETTINGS -> SettingsScreen(
                    authRepository = container.authRepository,
                    sessionManager = container.sessionManager,
                    themePreferences = container.themePreferences,
                    onLoggedOut = onLoggedOut
                )
            }
        }
    }
}
