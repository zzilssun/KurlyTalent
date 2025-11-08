package com.kurly.domain.usecase

import java.util.UUID

interface EnqueueLocationUpdateUseCase : UseCase<Unit, UUID> {
    override suspend fun invoke(args: Unit): UUID
}