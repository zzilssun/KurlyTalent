package com.kurly.domain.repository

import com.kurly.domain.model.LocationInfo
import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    suspend fun saveLocation(latitude: Double, longitude: Double, timestamp: Long)

    fun getAllLocations(): Flow<List<LocationInfo>>
}