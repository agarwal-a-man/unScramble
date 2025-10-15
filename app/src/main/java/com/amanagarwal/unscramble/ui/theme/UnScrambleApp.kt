@file:OptIn(ExperimentalMaterial3Api::class)
package com.amanagarwal.unscramble.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amanagarwal.unscramble.ui.screens.HomeScreen
import com.amanagarwal.unscramble.viewmodels.GameViewModel
import com.amanagarwal.unscramble.viewmodels.ViewModelProvider
import com.amanagarwal.unscramble.viewmodels.WordsViewModel

@Composable
fun UnScrambleApp() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // refreshKey increments cause a new fetch cycle
    val refreshKey = remember { mutableStateOf(0) }

    val wordsViewModel: WordsViewModel =
        viewModel(factory = ViewModelProvider.Factory)
    val gameViewModel: GameViewModel = viewModel()

    // collect api state safely for recomposition
    val apiUiState by wordsViewModel.apiUiState.collectAsState()

    // define what 'Play Again' does: reset UI state immediately and request new words
    val onPlayAgain = {
        gameViewModel.resetGame()        // clear score/progress immediately
        refreshKey.value = refreshKey.value + 1 // trigger fetch of new words
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { /* your top app bar here */ }
    ) { innerPadding ->
        HomeScreen(
            apiUiState = apiUiState,
            contentPadding = innerPadding,
            retryAction = wordsViewModel::fetchWords,
            gameViewModel = gameViewModel,
            refreshKey = refreshKey.value,
            onRequestRefresh = { refreshKey.value = refreshKey.value + 1 },
            onPlayAgain = onPlayAgain
        )
    }
}
