package com.amanagarwal.unscramble.domain.usecase

import com.amanagarwal.unscramble.data.SCORE_INCREASE

class CalculateScoreUseCase {

    fun onCorrectGuess(currentScore: Int): Int {
        return currentScore + SCORE_INCREASE
    }

    fun onWrongGuess(currentScore: Int): Int {
        return (currentScore - 10).coerceAtLeast(0)
    }

    fun onSkip(currentScore: Int): Int {
        return currentScore // no penalty for skipping
    }
}
