@file:OptIn(ExperimentalMaterial3Api::class)
package com.amanagarwal.unscramble.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amanagarwal.unscramble.ui.screens.GameScreen
import com.amanagarwal.unscramble.ui.screens.StartScreen
import com.amanagarwal.unscramble.viewmodels.GameViewModel

enum class UnscrambleScreen {
    Start,
    Game
}

@Composable
fun UnScrambleApp() {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = UnscrambleScreen.Start.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(route = UnscrambleScreen.Start.name) {
                    StartScreen(
                        onStartButtonClicked = {
                            navController.navigate(UnscrambleScreen.Game.name)
                        }
                    )
                }
                composable(route = UnscrambleScreen.Game.name) {
                    // hiltViewModel() replaces viewModel(factory = ViewModelProvider.Factory)
                    // Hilt handles construction and injection automatically
                    val gameViewModel: GameViewModel = hiltViewModel()

                    GameScreen(
                        gameViewModel = gameViewModel,
                        onHome = {
                            navController.popBackStack(UnscrambleScreen.Start.name, inclusive = false)
                        }
                    )
                }
            }
        }
    }
}
