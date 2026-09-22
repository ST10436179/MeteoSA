package com.meteosa.app.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

/**
 * Turns a Postgres `timestamptz` as returned by the backend (e.g. "2026-09-22T03:39:54.763Z")
 * into a short "X ago" label. Only the first 19 characters are parsed (fractional seconds and
 * the trailing "Z" are ignored) since sub-second precision doesn't matter for a relative label.
 */
fun isoTimestampToRelativeTime(iso: String): String = try {
    val truncated = if (iso.length >= 19) iso.substring(0, 19) else iso
    val then = isoFormat.parse(truncated)?.time
    if (then == null) {
        ""
    } else {
        val minutes = (System.currentTimeMillis() - then) / 60_000
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 60 * 24 -> "${minutes / 60}h ago"
            else -> "${minutes / (60 * 24)}d ago"
        }
    }
} catch (t: Throwable) {
    ""
}
