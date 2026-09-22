package com.meteosa.app.ui.screens.reports

import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.meteosa.app.data.remote.dto.ReportDto
import com.meteosa.app.data.remote.dto.ReportType
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * Maps a report type to a marker colour so the map reads at a glance. Not private: ReportsScreen
 * reuses this for the legend shown below the map, so the two always stay in sync.
 */
fun colorForReportType(reportType: String): androidx.compose.ui.graphics.Color = when (reportType) {
    ReportType.FLOOD.apiValue -> androidx.compose.ui.graphics.Color(0xFF1976D2)
    ReportType.HAIL.apiValue -> androidx.compose.ui.graphics.Color(0xFF64B5F6)
    ReportType.WIND.apiValue -> androidx.compose.ui.graphics.Color(0xFF757575)
    ReportType.FIRE.apiValue -> androidx.compose.ui.graphics.Color(0xFFE64A19)
    else -> androidx.compose.ui.graphics.Color(0xFF7B1FA2)
}

private fun dotDrawable(color: Int): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(color)
    setStroke(3, android.graphics.Color.WHITE)
    setSize(36, 36)
}

/**
 * Part 1's "Community Impact" feature: reports "aggregated and displayed on a localised map".
 * Uses osmdroid (OpenStreetMap tiles) rather than the Mapbox named in the design doc, since
 * Mapbox and Google Maps both require a signed-up access token that isn't available in this
 * project - osmdroid needs no account/key at all.
 */
@Composable
fun ReportsMapView(
    reports: List<ReportDto>,
    centerLat: Double,
    centerLon: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val typeLabels = ReportType.values().associateBy { it.apiValue }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().load(
                ctx,
                ctx.getSharedPreferences("osmdroid_prefs", android.content.Context.MODE_PRIVATE)
            )
            Configuration.getInstance().userAgentValue = ctx.packageName
            Configuration.getInstance().osmdroidBasePath = File(ctx.cacheDir, "osmdroid")
            Configuration.getInstance().osmdroidTileCache = File(ctx.cacheDir, "osmdroid/tiles")

            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(11.0)
                controller.setCenter(GeoPoint(centerLat, centerLon))
            }
        },
        update = { mapView ->
            mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
            mapView.overlays.clear()
            reports.forEach { report ->
                val marker = Marker(mapView)
                marker.position = GeoPoint(report.latitude, report.longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker.icon = dotDrawable(colorForReportType(report.reportType).toArgb())
                marker.title = typeLabels[report.reportType]?.label ?: report.reportType
                marker.snippet = report.description
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        },
        onRelease = { it.onDetach() }
    )
}
