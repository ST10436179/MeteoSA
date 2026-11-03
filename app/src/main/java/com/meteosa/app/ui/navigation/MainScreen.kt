package com.meteosa.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.meteosa.app.R
import com.meteosa.app.di.AppContainer
import com.meteosa.app.ui.screens.home.HomeScreen
import com.meteosa.app.ui.screens.reports.ReportsScreen
import com.meteosa.app.ui.screens.settings.SettingsScreen
import com.meteosa.app.ui.screens.settings.badgeLabelRes
import com.meteosa.app.ui.screens.settings.badgeTierForPoints
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class MainTab { HOME, REPORTS, SETTINGS }

@Composable
fun MainScreen(container: AppContainer, onLoggedOut: () -> Unit) {
    var tab by remember { mutableStateOf(MainTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val session by container.sessionManager.session.collectAsState()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op either way: notifications are a nice-to-have, never a blocker */ }

    // Push notifications (Part 3): ask for POST_NOTIFICATIONS on API 33+, then push this
    // device's current FCM token to the backend so it can target real-time alerts at it.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            container.notificationsRepository.updateFcmToken(token)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.HOME,
                    onClick = { tab = MainTab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_home)) }
                )
                NavigationBarItem(
                    selected = tab == MainTab.REPORTS,
                    onClick = { tab = MainTab.REPORTS },
                    icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_reports)) }
                )
                NavigationBarItem(
                    selected = tab == MainTab.SETTINGS,
                    onClick = { tab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    weatherRepository = container.weatherRepository,
                    dataSaverRepository = container.dataSaverRepository,
                    reportsRepository = container.reportsRepository,
                    savedLocationsRepository = container.savedLocationsRepository
                )
                MainTab.REPORTS -> ReportsScreen(
                    reportsRepository = container.reportsRepository,
                    currentUserId = session?.userId ?: "",
                    onPointsAwarded = { totalPoints ->
                        container.sessionManager.updatePoints(totalPoints)
                        coroutineScope.launch {
                            val badgeLabel = context.getString(badgeLabelRes(badgeTierForPoints(totalPoints)))
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.report_submitted_snackbar, badgeLabel, totalPoints)
                            )
                        }
                    },
                    onReportDeleted = { totalPoints ->
                        container.sessionManager.updatePoints(totalPoints)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.report_deleted_snackbar, totalPoints)
                            )
                        }
                    },
                    onReportQueuedOffline = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.offline_report_saved_snackbar))
                        }
                    }
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
