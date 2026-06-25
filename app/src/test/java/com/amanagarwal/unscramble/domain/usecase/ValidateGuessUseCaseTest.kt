package com.amanagarwal.unscramble.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ValidateGuessUseCaseTest {

    private lateinit var useCase: ValidateGuessUseCase

    @Before
    fun setUp() {
        useCase = ValidateGuessUseCase()
    }

    @Test
    fun `correct guess returns Correct`() {
        val result = useCase("animal", "animal")
        assertEquals(ValidateGuessUseCase.Result.Correct, result)
    }

    @Test
    fun `correct guess is case insensitive`() {
        assertEquals(ValidateGuessUseCase.Result.Correct, useCase("ANIMAL", "animal"))
        assertEquals(ValidateGuessUseCase.Result.Correct, useCase("Animal", "animal"))
        assertEquals(ValidateGuessUseCase.Result.Correct, useCase("aNiMaL", "animal"))
    }

    @Test
    fun `correct guess trims whitespace`() {
        assertEquals(ValidateGuessUseCase.Result.Correct, useCase("  animal  ", "animal"))
        assertEquals(ValidateGuessUseCase.Result.Correct, useCase("animal ", "animal"))
    }

    @Test
    fun `wrong guess returns Incorrect`() {
        val result = useCase("cat", "animal")
        assertEquals(ValidateGuessUseCase.Result.Incorrect, result)
    }

    @Test
    fun `empty string returns Empty`() {
        assertEquals(ValidateGuessUseCase.Result.Empty, useCase("", "animal"))
    }

    @Test
    fun `blank whitespace-only guess returns Empty`() {
        assertEquals(ValidateGuessUseCase.Result.Empty, useCase("   ", "animal"))
    }

    @Test
    fun `partial match returns Incorrect`() {
        assertEquals(ValidateGuessUseCase.Result.Incorrect, useCase("anim", "animal"))
    }
}
