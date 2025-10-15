package com.amanagarwal.unscramble.viewmodels
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amanagarwal.unscramble.WordsApplication
import com.amanagarwal.unscramble.data.MAX_NO_OF_WORDS
import com.amanagarwal.unscramble.data.SCORE_INCREASE
import com.amanagarwal.unscramble.data.WordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.amanagarwal.unscramble.data.allWords
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException


class GameViewModel : ViewModel() {

    private var currentWord: String = ""
    private val usedWords = mutableSetOf<String>()
    private var availableWords: Set<String> = emptySet()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var userGuess by mutableStateOf("")
        private set

    fun setWords(words: Set<String>) {
        availableWords = words
        usedWords.clear()
        _uiState.value = GameUiState(currentScrambleWord = pickRandomWordAndShuffle())
    }

    fun updateUserGuess(guess: String) {
        userGuess = guess
    }

    fun checkUserGuess() {
        val guess = userGuess.trim()
        if (guess.equals(currentWord, ignoreCase = true)) {
            updateGameState(_uiState.value.score + SCORE_INCREASE)
        } else {
            val newScore = (_uiState.value.score - 10).coerceAtLeast(0)
            _uiState.update { it.copy(isGuessedWordWrong = true, score = newScore) }
        }
        updateUserGuess("")
    }

    fun skipWord() {
        updateGameState(_uiState.value.score)
        updateUserGuess("")
    }

    private fun pickRandomWordAndShuffle(): String {
        val unused = availableWords - usedWords
        if (unused.isEmpty()) return ""
        currentWord = unused.random()
        usedWords.add(currentWord)
        return shuffleCurrentWord(currentWord)
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
        if (usedWords.size >= MAX_NO_OF_WORDS) {
            _uiState.update { it.copy(isGameOver = true, score = updatedScore) }
        } else {
            _uiState.update {
                it.copy(
                    isGuessedWordWrong = false,
                    currentWordCount = it.currentWordCount + 1,
                    score = updatedScore,
                    currentScrambleWord = pickRandomWordAndShuffle()
                )
            }
        }
    }

    fun resetGame() {
        // clear used words and UI state but don't fetch — fetching is handled by WordsViewModel via refreshKey
        usedWords.clear()
        userGuess = ""
        _uiState.value = GameUiState() // resets score/currentWordCount/isGameOver etc.
        // NOTE: do NOT call setWords() here. setWords(...) will be invoked by Reset's LaunchedEffect
    }

}

data class GameUiState(
    val currentScrambleWord: String = "",
    val isGuessedWordWrong: Boolean = false,
    val score: Int = 0,
    val currentWordCount: Int = 1,
    val isGameOver: Boolean = false
)
