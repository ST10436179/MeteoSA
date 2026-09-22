package com.meteosa.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meteosa.app.data.local.SessionManager
import com.meteosa.app.data.local.ThemePreferences
import com.meteosa.app.data.repository.AuthRepository
import com.meteosa.app.util.GenericViewModelFactory

@Composable
fun SettingsScreen(
    authRepository: AuthRepository,
    sessionManager: SessionManager,
    themePreferences: ThemePreferences,
    onLoggedOut: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = GenericViewModelFactory { SettingsViewModel(authRepository, sessionManager, themePreferences) }
    )
    val session by viewModel.session.collectAsState()
    val darkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)
    val severeAlerts by themePreferences.severeAlertsEnabled.collectAsState(initial = true)
    val dailyForecastNotifs by themePreferences.dailyForecastNotifsEnabled.collectAsState(initial = true)
    val forceDataSaver by themePreferences.forceDataSaverEnabled.collectAsState(initial = false)

    LaunchedEffect(session) {
        if (session == null) onLoggedOut()
    }
    if (session == null) {
        // Nothing to render while the navigation triggered above takes effect.
        return
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(session!!.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(session!!.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.height(32.dp).width(32.dp)
                            )
                            Spacer(modifier = Modifier.padding(start = 12.dp))
                            Column {
                                Text(
                                    badgeForPoints(session!!.points),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "${session!!.points} points earned reporting weather impacts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Preferences", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            SettingsSwitchRow(
                title = "Dark theme",
                checked = darkTheme,
                onCheckedChange = viewModel::setDarkTheme
            )
            HorizontalDivider()
            SettingsSwitchRow(
                title = "Severe weather alerts",
                checked = severeAlerts,
                onCheckedChange = viewModel::setSevereAlertsEnabled
            )
            HorizontalDivider()
            SettingsSwitchRow(
                title = "Daily forecast notifications",
                checked = dailyForecastNotifs,
                onCheckedChange = viewModel::setDailyForecastNotifsEnabled
            )
            HorizontalDivider()
            SettingsSwitchRow(
                title = "Force Data-Saver Mode",
                checked = forceDataSaver,
                onCheckedChange = viewModel::setForceDataSaverEnabled
            )
            Text(
                "Also switches on automatically on a restricted network or during load-shedding.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Language")
                Text("English (isiZulu, Afrikaans in final release)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.logout() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log out")
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
