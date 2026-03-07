package com.amanagarwal.unscramble.domain.usecase

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
