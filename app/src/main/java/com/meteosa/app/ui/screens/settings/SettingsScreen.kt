package com.meteosa.app.ui.screens.settings

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.meteosa.app.R
import com.meteosa.app.data.local.LocalePreferences
import com.meteosa.app.data.local.SessionManager
import com.meteosa.app.data.local.ThemePreferences
import com.meteosa.app.data.repository.AuthRepository
import com.meteosa.app.util.GenericViewModelFactory
import com.meteosa.app.util.UiState
import java.io.ByteArrayOutputStream

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
    val avatarUploadState by viewModel.avatarUploadState.collectAsState()
    val context = LocalContext.current

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
            if (result != null) {
                val (base64, contentType) = downscaleAndEncode(result)
                viewModel.uploadAvatar(base64, contentType)
            }
        }
    }

    LaunchedEffect(session) {
        if (session == null) onLoggedOut()
    }
    if (session == null) {
        // Nothing to render while the navigation triggered above takes effect.
        return
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarPicker(
                            avatarUrl = session!!.avatarUrl,
                            isUploading = avatarUploadState is UiState.Loading,
                            onClick = {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(session!!.displayName, style = MaterialTheme.typography.titleLarge)
                            Text(session!!.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (avatarUploadState is UiState.Error) {
                        Text(
                            (avatarUploadState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
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
                                    stringResource(badgeLabelRes(badgeTierForPoints(session!!.points))),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    stringResource(R.string.points_earned, session!!.points),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.preferences), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            SettingsSwitchRow(
                title = stringResource(R.string.dark_theme),
                checked = darkTheme,
                onCheckedChange = viewModel::setDarkTheme
            )
            HorizontalDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.severe_weather_alerts),
                checked = severeAlerts,
                onCheckedChange = viewModel::setSevereAlertsEnabled
            )
            HorizontalDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.daily_forecast_notifications),
                checked = dailyForecastNotifs,
                onCheckedChange = viewModel::setDailyForecastNotifsEnabled
            )
            HorizontalDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.force_data_saver),
                checked = forceDataSaver,
                onCheckedChange = viewModel::setForceDataSaverEnabled
            )
            Text(
                stringResource(R.string.data_saver_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider()
            LanguageRow()

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.logout() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.logout))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Interactive language picker: persists the chosen tag and recreates the Activity to apply it. */
@Composable
private fun LanguageRow() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val currentTag = remember { LocalePreferences.getLanguageTag(context) }
    val currentLabel = LocalePreferences.SUPPORTED_LANGUAGES.firstOrNull { it.first == currentTag }?.second
        ?: LocalePreferences.SUPPORTED_LANGUAGES.first().second

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.language))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(currentLabel)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                LocalePreferences.SUPPORTED_LANGUAGES.forEach { (tag, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            if (tag != currentTag) {
                                LocalePreferences.setLanguageTag(context, tag)
                                (context as? Activity)?.recreate()
                            }
                        }
                    )
                }
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

/**
 * Profile picture upload (Part 3 User Defined feature): a tappable circular avatar that opens
 * the system Photo Picker (no storage permission needed - it runs in a separate, trusted
 * process that only hands back the one photo the user picks). Shows a small camera badge to
 * make it clear it is tappable, and a progress spinner over it while the upload is in flight.
 */
@Composable
private fun AvatarPicker(avatarUrl: String?, isUploading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !isUploading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(R.string.profile_picture),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = stringResource(R.string.profile_picture),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
        }
        if (isUploading) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** Downscales to at most 512px on the long edge and compresses to an 80%-quality JPEG, then
 *  base64-encodes it - small enough to send as plain JSON rather than multipart. */
private fun downscaleAndEncode(bitmap: Bitmap): Pair<String, String> {
    val maxDimension = 512
    val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    } else {
        bitmap
    }
    val outputStream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    return base64 to "image/jpeg"
}
