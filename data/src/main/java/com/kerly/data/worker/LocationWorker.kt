package com.kerly.data.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.kerly.data.worker.exception.LocationException
import com.kurly.domain.repository.LocationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class LocationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val a = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val b = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!a && !b) {
                throw LocationException.PermissionDenied()
            }

            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (location != null) {
                locationRepository.saveLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis()
                )
                Result.success()
            } else {
                throw LocationException.FailToFetch()
            }

        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "LocationWorker"
    }
}