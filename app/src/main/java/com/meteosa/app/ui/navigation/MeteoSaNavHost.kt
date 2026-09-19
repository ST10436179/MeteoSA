package com.meteosa.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meteosa.app.di.AppContainer
import com.meteosa.app.ui.screens.auth.AuthScreen
import com.meteosa.app.ui.screens.splash.SplashScreen

@Composable
fun MeteoSaNavHost(container: AppContainer) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(isLoggedIn = container.sessionManager.isLoggedIn) { loggedIn ->
                val destination = if (loggedIn) Screen.Home.route else Screen.Auth.route
                navController.navigate(destination) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
        composable(Screen.Auth.route) {
            AuthScreen(authRepository = container.authRepository) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
        }
        composable(Screen.Home.route) {
            MainScreen(container = container) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        }
    }
}
