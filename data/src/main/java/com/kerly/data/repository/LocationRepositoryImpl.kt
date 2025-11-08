package com.kerly.data.repository

import com.kerly.data.local.dao.LocationDao
import com.kerly.data.local.entities.LocationEntity
import com.kerly.data.mapper.toLocationInfo
import com.kurly.domain.model.LocationInfo
import com.kurly.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
) : LocationRepository {

    override suspend fun saveLocation(latitude: Double, longitude: Double, timestamp: Long) {
        val locationEntity = LocationEntity(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        )
        locationDao.insertLocation(locationEntity)
    }

    override fun getAllLocations(): Flow<List<LocationInfo>> {
        return locationDao.getAllLocations().map { entities ->
            entities.map { it.toLocationInfo() }
        }
    }
}