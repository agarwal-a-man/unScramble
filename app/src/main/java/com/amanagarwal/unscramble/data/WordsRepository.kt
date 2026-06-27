package com.amanagarwal.unscramble.data

import android.util.Log
import com.amanagarwal.unscramble.network.WordsApiService
import javax.inject.Inject

private const val TAG = "WordsRepository"

interface WordsRepository {
    /**
     * Returns the best available word set and its source.
     * Callers should never need to handle fallback — that is the repository's job.
     */
    suspend fun getWords(): WordsResult

    /**
     * Triggers a background attempt to refresh the in-memory cache from the API.
     * Safe to call at any time — silently ignored if the request fails.
     * Returns true if the refresh succeeded and cache was updated.
     */
    suspend fun refreshCacheInBackground(): Boolean
}

class NetworkWordsRepository @Inject constructor(
    private val wordsApiService: WordsApiService
) : WordsRepository {

    /**
     * In-memory word cache — updated whenever the API responds successfully.
     * Survives configuration changes (repository is Singleton-scoped via Hilt).
     *
     * Phase 2 migration note: replace this with a Room DAO call so the cache
     * persists across process restarts.
     */
    private var wordCache: Set<String> = emptySet()

    override suspend fun getWords(): WordsResult {
        return try {
            val words = wordsApiService.getWord()
            if (words.isEmpty()) throw Exception("API returned empty word list")
            wordCache = words
            Log.d(TAG, "Live words fetched. Count: ${words.size}. Cache updated.")
            WordsResult.Live(words)
        } catch (e: Exception) {
            Log.w(TAG, "API failed: ${e.message}. Checking cache.")
            if (wordCache.isNotEmpty()) {
                Log.d(TAG, "Serving cached words. Count: ${wordCache.size}")
                WordsResult.Cached(wordCache)
            } else {
                Log.w(TAG, "No cache available. Falling back to static word list.")
                WordsResult.Static(allWords)
            }
        }
    }

    override suspend fun refreshCacheInBackground(): Boolean {
        return try {
            val words = wordsApiService.getWord()
            if (words.isEmpty()) return false
            wordCache = words
            Log.d(TAG, "Background cache refresh successful. Count: ${words.size}")
            true
        } catch (e: Exception) {
            Log.d(TAG, "Background cache refresh failed silently: ${e.message}")
            false
        }
    }
}