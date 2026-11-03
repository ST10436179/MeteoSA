package com.meteosa.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User Defined feature (Part 3): a named location the user wants quick access to (e.g. "Home",
 * "Mom's house", "Work"), so they don't have to rely on GPS or the Johannesburg fallback every
 * time. Local-only (RoomDB) - this is a device preference, not something that needs to sync
 * across devices via the backend.
 */
@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis()
)
