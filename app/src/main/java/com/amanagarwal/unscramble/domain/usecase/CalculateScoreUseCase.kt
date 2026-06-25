package com.amanagarwal.unscramble.domain.usecase

import com.amanagarwal.unscramble.data.SCORE_INCREASE

/**
 * Encapsulates all scoring arithmetic for the game.
 *
 * Keeping scoring here (not in the ViewModel) means:
 * - Rules can be changed in one place without touching UI or state management.
 * - Rules are unit-testable without Android dependencies.
 *
 * Current rules:
 * - Correct guess → +[SCORE_INCREASE] (default: +20)
 * - Wrong guess   → -10, floored at 0 (score can never go negative)
 * - Skip          → no change (no penalty for skipping)
 *
 * To change scoring: edit this file and update [SCORE_INCREASE] in `WordsData.kt`.
 */
class CalculateScoreUseCase {

    /** Awards points for a correct guess. */
    fun onCorrectGuess(currentScore: Int): Int {
        return currentScore + SCORE_INCREASE
    }

    /**
     * Deducts points for a wrong guess.
     * Score is floored at 0 — the player can never have a negative score.
     */
    fun onWrongGuess(currentScore: Int): Int {
        return (currentScore - 10).coerceAtLeast(0)
    }

    /** Returns the score unchanged — skipping carries no penalty. */
    fun onSkip(currentScore: Int): Int {
        return currentScore
    }
}
