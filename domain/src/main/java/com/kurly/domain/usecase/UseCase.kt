package com.kurly.domain.usecase

interface UseCase<T, R> {
    suspend fun invoke(args: T): R
}