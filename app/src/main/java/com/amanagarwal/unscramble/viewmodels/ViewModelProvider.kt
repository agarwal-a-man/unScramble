package com.amanagarwal.unscramble.viewmodels

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amanagarwal.unscramble.WordsApplication
import com.amanagarwal.unscramble.domain.usecase.CalculateScoreUseCase
import com.amanagarwal.unscramble.domain.usecase.ShuffleWordUseCase
import com.amanagarwal.unscramble.domain.usecase.ValidateGuessUseCase

object ViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            GameViewModel(
                wordsRepository = application().container.wordsRepository,
                shuffleWordUseCase = ShuffleWordUseCase(),
                validateGuessUseCase = ValidateGuessUseCase(),
                calculateScoreUseCase = CalculateScoreUseCase()
            )
        }
    }
}

fun CreationExtras.application(): WordsApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as WordsApplication)
