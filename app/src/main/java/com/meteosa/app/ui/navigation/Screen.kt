package com.meteosa.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Reports : Screen("reports")
    data object Settings : Screen("settings")
}
