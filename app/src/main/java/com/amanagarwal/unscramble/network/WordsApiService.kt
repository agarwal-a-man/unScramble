package com.amanagarwal.unscramble.network

import androidx.annotation.Keep

/**
 * Contract for the random word API.
 *
 * NOTE: The @GET / @Query annotations below are kept as documentation of the API endpoint
 * and query parameter, but they are NOT processed by Retrofit's dynamic proxy — see
 * [com.amanagarwal.unscramble.di.NetworkModule.provideWordsApiService] for why we use
 * a direct OkHttp implementation instead.
 *
 * Endpoint: GET https://random-word-api.herokuapp.com/word?number={number}
 * Response: JSON array of strings, e.g. ["apple", "bench", "crane"]
 */
@Keep
interface WordsApiService {
    suspend fun getWord(number: Int = 10): Set<String>
}
