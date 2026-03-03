package com.amanagarwal.unscramble.viewmodels

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amanagarwal.unscramble.WordsApplication

object ViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            GameViewModel(application().container.wordsRepository)
        }
    }
}

fun CreationExtras.application(): WordsApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as WordsApplication)
