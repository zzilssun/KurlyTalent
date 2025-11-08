package com.kerly.core.mvi

interface UseCase<T, R> {
    suspend operator fun invoke(args: T): R
}