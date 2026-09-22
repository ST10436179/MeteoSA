package com.meteosa.app.ui.screens.reports

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.meteosa.app.util.isoTimestampToRelativeTime

private const val DEFAULT_LAT = -26.2041
private const val DEFAULT_LON = 28.0473

@Composable
fun ReportsScreen(
    reportsRepository: ReportsRepository,
    currentUserId: String,
    onPointsAwarded: (Int) -> Unit,
    onReportDeleted: (Int) -> Unit
) {
    val viewModel: ReportsViewModel = viewModel(factory = GenericViewModelFactory { ReportsViewModel(reportsRepository) })
    val reportsState by viewModel.reportsState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var reportPendingDelete by remember { mutableStateOf<ReportDto?>(null) }

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

    LaunchedEffect(deleteState) {
        val state = deleteState
        if (state is UiState.Success) {
            onReportDeleted(state.data)
            viewModel.resetDeleteState()
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val reportsForMap = (reportsState as? UiState.Success)?.data.orEmpty()
            ReportsMapView(
                reports = reportsForMap,
                centerLat = deviceLat,
                centerLon = deviceLon,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
            // A rounded, elevated panel below the map (rather than plain background) makes it
            // read as its own section instead of the report cards looking like they're floating
            // loose over the map with no boundary between the two.
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ReportsLegend()
                    HorizontalDivider()
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
                                        // Extra bottom padding keeps the last card clear of the FAB,
                                        // which otherwise sits directly on top of it.
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(state.data) { report ->
                                        ReportCard(
                                            report = report,
                                            isOwnReport = report.userId == currentUserId,
                                            onDeleteClick = { reportPendingDelete = report }
                                        )
                                    }
                                    }
                                }
                            }
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

    reportPendingDelete?.let { report ->
        AlertDialog(
            onDismissRequest = { reportPendingDelete = null },
            title = { Text("Delete this report?") },
            text = { Text("This also removes the points it earned. This can't be undone.") },
            confirmButton = {
                TextButton(
                    enabled = deleteState !is UiState.Loading,
                    onClick = {
                        viewModel.deleteReport(report.reportId, deviceLat, deviceLon)
                        reportPendingDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { reportPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

/** Always-visible key explaining the map's marker colours, and fills what would otherwise be a
 *  big empty gap below a short report list. */
@Composable
private fun ReportsLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ReportType.values().forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(colorForReportType(type.apiValue), CircleShape)
                )
                Spacer(Modifier.width(4.dp))
                Text(type.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ReportCard(report: ReportDto, isOwnReport: Boolean, onDeleteClick: () -> Unit) {
    val typeLabel = ReportType.values().find { it.apiValue == report.reportType }?.label ?: report.reportType
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(colorForReportType(report.reportType), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(typeLabel, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                Text(
                    "· ${report.displayName ?: "Community member"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (isOwnReport) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete report",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(report.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                isoTimestampToRelativeTime(report.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
