package com.amanagarwal.unscramble.ui
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.amanagarwal.unscramble.data.MAX_NO_OF_WORDS
import com.amanagarwal.unscramble.data.SCORE_INCREASE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.amanagarwal.unscramble.data.allWords
import kotlinx.coroutines.flow.update

private lateinit var currentWord :String
private var usedWords : MutableSet<String> = mutableSetOf()



class GameViewModel:ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var userGuess by mutableStateOf("")
        private set

    init {
        resetGame()
    }

    fun resetGame() {
        usedWords.clear()
        _uiState.value = GameUiState(currentScrambleWord = pickRandomWordAndShuffle())
    }


    fun updateUserGuess(guessWord: String) {
        userGuess = guessWord
    }

    private fun pickRandomWordAndShuffle(): String {
        currentWord = allWords.random()
        if (usedWords.contains(currentWord)) {
            return pickRandomWordAndShuffle()
        } else {
            usedWords.add(currentWord)
            return shuffleCurrentWord(currentWord)
        }
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        tempWord.shuffle()
        if (String(tempWord) == word) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }
    fun skipWord(){
        updateGameState(_uiState.value.score)
        updateUserGuess("")
    }

    fun checkUserGuess(){
        if(userGuess.equals(currentWord,ignoreCase = true)){
            val updatedScore = _uiState.value.score.plus(SCORE_INCREASE)
            updateGameState(updatedScore)
        }
        else{
            var minusScore=0
            if(_uiState.value.score>0) {
                 minusScore = 10
            }
            _uiState.update{
                currentState->currentState.copy(isGuessedWordWrong = true,
                    score=_uiState.value.score-minusScore)
            }
        }
        updateUserGuess("")
    }
    private fun updateGameState(updatedScore:Int){
        if(usedWords.size == MAX_NO_OF_WORDS){
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    score = updatedScore,
                    isGameOver=true,
                )
            }
        }

        else{
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    currentWordCount = currentState.currentWordCount.inc(),
                    score = updatedScore,
                    currentScrambleWord = pickRandomWordAndShuffle(),
                )
            }
        }
    }
}
