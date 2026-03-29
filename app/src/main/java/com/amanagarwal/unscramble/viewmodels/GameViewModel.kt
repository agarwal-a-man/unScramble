package com.amanagarwal.unscramble.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanagarwal.unscramble.data.MAX_NO_OF_WORDS
import com.amanagarwal.unscramble.data.WordsRepository
import com.amanagarwal.unscramble.data.allWords
import com.amanagarwal.unscramble.domain.usecase.CalculateScoreUseCase
import com.amanagarwal.unscramble.domain.usecase.ShuffleWordUseCase
import com.amanagarwal.unscramble.domain.usecase.ValidateGuessUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "GameViewModel"

sealed interface GameUiState {
    object Loading : GameUiState
    object Error : GameUiState
    data class Success(
        val currentScrambleWord: String = "",
        val isGuessedWordWrong: Boolean = false,
        val score: Int = 0,
        val currentWordCount: Int = 1,
        val isGameOver: Boolean = false,
        val isDevMode: Boolean = false,
        val correctWord: String = "",
        val isOffline: Boolean = false,
        val availableWordsCount: Int = 0,
        val usedWordsCount: Int = 0,
        val scoreChangeDelta: Int = 0
    ) : GameUiState
}

class GameViewModel(
    private val wordsRepository: WordsRepository,
    private val shuffleWordUseCase: ShuffleWordUseCase,
    private val validateGuessUseCase: ValidateGuessUseCase,
    private val calculateScoreUseCase: CalculateScoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentWord: String = ""
    private var isDevMode: Boolean = false
    private var isOffline: Boolean = false
    private val usedWords = mutableSetOf<String>()
    private var availableWords: Set<String> = emptySet()
    private var fetchJob: Job? = null

    var userGuess by mutableStateOf("")
        private set

    init {
        Log.d(TAG, "GameViewModel initialized")
        fetchWords()
    }

    fun fetchWords() {
        Log.d(TAG, "Fetching words...")
        _uiState.value = GameUiState.Loading
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                val words = wordsRepository.getUnscrambledWord()
                availableWords = words
                isOffline = false
                Log.d(TAG, "Words successfully fetched from repository. Count: ${words.size}")
                resetGameInternal()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching words: ${e.message}. Falling back to local words.", e)
                availableWords = allWords
                isOffline = true
                resetGameInternal()
            }
        }
    }

    private fun resetGameInternal() {
        Log.d(TAG, "Resetting game internal state")
        usedWords.clear()
        userGuess = ""
        val firstWord = pickRandomWordAndShuffle()
        if (firstWord.isBlank()) {
            _uiState.value = GameUiState.Error
            return
        }
        _uiState.value = GameUiState.Success(
            currentScrambleWord = firstWord,
            currentWordCount = 1,
            score = 0,
            isGameOver = false,
            isDevMode = isDevMode,
            correctWord = currentWord,
            isOffline = isOffline,
            availableWordsCount = availableWords.size,
            usedWordsCount = usedWords.size,
            scoreChangeDelta = 0
        )
    }

    fun updateUserGuess(guess: String) {
        _uiState.update { 
            if (it is GameUiState.Success) it.copy(scoreChangeDelta = 0) else it
        }
        if (com.amanagarwal.unscramble.BuildConfig.DEBUG) {
            when (guess) {
                "dev_mode=true" -> {
                    isDevMode = true
                    updateDevModeInState()
                    userGuess = ""
                    Log.d(TAG, "Developer mode enabled")
                }
                "dev_mode=false" -> {
                    isDevMode = false
                    updateDevModeInState()
                    userGuess = ""
                    Log.d(TAG, "Developer mode disabled")
                }
                else -> {
                    userGuess = guess
                }
            }
        } else {
            userGuess = guess
        }
    }
    
    private fun updateDevModeInState() {
        _uiState.update { 
            if (it is GameUiState.Success) it.copy(
                isDevMode = isDevMode,
                correctWord = currentWord,
                isOffline = isOffline,
                availableWordsCount = availableWords.size,
                usedWordsCount = usedWords.size
            ) else it
        }
    }

    fun checkUserGuess() {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Success) return

        when (validateGuessUseCase(userGuess, currentWord)) {
            is ValidateGuessUseCase.Result.Correct -> {
                Log.d(TAG, "Correct guess!")
                val newScore = calculateScoreUseCase.onCorrectGuess(currentState.score)
                updateGameState(newScore, newScore - currentState.score)
            }
            is ValidateGuessUseCase.Result.Incorrect -> {
                Log.d(TAG, "Wrong guess!")
                val newScore = calculateScoreUseCase.onWrongGuess(currentState.score)
                _uiState.update {
                    if (it is GameUiState.Success) it.copy(
                        isGuessedWordWrong = true,
                        score = newScore,
                        scoreChangeDelta = newScore - currentState.score
                    ) else it
                }
            }
            is ValidateGuessUseCase.Result.Empty -> {
                // Do nothing — don't penalize an empty submission
            }
        }
        updateUserGuess("")
    }

    fun skipWord() {
        Log.d(TAG, "Word skipped: $currentWord")
        val currentState = _uiState.value
        if (currentState is GameUiState.Success) {
            val newScore = calculateScoreUseCase.onSkip(currentState.score)
            updateGameState(newScore, newScore - currentState.score)
            updateUserGuess("")
        }
    }

    // Dev mode feature: Skip word without losing score and without incrementing word count if desired?
    // Actually, usually "Skip" just moves to next word. A dev skip might just be "Give me another one".
    fun devSkip() {
        if (!isDevMode) return
        Log.d(TAG, "Dev Skip triggered")
        val nextWord = pickRandomWordAndShuffle()
        if (nextWord.isBlank()) return
        _uiState.update {
            if (it is GameUiState.Success) {
                it.copy(
                    isGuessedWordWrong = false,
                    currentScrambleWord = nextWord,
                    correctWord = currentWord,
                    usedWordsCount = usedWords.size,
                    scoreChangeDelta = 0
                )
            } else it
        }
    }

    fun devReveal() {
        if (!isDevMode) return
        userGuess = currentWord
    }

    private fun pickRandomWordAndShuffle(): String {
        val unused = availableWords - usedWords
        if (unused.isEmpty()) {
            Log.w(TAG, "No more available words!")
            _uiState.update {
                if (it is GameUiState.Success) it.copy(isGameOver = true) else it
            }
            return ""
        }
        currentWord = unused.random()
        usedWords.add(currentWord)
        val shuffled = shuffleWordUseCase(currentWord)
        Log.d(TAG, "Picked word: $currentWord, Shuffled: $shuffled")
        return shuffled
    }

    private fun updateGameState(updatedScore: Int, delta: Int) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Success) return

        if (usedWords.size >= MAX_NO_OF_WORDS) {
            Log.d(TAG, "Game Over. Final Score: $updatedScore")
            _uiState.update { 
                if (it is GameUiState.Success) it.copy(
                    isGameOver = true, 
                    score = updatedScore,
                    scoreChangeDelta = delta
                ) else it
            }
        } else {
            val nextWord = pickRandomWordAndShuffle()
            if (nextWord.isBlank()) {
                _uiState.update {
                    if (it is GameUiState.Success) it.copy(
                        isGameOver = true, 
                        score = updatedScore,
                        scoreChangeDelta = delta
                    ) else it
                }
                return
            }
            Log.d(TAG, "Moving to next word. New Score: $updatedScore, Current Count: ${usedWords.size}")
            _uiState.update {
                if (it is GameUiState.Success) {
                    it.copy(
                        isGuessedWordWrong = false,
                        currentWordCount = it.currentWordCount + 1,
                        score = updatedScore,
                        currentScrambleWord = nextWord,
                        correctWord = currentWord,
                        isDevMode = isDevMode,
                        isOffline = isOffline,
                        usedWordsCount = usedWords.size,
                        scoreChangeDelta = delta
                    )
                } else it
            }
        }
    }

    fun resetGame() {
        Log.d(TAG, "Reset game requested")
        fetchWords()
    }
}
