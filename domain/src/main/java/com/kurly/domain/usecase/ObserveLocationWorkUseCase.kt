package com.kurly.domain.usecase

import androidx.work.WorkInfo
import com.kerly.core.mvi.UseCase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ObserveLocationWorkUseCase : UseCase<UUID, Flow<WorkInfo?>> {
    override suspend fun invoke(args: UUID): Flow<WorkInfo?>
}