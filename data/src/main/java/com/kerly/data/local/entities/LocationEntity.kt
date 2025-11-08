package com.kerly.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_history")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double, // 위도
    val longitude: Double, // 경도
    val timestamp: Long = System.currentTimeMillis() // 저장 시각
)