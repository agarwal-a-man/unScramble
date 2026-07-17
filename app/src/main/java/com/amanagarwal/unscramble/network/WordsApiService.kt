package com.amanagarwal.unscramble.network

import androidx.annotation.Keep
import retrofit2.http.GET
import retrofit2.http.Query

@Keep
interface WordsApiService {
    @GET("word")
    suspend fun getWord(
        @Query("number") number: Int = 10
    ): Set<String>
}
