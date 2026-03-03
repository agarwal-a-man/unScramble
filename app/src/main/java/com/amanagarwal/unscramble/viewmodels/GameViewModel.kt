package com.amanagarwal.unscramble.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amanagarwal.unscramble.data.MAX_NO_OF_WORDS
import com.amanagarwal.unscramble.data.SCORE_INCREASE
import com.amanagarwal.unscramble.data.WordsRepository
import com.amanagarwal.unscramble.data.allWords
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
        val usedWordsCount: Int = 0
    ) : GameUiState
}

class GameViewModel(private val wordsRepository: WordsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentWord: String = ""
    private var isDevMode: Boolean = false
    private var isOffline: Boolean = false
    private val usedWords = mutableSetOf<String>()
    private var availableWords: Set<String> = emptySet()

    var userGuess by mutableStateOf("")
        private set

    init {
        Log.d(TAG, "GameViewModel initialized")
        fetchWords()
    }

    fun fetchWords() {
        Log.d(TAG, "Fetching words...")
        _uiState.value = GameUiState.Loading
        viewModelScope.launch {
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
        _uiState.value = GameUiState.Success(
            currentScrambleWord = firstWord,
            currentWordCount = 1,
            score = 0,
            isGameOver = false,
            isDevMode = isDevMode,
            correctWord = currentWord,
            isOffline = isOffline,
            availableWordsCount = availableWords.size,
            usedWordsCount = usedWords.size
        )
    }

    fun updateUserGuess(guess: String) {
        if (guess == "dev_mode=true") {
            isDevMode = true
            updateDevModeInState()
            userGuess = ""
            Log.d(TAG, "Developer mode enabled")
        } else if (guess == "dev_mode=false") {
            isDevMode = false
            updateDevModeInState()
            userGuess = ""
            Log.d(TAG, "Developer mode disabled")
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

        val guess = userGuess.trim()
        Log.d(TAG, "Checking guess: $guess against target: $currentWord")
        if (guess.equals(currentWord, ignoreCase = true)) {
            Log.d(TAG, "Correct guess!")
            updateGameState(currentState.score + SCORE_INCREASE)
        } else {
            Log.d(TAG, "Wrong guess!")
            val newScore = (currentState.score - 10).coerceAtLeast(0)
            _uiState.update { 
                if (it is GameUiState.Success) it.copy(isGuessedWordWrong = true, score = newScore) else it
            }
        }
        updateUserGuess("")
    }

    fun skipWord() {
        Log.d(TAG, "Word skipped: $currentWord")
        val currentState = _uiState.value
        if (currentState is GameUiState.Success) {
            updateGameState(currentState.score)
            updateUserGuess("")
        }
    }

    // Dev mode feature: Skip word without losing score and without incrementing word count if desired?
    // Actually, usually "Skip" just moves to next word. A dev skip might just be "Give me another one".
    fun devSkip() {
        if (!isDevMode) return
        Log.d(TAG, "Dev Skip triggered")
        val nextWord = pickRandomWordAndShuffle()
        _uiState.update {
            if (it is GameUiState.Success) {
                it.copy(
                    isGuessedWordWrong = false,
                    currentScrambleWord = nextWord,
                    correctWord = currentWord,
                    usedWordsCount = usedWords.size
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
            return ""
        }
        currentWord = unused.random()
        usedWords.add(currentWord)
        val shuffled = shuffleCurrentWord(currentWord)
        Log.d(TAG, "Picked word: $currentWord, Shuffled: $shuffled")
        return shuffled
    }

    private fun shuffleCurrentWord(word: String): String {
        if (word.length < 2) return word
        var shuffled: String
        do {
            shuffled = word.toList().shuffled().joinToString("")
        } while (shuffled == word)
        return shuffled
    }

    private fun updateGameState(updatedScore: Int) {
        val currentState = _uiState.value
        if (currentState !is GameUiState.Success) return

        if (usedWords.size >= MAX_NO_OF_WORDS) {
            Log.d(TAG, "Game Over. Final Score: $updatedScore")
            _uiState.update { 
                if (it is GameUiState.Success) it.copy(isGameOver = true, score = updatedScore) else it
            }
        } else {
            val nextWord = pickRandomWordAndShuffle()
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
                        usedWordsCount = usedWords.size
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
