package com.amanagarwal.unscramble.domain.usecase

class ShuffleWordUseCase {
    operator fun invoke(word: String): String {
        if (word.length < 2) return word
        var shuffled: String
        var attempts = 0
        do {
            shuffled = word.toList().shuffled().joinToString("")
            attempts++
        } while (shuffled == word && attempts < 50)
        return shuffled
    }
}
