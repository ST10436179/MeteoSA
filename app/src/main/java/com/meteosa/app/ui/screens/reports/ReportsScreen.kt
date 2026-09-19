package com.meteosa.app.ui.screens.reports

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.remote.dto.ReportType
import com.meteosa.app.data.repository.ReportsRepository
import com.meteosa.app.util.GenericViewModelFactory
import com.meteosa.app.util.UiState

private const val DEFAULT_LAT = -26.2041
private const val DEFAULT_LON = 28.0473

@Composable
fun ReportsScreen(reportsRepository: ReportsRepository, onPointsAwarded: (Int) -> Unit) {
    val viewModel: ReportsViewModel = viewModel(factory = GenericViewModelFactory { ReportsViewModel(reportsRepository) })
    val reportsState by viewModel.reportsState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var deviceLat by remember { mutableStateOf(DEFAULT_LAT) }
    var deviceLon by remember { mutableStateOf(DEFAULT_LON) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) { deviceLat = loc.latitude; deviceLon = loc.longitude }
                    viewModel.loadReports(deviceLat, deviceLon)
                }
            } catch (e: SecurityException) {
                viewModel.loadReports(deviceLat, deviceLon)
            }
        } else {
            viewModel.loadReports(deviceLat, deviceLon)
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) { deviceLat = loc.latitude; deviceLon = loc.longitude }
                    viewModel.loadReports(deviceLat, deviceLon)
                }
            } catch (e: SecurityException) {
                viewModel.loadReports(deviceLat, deviceLon)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // See AuthScreen for why this is a LaunchedEffect rather than a plain "if": it must fire
    // exactly once per successful submission, not on every recomposition while still Success.
    LaunchedEffect(submitState) {
        val state = submitState
        if (state is UiState.Success) {
            onPointsAwarded(state.data)
            showDialog = false
            viewModel.resetSubmitState()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Community Reports") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Report a weather impact")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = reportsState) {
                is UiState.Loading, UiState.Idle -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Error -> Text(
                    state.message,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            "No reports near you yet. Be the first to report a weather impact!",
                            modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.data) { report -> ReportCard(report) }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        SubmitReportDialog(
            isSubmitting = submitState is UiState.Loading,
            errorMessage = (submitState as? UiState.Error)?.message,
            onDismiss = { showDialog = false; viewModel.resetSubmitState() },
            onSubmit = { type, description ->
                viewModel.submitReport(deviceLat, deviceLon, type, description)
            }
        )
    }
}

@Composable
private fun ReportCard(report: ReportDto) {
    val typeLabel = ReportType.values().find { it.apiValue == report.reportType }?.label ?: report.reportType
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(typeLabel) })
                Spacer(Modifier.width(8.dp))
                Text(
                    report.displayName ?: "Community member",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(report.description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SubmitReportDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (type: String, description: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(ReportType.FLOOD) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report a weather impact") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ReportType.values().forEach { type ->
                            DropdownMenuItem(text = { Text(type.label) }, onClick = { selectedType = type; expanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Knee-deep flooding on Main Rd") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isSubmitting, onClick = { onSubmit(selectedType.apiValue, description) }) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp))
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
