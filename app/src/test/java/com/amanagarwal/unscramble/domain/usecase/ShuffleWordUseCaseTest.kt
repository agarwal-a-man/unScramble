package com.amanagarwal.unscramble.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShuffleWordUseCaseTest {

    private lateinit var useCase: ShuffleWordUseCase

    @Before
    fun setUp() {
        useCase = ShuffleWordUseCase()
    }

    @Test
    fun `result contains same letters as input`() {
        val word = "elephant"
        val shuffled = useCase(word)
        assertEquals(word.toList().sorted(), shuffled.toList().sorted())
    }

    @Test
    fun `single char word returned as-is`() {
        assertEquals("a", useCase("a"))
    }

    @Test
    fun `two char word with different letters is shuffled`() {
        // "ab" must become "ba" since only one other permutation exists
        val results = (1..20).map { useCase("ab") }.toSet()
        assertTrue("Should produce both permutations", results.size > 1 || results.first() == "ba")
    }

    @Test
    fun `shuffled result has same length as input`() {
        val word = "keyboard"
        assertEquals(word.length, useCase(word).length)
    }

    @Test
    fun `result differs from original for sufficiently long unique-letter word`() {
        // Run 10 times — statistically near-impossible to always get same order for an 8-letter word
        val word = "keyboard"
        val atLeastOnceDifferent = (1..10).any { useCase(word) != word }
        assertTrue("Shuffle should produce a different order at least once", atLeastOnceDifferent)
    }

    @Test
    fun `empty string returns empty string`() {
        assertEquals("", useCase(""))
    }
}
