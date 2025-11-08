package com.kerly.data.usecase

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kurly.domain.usecase.ObserveLocationWorkUseCase
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class ObserveLocationWorkUseCaseImpl @Inject constructor(
    private val workManager: WorkManager,
) : ObserveLocationWorkUseCase {

    override suspend fun invoke(args: UUID): Flow<WorkInfo?> {
        return workManager.getWorkInfoByIdFlow(args)
    }
}