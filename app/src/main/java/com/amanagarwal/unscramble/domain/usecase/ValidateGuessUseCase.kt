package com.amanagarwal.unscramble.domain.usecase

/**
 * Validates the player's guess against the correct word.
 *
 * Comparison is case-insensitive and trims leading/trailing whitespace.
 *
 * Returns a [Result] sealed class:
 * - [Result.Correct]   — trimmed guess matches the correct word (ignoring case)
 * - [Result.Incorrect] — trimmed guess is non-blank but does not match
 * - [Result.Empty]     — guess is blank; the caller should not penalise this
 *
 * This use case is stateless and has no Android dependencies — safe to unit test on JVM.
 */
class ValidateGuessUseCase {

    sealed interface Result {
        object Correct : Result
        object Incorrect : Result
        object Empty : Result
    }

    operator fun invoke(guess: String, correctWord: String): Result {
        val trimmed = guess.trim()
        if (trimmed.isBlank()) return Result.Empty
        return if (trimmed.equals(correctWord, ignoreCase = true)) {
            Result.Correct
        } else {
            Result.Incorrect
        }
    }
}
