package com.meteosa.app.data.repository

import com.meteosa.app.data.local.db.SavedLocationDao
import com.meteosa.app.data.local.db.SavedLocationEntity
import kotlinx.coroutines.flow.Flow

/** User Defined feature (Part 3): lets the user save and quickly switch between named
 *  locations instead of always relying on GPS or the Johannesburg fallback. */
class SavedLocationsRepository(private val dao: SavedLocationDao) {
    val savedLocations: Flow<List<SavedLocationEntity>> = dao.observeAll()

    suspend fun save(name: String, latitude: Double, longitude: Double) {
        dao.insert(SavedLocationEntity(name = name, latitude = latitude, longitude = longitude))
    }

    suspend fun delete(location: SavedLocationEntity) {
        dao.delete(location)
    }
}
