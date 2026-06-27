package com.amanagarwal.unscramble.domain.usecase

/**
 * Shuffles the characters of a word to produce a scrambled version for display.
 *
 * Guarantees the shuffled result differs from the original by re-shuffling up to 50 times.
 * For very short words (< 2 characters) or words that cannot produce a unique permutation
 * within 50 attempts, the original word is returned unchanged.
 *
 * **Known limitation (Bug #6):** If all 50 attempts produce the same arrangement
 * (e.g. a word with all identical letters), the original unshuffled word is returned.
 * Future fix: detect single-permutation words before returning to the caller.
 *
 * This use case is stateless — safe to unit test on JVM.
 */
class ShuffleWordUseCase {
    operator fun invoke(word: String): String {
        if (word.length < 2) return word
        var shuffled: String
        var attempts = 0
        do {
            shuffled = word.toList().shuffled().joinToString("")
            attempts++
        } while (shuffled == word && attempts < 50)
        return shuffled
    }
}
