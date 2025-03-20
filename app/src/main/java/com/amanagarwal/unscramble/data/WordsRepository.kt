package com.amanagarwal.unscramble.data

import com.amanagarwal.unscramble.network.WordsApiService

interface WordsRepository {
    suspend fun getUnscrambledWord(): Set<String>
}

class NetworkWordsRepository(private val wordsApiService: WordsApiService) : WordsRepository {
    override suspend fun getUnscrambledWord(): Set<String> {
        return wordsApiService.getWord()
    }
}