package com.amanagarwal.unscramble.data

import com.amanagarwal.unscramble.network.WordsApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

interface AppContainer {
    val wordsRepository : WordsRepository
}

class DefaultAppContainer : AppContainer{
    private val baseUrl = "https://random-word-api.vercel.app/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .baseUrl(baseUrl)
        .build()

    private val retrofitService: WordsApiService by lazy {
        retrofit.create(WordsApiService::class.java)
    }


    override val wordsRepository: WordsRepository by lazy{
        NetworkWordsRepository(retrofitService)
    }
}