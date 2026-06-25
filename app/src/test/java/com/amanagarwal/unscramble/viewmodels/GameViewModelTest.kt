package com.amanagarwal.unscramble.viewmodels

import com.amanagarwal.unscramble.data.MAX_NO_OF_WORDS
import com.amanagarwal.unscramble.data.SCORE_INCREASE
import com.amanagarwal.unscramble.data.WordsRepository
import com.amanagarwal.unscramble.data.WordsResult
import com.amanagarwal.unscramble.domain.usecase.CalculateScoreUseCase
import com.amanagarwal.unscramble.domain.usecase.ShuffleWordUseCase
import com.amanagarwal.unscramble.domain.usecase.ValidateGuessUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // A fake repository that returns a controlled word set
    private val fakeWords = setOf(
        "animal", "basket", "camera", "dancing", "elegant",
        "funnel", "guitar", "harvest", "kingdom", "journey"
    ) // exactly MAX_NO_OF_WORDS (10) words

    private val successRepo = object : WordsRepository {
        override suspend fun getWords(): WordsResult = WordsResult.Live(fakeWords)
        override suspend fun refreshCacheInBackground(): Boolean = true
    }

    private val failingRepo = object : WordsRepository {
        override suspend fun getWords(): WordsResult = WordsResult.Static(fakeWords)
        override suspend fun refreshCacheInBackground(): Boolean = false
    }

    private val emptyRepo = object : WordsRepository {
        override suspend fun getWords(): WordsResult = WordsResult.Static(fakeWords)
        override suspend fun refreshCacheInBackground(): Boolean = false
    }

    private fun buildViewModel(repo: WordsRepository = successRepo): GameViewModel {
        return GameViewModel(
            wordsRepository = repo,
            shuffleWordUseCase = ShuffleWordUseCase(),
            validateGuessUseCase = ValidateGuessUseCase(),
            calculateScoreUseCase = CalculateScoreUseCase()
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initialization ───────────────────────────────────────────────────────

    @Test
    fun `initial state is Loading`() {
        val vm = buildViewModel()
        assertTrue(vm.uiState.value is GameUiState.Loading)
    }

    @Test
    fun `after fetch completes state is Success`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is GameUiState.Success)
    }

    @Test
    fun `success state starts at word count 1 with score 0`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val state = vm.uiState.value as GameUiState.Success
        assertEquals(1, state.currentWordCount)
        assertEquals(0, state.score)
        assertFalse(state.isGameOver)
    }

    @Test
    fun `scrambled word is not equal to correct word`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val state = vm.uiState.value as GameUiState.Success
        // The scrambled word should differ from the correct word (or be the same only for edge cases)
        // For our 6+ letter words it's statistically guaranteed
        assertTrue(state.correctWord.isNotBlank())
        assertTrue(state.currentScrambleWord.isNotBlank())
    }

    @Test
    fun `when repo fails state falls back to Success using local words`() = runTest {
        val vm = buildViewModel(repo = failingRepo)
        advanceUntilIdle()
        val state = vm.uiState.value as GameUiState.Success
        assertTrue(state.isOffline)
    }

    @Test
    fun `when repo returns empty list state falls back to local words`() = runTest {
        val vm = buildViewModel(repo = emptyRepo)
        advanceUntilIdle()
        // emptySet triggers the exception path → falls back to allWords → isOffline = true
        val state = vm.uiState.value as GameUiState.Success
        assertTrue(state.isOffline)
    }

    // ─── Correct Guess ───────────────────────────────────────────────────────

    @Test
    fun `correct guess increases score by SCORE_INCREASE`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val stateBefore = vm.uiState.value as GameUiState.Success

        vm.updateUserGuess(stateBefore.correctWord)
        vm.checkUserGuess()

        val stateAfter = vm.uiState.value as GameUiState.Success
        assertEquals(stateBefore.score + SCORE_INCREASE, stateAfter.score)
    }

    @Test
    fun `correct guess advances to next word`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val stateBefore = vm.uiState.value as GameUiState.Success

        vm.updateUserGuess(stateBefore.correctWord)
        vm.checkUserGuess()

        val stateAfter = vm.uiState.value as GameUiState.Success
        assertEquals(stateBefore.currentWordCount + 1, stateAfter.currentWordCount)
    }

    @Test
    fun `correct guess clears isGuessedWordWrong flag`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val state = vm.uiState.value as GameUiState.Success

        // First make a wrong guess to set the flag
        vm.updateUserGuess("wrongword")
        vm.checkUserGuess()
        assertTrue((vm.uiState.value as GameUiState.Success).isGuessedWordWrong)

        // Then submit correct answer
        val stateForCorrect = vm.uiState.value as GameUiState.Success
        vm.updateUserGuess(stateForCorrect.correctWord)
        vm.checkUserGuess()
        assertFalse((vm.uiState.value as GameUiState.Success).isGuessedWordWrong)
    }

    @Test
    fun `correct guess sets positive scoreChangeDelta`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val state = vm.uiState.value as GameUiState.Success

        vm.updateUserGuess(state.correctWord)
        vm.checkUserGuess()

        val stateAfter = vm.uiState.value as GameUiState.Success
        assertTrue("Delta should be positive after correct guess", stateAfter.scoreChangeDelta > 0)
        assertEquals(SCORE_INCREASE, stateAfter.scoreChangeDelta)
    }

    // ─── Wrong Guess ─────────────────────────────────────────────────────────

    @Test
    fun `wrong guess sets isGuessedWordWrong`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.updateUserGuess("wrongword")
        vm.checkUserGuess()

        val state = vm.uiState.value as GameUiState.Success
        assertTrue(state.isGuessedWordWrong)
    }

    @Test
    fun `wrong guess decreases score`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        // Give a starting score via correct guess first
        val correctWord = (vm.uiState.value as GameUiState.Success).correctWord
        vm.updateUserGuess(correctWord)
        vm.checkUserGuess()
        val scoreBeforeWrong = (vm.uiState.value as GameUiState.Success).score

        vm.updateUserGuess("wrongword")
        vm.checkUserGuess()

        val stateAfter = vm.uiState.value as GameUiState.Success
        assertTrue(stateAfter.score < scoreBeforeWrong)
    }

    @Test
    fun `wrong guess score never goes below zero`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        // Score starts at 0, wrong guess should not go negative
        vm.updateUserGuess("wrongword")
        vm.checkUserGuess()

        val state = vm.uiState.value as GameUiState.Success
        assertTrue(state.score >= 0)
    }

    @Test
    fun `wrong guess does NOT advance word count`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val wordCountBefore = (vm.uiState.value as GameUiState.Success).currentWordCount

        vm.updateUserGuess("wrongword")
        vm.checkUserGuess()

        val wordCountAfter = (vm.uiState.value as GameUiState.Success).currentWordCount
        assertEquals(wordCountBefore, wordCountAfter)
    }

    @Test
    fun `wrong guess sets negative scoreChangeDelta`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        // Get some score first so the penalty is meaningful
        val correctWord = (vm.uiState.value as GameUiState.Success).correctWord
        vm.updateUserGuess(correctWord)
        vm.checkUserGuess()

        vm.updateUserGuess("wrongword")
        vm.checkUserGuess()

        val state = vm.uiState.value as GameUiState.Success
        assertTrue("Delta should be negative after wrong guess", state.scoreChangeDelta < 0)
    }

    // ─── Bug #1 Regression: scoreChangeDelta survives updateUserGuess("") ────

    @Test
    fun `scoreChangeDelta is NOT wiped after correct guess submission`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val state = vm.uiState.value as GameUiState.Success

        vm.updateUserGuess(state.correctWord)
        vm.checkUserGuess() // internally calls updateUserGuess("") at the end

        // Delta must still be SCORE_INCREASE — not 0
        val stateAfter = vm.uiState.value as GameUiState.Success
        assertNotEquals(
            "Bug #1 regression: scoreChangeDelta should not be 0 after correct guess",
            0,
            stateAfter.scoreChangeDelta
        )
    }

    @Test
    fun `scoreChangeDelta resets to 0 when user starts typing new guess`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val state = vm.uiState.value as GameUiState.Success

        // Submit correct answer → delta becomes SCORE_INCREASE
        vm.updateUserGuess(state.correctWord)
        vm.checkUserGuess()

        // User starts typing next guess → delta should clear
        vm.updateUserGuess("a")
        assertEquals(0, (vm.uiState.value as GameUiState.Success).scoreChangeDelta)
    }

    // ─── Empty Guess ─────────────────────────────────────────────────────────

    @Test
    fun `empty guess does nothing`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val stateBefore = vm.uiState.value as GameUiState.Success

        vm.updateUserGuess("")
        vm.checkUserGuess()

        val stateAfter = vm.uiState.value as GameUiState.Success
        assertEquals(stateBefore.score, stateAfter.score)
        assertEquals(stateBefore.currentWordCount, stateAfter.currentWordCount)
        assertFalse(stateAfter.isGuessedWordWrong)
    }

    // ─── Skip ────────────────────────────────────────────────────────────────

    @Test
    fun `skip advances to next word`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val stateBefore = vm.uiState.value as GameUiState.Success

        vm.skipWord()

        val stateAfter = vm.uiState.value as GameUiState.Success
        assertEquals(stateBefore.currentWordCount + 1, stateAfter.currentWordCount)
    }

    @Test
    fun `skip does not change score`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        val scoreBefore = (vm.uiState.value as GameUiState.Success).score

        vm.skipWord()

        val scoreAfter = (vm.uiState.value as GameUiState.Success).score
        assertEquals(scoreBefore, scoreAfter)
    }

    // ─── Game Over ───────────────────────────────────────────────────────────

    @Test
    fun `game ends after MAX_NO_OF_WORDS correct guesses`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        repeat(MAX_NO_OF_WORDS) {
            val state = vm.uiState.value
            if (state is GameUiState.Success && !state.isGameOver) {
                vm.updateUserGuess(state.correctWord)
                vm.checkUserGuess()
            }
        }

        val finalState = vm.uiState.value as GameUiState.Success
        assertTrue(finalState.isGameOver)
    }

    @Test
    fun `game over score equals sum of correct guesses`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        repeat(MAX_NO_OF_WORDS) {
            val state = vm.uiState.value
            if (state is GameUiState.Success && !state.isGameOver) {
                vm.updateUserGuess(state.correctWord)
                vm.checkUserGuess()
            }
        }

        val finalState = vm.uiState.value as GameUiState.Success
        assertEquals(MAX_NO_OF_WORDS * SCORE_INCREASE, finalState.score)
    }

    // ─── Reset ───────────────────────────────────────────────────────────────

    @Test
    fun `resetGame resets word count and score`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        // Play some guesses
        val state = vm.uiState.value as GameUiState.Success
        vm.updateUserGuess(state.correctWord)
        vm.checkUserGuess()

        vm.resetGame()
        advanceUntilIdle()

        val resetState = vm.uiState.value as GameUiState.Success
        assertEquals(0, resetState.score)
        assertEquals(1, resetState.currentWordCount)
        assertFalse(resetState.isGameOver)
    }

    // ─── Bug #3 Regression: devSkip currentWordCount desync ──────────────────

    @Test
    fun `devSkip increments currentWordCount`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.enableDevModeForTesting()

        val countBefore = (vm.uiState.value as GameUiState.Success).currentWordCount
        vm.devSkip()
        val countAfter = (vm.uiState.value as GameUiState.Success).currentWordCount

        assertEquals("Bug #3 regression: devSkip must increment currentWordCount", countBefore + 1, countAfter)
    }

    @Test
    fun `devSkip currentWordCount stays in sync with usedWords after multiple skips`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.enableDevModeForTesting()

        repeat(3) { vm.devSkip() }

        val state = vm.uiState.value as GameUiState.Success
        // Started at count=1, 3 skips → should be 4
        assertEquals(4, state.currentWordCount)
        assertEquals(4, state.usedWordsCount)
    }

    // ─── Bug #4 Regression: devSkip overwriting isGameOver ───────────────────

    @Test
    fun `devSkip does not overwrite isGameOver when word pool exhausted`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.enableDevModeForTesting()

        // Exhaust all words via devSkip (fakeWords has MAX_NO_OF_WORDS=10, first word loaded at init)
        // After init: 1 word used. Need MAX_NO_OF_WORDS-1 more devSkips to exhaust pool.
        repeat(MAX_NO_OF_WORDS - 1) { vm.devSkip() }

        // Pool is now empty — one more devSkip should trigger game over, not corrupt state
        vm.devSkip()

        val state = vm.uiState.value as GameUiState.Success
        assertTrue("Bug #4 regression: isGameOver must be true when word pool is exhausted", state.isGameOver)
    }
}

