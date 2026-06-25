package com.amanagarwal.unscramble.domain.usecase

import com.amanagarwal.unscramble.data.SCORE_INCREASE
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateScoreUseCaseTest {

    private lateinit var useCase: CalculateScoreUseCase

    @Before
    fun setUp() {
        useCase = CalculateScoreUseCase()
    }

    @Test
    fun `onCorrectGuess increases score by SCORE_INCREASE`() {
        assertEquals(SCORE_INCREASE, useCase.onCorrectGuess(0))
        assertEquals(100 + SCORE_INCREASE, useCase.onCorrectGuess(100))
    }

    @Test
    fun `onWrongGuess decreases score by 10`() {
        assertEquals(90, useCase.onWrongGuess(100))
        assertEquals(10, useCase.onWrongGuess(20))
    }

    @Test
    fun `onWrongGuess never goes below zero`() {
        assertEquals(0, useCase.onWrongGuess(0))
        assertEquals(0, useCase.onWrongGuess(5))
        assertEquals(0, useCase.onWrongGuess(9))
    }

    @Test
    fun `onSkip returns same score`() {
        assertEquals(0, useCase.onSkip(0))
        assertEquals(60, useCase.onSkip(60))
        assertEquals(200, useCase.onSkip(200))
    }
}
