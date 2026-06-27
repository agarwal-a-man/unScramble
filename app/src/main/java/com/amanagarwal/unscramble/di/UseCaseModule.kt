package com.amanagarwal.unscramble.di

import com.amanagarwal.unscramble.domain.usecase.CalculateScoreUseCase
import com.amanagarwal.unscramble.domain.usecase.ShuffleWordUseCase
import com.amanagarwal.unscramble.domain.usecase.ValidateGuessUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Provides use cases scoped to the ViewModel's lifetime.
 * ViewModelScoped means one instance per ViewModel — created when the ViewModel
 * is created and garbage-collected with it. Since use cases are stateless, this
 * is the correct minimal scope (not Singleton, not unscoped).
 */
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideShuffleWordUseCase(): ShuffleWordUseCase = ShuffleWordUseCase()

    @Provides
    @ViewModelScoped
    fun provideValidateGuessUseCase(): ValidateGuessUseCase = ValidateGuessUseCase()

    @Provides
    @ViewModelScoped
    fun provideCalculateScoreUseCase(): CalculateScoreUseCase = CalculateScoreUseCase()
}
