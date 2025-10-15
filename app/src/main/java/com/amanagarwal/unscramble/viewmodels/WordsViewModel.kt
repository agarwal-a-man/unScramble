package com.amanagarwal.unscramble.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amanagarwal.unscramble.WordsApplication
import com.amanagarwal.unscramble.data.WordsRepository
import com.amanagarwal.unscramble.data.allWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WordsViewModel(private val wordRepository: WordsRepository) : ViewModel() {
    sealed interface ApiUiState{
        data class Offline(val words : Set<String>): ApiUiState
        data class Success(val words : Set<String>):ApiUiState
        object Error : ApiUiState
        object Loading : ApiUiState
    }

    private val _apiUiState = MutableStateFlow<ApiUiState>(ApiUiState.Loading)
    val apiUiState : StateFlow<ApiUiState> = _apiUiState.asStateFlow()

    init{
        fetchWords()
    }
    fun fetchWords(){
        viewModelScope.launch{
            _apiUiState.value = ApiUiState.Loading
            try{
                val words = wordRepository.getUnscrambledWord()
                _apiUiState.value = ApiUiState.Success(words)
                Log.d("WordsViewModel", "Success:")
            }catch(e:Exception){
                _apiUiState.value = ApiUiState.Offline(allWords)
                Log.d("WordsViewModel", "Offline:")
            }
        }
    }
//    companion object {
//        val Factory: ViewModelProvider.Factory = viewModelFactory {
//            initializer {
//                val application = (this[APPLICATION_KEY] as WordsApplication)
//                val repo = application.container.wordsRepository
//                WordsViewModel(wordRepository = repo)
//            }
//        }
//    }
}
