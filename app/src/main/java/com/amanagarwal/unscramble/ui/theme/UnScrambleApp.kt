@file:OptIn(ExperimentalMaterial3Api::class)
package com.amanagarwal.unscramble.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amanagarwal.unscramble.ui.screens.GameScreen
import com.amanagarwal.unscramble.ui.screens.StartScreen
import com.amanagarwal.unscramble.viewmodels.GameViewModel
import com.amanagarwal.unscramble.viewmodels.ViewModelProvider

enum class UnscrambleScreen {
    Start,
    Game
}

@Composable
fun UnScrambleApp() {
    val navController = rememberNavController()

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
                val gameViewModel: GameViewModel = viewModel(factory = ViewModelProvider.Factory)

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
