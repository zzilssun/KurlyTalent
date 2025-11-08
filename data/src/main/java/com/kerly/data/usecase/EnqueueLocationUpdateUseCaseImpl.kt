package com.kerly.data.usecase

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kerly.data.worker.LocationWorker
import com.kurly.domain.usecase.EnqueueLocationUpdateUseCase
import java.util.UUID
import javax.inject.Inject

class EnqueueLocationUpdateUseCaseImpl @Inject constructor(
    private val workManager: WorkManager,
) : EnqueueLocationUpdateUseCase {

    override suspend fun invoke(args: Unit): UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val locationWorkRequest = OneTimeWorkRequestBuilder<LocationWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName = LocationWorker.WORK_NAME,
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = locationWorkRequest,
        )

        return locationWorkRequest.id
    }
}