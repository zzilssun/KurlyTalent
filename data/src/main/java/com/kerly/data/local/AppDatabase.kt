package com.kerly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kerly.data.local.dao.LocationDao
import com.kerly.data.local.entities.LocationEntity

@Database(
    entities = [LocationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}