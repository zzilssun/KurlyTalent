package com.kerly.data.di

import android.content.Context
import androidx.room.Room
import com.kerly.data.local.AppDatabase
import com.kerly.data.local.dao.LocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "kurly_talent_db"

    @Provides
    @Singleton
    internal fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = DATABASE_NAME,
        ).build()
    }

    @Provides
    @Singleton
    internal fun provideLocationDao(database: AppDatabase): LocationDao {
        return database.locationDao()
    }
}