package com.amanagarwal.unscramble.network

import retrofit2.http.GET
import retrofit2.http.Query

interface WordsApiService {
    @GET("word")
    suspend fun getWord(
        @Query("number") number: Int = 10
    ): Set<String>
}
