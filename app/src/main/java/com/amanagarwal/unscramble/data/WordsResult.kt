package com.amanagarwal.unscramble.data

import androidx.annotation.Keep

/**
 * Represents the origin of the word set returned by [WordsRepository].
 *
 * Callers (e.g. [com.amanagarwal.unscramble.viewmodels.GameViewModel]) use this to:
 * - Set the `isOffline` flag in [com.amanagarwal.unscramble.viewmodels.GameUiState.Success]
 * - Log the data source for debugging
 *
 * ## Decision tree inside [NetworkWordsRepository.getWords]
 * ```
 * API responds with non-empty list  →  Live   (cache is updated)
 * API fails, in-memory cache exists →  Cached (cache is NOT updated)
 * API fails, cache is empty         →  Static (static allWords list used)
 * ```
 *
 * ## Phase 2 migration note
 * When Room is added, [Cached] will represent words read from the Room database
 * (persisted across process restarts), replacing the current in-memory cache.
 * [Static] remains as the absolute last resort when both API and DB are unavailable.
 */
@Keep
sealed interface WordsResult {
    val words: Set<String>

    /** Words fetched live from the API. The in-memory cache has been updated. */
    @Keep
    data class Live(override val words: Set<String>) : WordsResult

    /**
     * API failed. Words served from the last successful API fetch (in-memory cache).
     * After Phase 2: will represent words read from Room database.
     */
    @Keep
    data class Cached(override val words: Set<String>) : WordsResult

    /**
     * API failed and no cache is available.
     * Serving the built-in static [allWords] list as a last resort.
     */
    @Keep
    data class Static(override val words: Set<String>) : WordsResult
}
