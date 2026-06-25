package com.amanagarwal.unscramble.data

/**
 * Represents the source of the word list returned by the repository.
 * Allows the ViewModel and UI to know whether words are fresh, cached, or static.
 *
 * When Room is added in Phase 2:
 *  - [Cached] will represent words read from the Room DB (persisted across restarts)
 *  - [Static] stays as the absolute last resort (no DB, no API)
 */
sealed interface WordsResult {
    val words: Set<String>

    /** Words fetched live from the API. Cache has been updated. */
    data class Live(override val words: Set<String>) : WordsResult

    /** API failed. Words served from the last successful in-memory API fetch. */
    data class Cached(override val words: Set<String>) : WordsResult

    /** API failed and no cache available. Serving the built-in static word list. */
    data class Static(override val words: Set<String>) : WordsResult
}
