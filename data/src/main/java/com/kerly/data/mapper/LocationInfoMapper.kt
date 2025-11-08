package com.kerly.data.mapper

import com.kerly.data.local.entities.LocationEntity
import com.kurly.domain.model.LocationInfo

internal fun LocationEntity.toLocationInfo(): LocationInfo {
    return LocationInfo(
        id = this.id,
        latitude = this.latitude,
        longitude = this.longitude,
        timestamp = this.timestamp
    )
}