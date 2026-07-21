package com.amanagarwal.unscramble.di

import android.util.Log
import com.amanagarwal.unscramble.network.WordsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "NetworkModule"

    /**
     * Base URL for the random word API.
     * Kept here so it's easy to swap during testing or if the endpoint changes.
     */
    private const val WORDS_API_URL = "https://random-word-api.herokuapp.com/word"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Provides OkHttpClient with explicit timeouts.
     * Fixes Bug #10 — previously Retrofit had no timeout configured,
     * causing the app to hang indefinitely on slow connections.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        Log.d(TAG, "Creating OkHttpClient")
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides a manual OkHttp-backed implementation of [WordsApiService].
     *
     * ## Why not Retrofit?
     * Retrofit creates a dynamic JVM proxy that calls [java.lang.reflect.Method.getGenericParameterTypes]
     * at runtime to resolve the `Continuation<Set<String>>` type parameter of suspend functions.
     * R8 (the Android minifier) **always strips generic Signature attributes** from interface
     * method parameters during minification — this happens regardless of any ProGuard
     * `-keepattributes Signature` rules, because signature stripping is applied at the
     * optimization pass level, before attribute-keep rules are evaluated.
     *
     * Result: `ClassCastException: java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType`
     * at `retrofit2.HttpServiceMethod.parseAnnotations:46` in every minified build.
     *
     * ## Why this works
     * This implementation calls OkHttp directly and uses kotlinx-serialization's
     * `decodeFromString<List<String>>()`, which is a **reified inline function** — the type
     * `List<String>` is resolved by the Kotlin compiler at **compile time** and embedded
     * directly into the call site bytecode. No Java runtime reflection is involved, so
     * R8 cannot break it.
     */
    @Provides
    @Singleton
    fun provideWordsApiService(client: OkHttpClient, json: Json): WordsApiService {
        Log.d(TAG, "Creating OkHttp-backed WordsApiService (Retrofit proxy bypassed)")
        return object : WordsApiService {
            override suspend fun getWord(number: Int): Set<String> =
                withContext(Dispatchers.IO) {
                    Log.d(TAG, "Making OkHttp request: $WORDS_API_URL?number=$number")
                    val request = Request.Builder()
                        .url("$WORDS_API_URL?number=$number")
                        .build()
                    val bodyString = client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("HTTP ${response.code}: ${response.message}")
                        }
                        response.body?.string()
                            ?: throw IOException("Response body was null")
                    }
                    Log.d(TAG, "Response received, parsing JSON")
                    json.decodeFromString<List<String>>(bodyString).toSet()
                }
        }
    }
}
