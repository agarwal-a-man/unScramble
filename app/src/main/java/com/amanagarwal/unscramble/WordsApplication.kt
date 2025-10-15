package com.amanagarwal.unscramble

import android.app.Application
import com.amanagarwal.unscramble.data.AppContainer
import com.amanagarwal.unscramble.data.DefaultAppContainer

class WordsApplication: Application(){
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}