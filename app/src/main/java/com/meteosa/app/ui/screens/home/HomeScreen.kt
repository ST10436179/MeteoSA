package com.meteosa.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.remote.dto.ReportType
import com.meteosa.app.data.repository.DataSaverRepository
import com.meteosa.app.data.repository.ReportsRepository
import com.meteosa.app.data.repository.WeatherRepository
import com.meteosa.app.ui.screens.reports.colorForReportType
import com.meteosa.app.util.GenericViewModelFactory
import com.meteosa.app.util.UiState
import kotlin.math.roundToInt

// Fallback used when location permission is denied or unavailable, matching the SA-focus of the app.
private const val DEFAULT_LAT = -26.2041
private const val DEFAULT_LON = 28.0473

private val SEVERE_CONDITION_IDS = (200..232) + listOf(502, 503, 504, 522, 531) + (602..622) + listOf(771, 781)

@Composable
fun HomeScreen(
    weatherRepository: WeatherRepository,
    dataSaverRepository: DataSaverRepository,
    reportsRepository: ReportsRepository
) {
    val viewModel: HomeViewModel = viewModel(
        factory = GenericViewModelFactory { HomeViewModel(weatherRepository, dataSaverRepository, reportsRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val locationLabel by viewModel.locationLabel.collectAsState()
    val dataSaverActive by viewModel.dataSaverActive.collectAsState()
    val nearbyReports by viewModel.nearbyReports.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun loadWithDeviceLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.loadWeather(location.latitude, location.longitude, usingDeviceLocation = true)
                } else {
                    viewModel.loadWeather(DEFAULT_LAT, DEFAULT_LON, usingDeviceLocation = false)
                }
            }.addOnFailureListener {
                viewModel.loadWeather(DEFAULT_LAT, DEFAULT_LON, usingDeviceLocation = false)
            }
        } catch (e: SecurityException) {
            viewModel.loadWeather(DEFAULT_LAT, DEFAULT_LON, usingDeviceLocation = false)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) loadWithDeviceLocation() else viewModel.loadWeather(DEFAULT_LAT, DEFAULT_LON, usingDeviceLocation = false)
    }

    fun requestLocationAndLoad() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            loadWithDeviceLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(Unit) { requestLocationAndLoad() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MeteoSA") },
                actions = {
                    IconButton(onClick = { requestLocationAndLoad() }) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Use my location")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is UiState.Loading, UiState.Idle -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { requestLocationAndLoad() }) { Text("Retry") }
                    }
                }
                is UiState.Success -> {
                    if (dataSaverActive) {
                        DataSaverHomeContent(locationLabel = locationLabel, data = state.data, nearbyReports = nearbyReports)
                    } else {
                        HomeContent(locationLabel = locationLabel, data = state.data, nearbyReports = nearbyReports)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(locationLabel: String, data: HomeUiData, nearbyReports: List<ReportDto>) {
    val current = data.current
    val isSevere = current.weather.firstOrNull()?.id?.let { it in SEVERE_CONDITION_IDS } ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(locationLabel, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${current.main.temp.roundToInt()}°C",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(12.dp))
            Text(
                weatherEmoji(current.weather.firstOrNull()?.icon ?: "01d"),
                style = MaterialTheme.typography.headlineMedium
            )
        }
        Text(
            current.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Feels like ${current.main.feelsLike.roundToInt()}°C · Humidity ${current.main.humidity}% · Wind ${current.wind.speed} m/s",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isSevere) {
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Severe Weather Alert", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.titleMedium)
                        Text(
                            current.weather.first().description.replaceFirstChar { it.uppercase() },
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("5-Day Forecast", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(data.daily) { day ->
                Card(modifier = Modifier.width(84.dp)) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(day.dayLabel, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(weatherEmoji(day.icon), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text("${day.maxTemp}°/${day.minTemp}°", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        NearbyReportsSection(nearbyReports)
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Ties Home back to the app's own community data instead of ending after the forecast - was
 * previously a large empty area below the 5-day forecast with nothing in it.
 */
@Composable
private fun NearbyReportsSection(reports: List<ReportDto>) {
    Spacer(Modifier.height(28.dp))
    Text("Community Reports Nearby", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    if (reports.isEmpty()) {
        Text(
            "No community reports nearby yet. Open the Reports tab to add one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            reports.forEach { report -> NearbyReportRow(report) }
        }
    }
}

@Composable
private fun NearbyReportRow(report: ReportDto) {
    val typeLabel = ReportType.values().find { it.apiValue == report.reportType }?.label ?: report.reportType
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colorForReportType(report.reportType), CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(typeLabel, style = MaterialTheme.typography.titleSmall)
                Text(
                    report.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Part 1's "Data-Saver Mode": no icons, no cards, no per-day graphics - just the numbers, so the
 * screen is cheap to render and cheap to re-fetch on a restricted connection. Shown automatically
 * when the device's network is metered/has system Data Saver on, or a load-shedding check
 * succeeds (see DataSaverRepository); can also be forced on from Settings for testing.
 */
@Composable
private fun DataSaverHomeContent(locationLabel: String, data: HomeUiData, nearbyReports: List<ReportDto>) {
    val current = data.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DataSaverOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(
                "Data-Saver Mode is on - lightweight text view to save data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(20.dp))

        Text(locationLabel, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "${current.main.temp.roundToInt()}°C, ${current.weather.firstOrNull()?.description ?: ""}",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Feels like ${current.main.feelsLike.roundToInt()}°C · Humidity ${current.main.humidity}% · Wind ${current.wind.speed} m/s",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(20.dp))
        Text("5-Day Forecast", style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        data.daily.forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(day.dayLabel, style = MaterialTheme.typography.bodyLarge)
                Text("${day.maxTemp}° / ${day.minTemp}°", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Community Reports Nearby", style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        if (nearbyReports.isEmpty()) {
            Text("No community reports nearby yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            nearbyReports.forEach { report ->
                val typeLabel = ReportType.values().find { it.apiValue == report.reportType }?.label ?: report.reportType
                Text("$typeLabel: ${report.description}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
