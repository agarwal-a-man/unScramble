package com.amanagarwal.unscramble.ui
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

private lateinit var currentWord :String
private var usedWords : MutableSet<String> = mutableSetOf()
private lateinit var newWords : Set<String>


sealed interface ApiUiState{
    data class Success(val words:Set<String>):ApiUiState
    object Error:ApiUiState
    object Loading:ApiUiState
}


class GameViewModel(private val wordRepository: WordsRepository):ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var userGuess by mutableStateOf("")
        private set
    var apiUiState: ApiUiState by mutableStateOf(ApiUiState.Loading)
        private set

    init {
        getWord()
        setWords()
        resetGame()
    }

    fun getWord() {
        viewModelScope.launch {
            apiUiState = ApiUiState.Loading
            try {
                apiUiState = ApiUiState.Success(wordRepository.getUnscrambledWord())
            } catch (e: IOException) {
                apiUiState = ApiUiState.Error
                ApiUiState.Success(allWords)
                Log.e("error", "getWord: ")
            } catch (e: HttpException) {
                apiUiState = ApiUiState.Error
                ApiUiState.Success(allWords)
                Log.e("error", "getWord: ")
            }
        }
    }
    fun setWords(){
        when(apiUiState){
            is ApiUiState.Success -> {
                newWords = (apiUiState as ApiUiState.Success).words
            }
            else -> {
                newWords = allWords
            }
        }
    }


    fun resetGame() {
        usedWords.clear()
        _uiState.value = GameUiState(currentScrambleWord = pickRandomWordAndShuffle())
    }


    fun updateUserGuess(guessWord: String) {
        userGuess = guessWord
    }


    fun pickRandomWordAndShuffle(): String {
        currentWord = newWords.random()
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
        userGuess=userGuess.trim()
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

    companion object{
            val Factory: ViewModelProvider.Factory = viewModelFactory {
                initializer {
                    val application = (this[APPLICATION_KEY] as WordsApplication)
                    val wordRepository = application.container.wordsRepository
                    GameViewModel(wordRepository = wordRepository)
                }
            }
        }
}
