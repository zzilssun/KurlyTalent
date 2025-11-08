package com.kerly.data.di

import com.kerly.data.usecase.EnqueueLocationUpdateUseCaseImpl
import com.kerly.data.usecase.ObserveLocationWorkUseCaseImpl
import com.kurly.domain.usecase.EnqueueLocationUpdateUseCase
import com.kurly.domain.usecase.ObserveLocationWorkUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    @Singleton
    abstract fun bindEnqueueLocationUpdateUseCase(
        useCase: EnqueueLocationUpdateUseCaseImpl
    ): EnqueueLocationUpdateUseCase

    @Binds
    @Singleton
    abstract fun bindObserveLocationWorkUseCase(
        useCase: ObserveLocationWorkUseCaseImpl
    ): ObserveLocationWorkUseCase
}