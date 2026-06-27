package com.amanagarwal.unscramble.di

import com.amanagarwal.unscramble.data.NetworkWordsRepository
import com.amanagarwal.unscramble.data.WordsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the NetworkWordsRepository concrete implementation to the WordsRepository interface.
     * Using @Binds instead of @Provides — cleaner when the binding is just an interface-to-impl
     * mapping with no extra construction logic needed.
     */
    @Binds
    @Singleton
    abstract fun bindWordsRepository(impl: NetworkWordsRepository): WordsRepository
}
