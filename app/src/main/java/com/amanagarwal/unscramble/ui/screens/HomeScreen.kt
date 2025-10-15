package com.amanagarwal.unscramble.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amanagarwal.unscramble.R
import com.amanagarwal.unscramble.viewmodels.GameViewModel
import com.amanagarwal.unscramble.viewmodels.WordsViewModel

@Composable
fun HomeScreen(
    apiUiState: WordsViewModel.ApiUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    retryAction: () -> Unit,
    gameViewModel: GameViewModel,
    refreshKey: Int,
    onRequestRefresh: () -> Unit, // not strictly required if onPlayAgain increments refreshKey
    onPlayAgain: () -> Unit
) {
    // This handles fetching & wiring words -> GameViewModel when apiUiState or refreshKey change.
    Reset(
        apiUiState = apiUiState,
        modifier = modifier,
        retryAction = retryAction,
        gameViewModel = gameViewModel,
        refreshKey = refreshKey
    )

    // Main game UI (FinalScoreDialog inside GameScreen will call onPlayAgain)
    GameScreen(
        gameViewModel = gameViewModel,
        onPlayAgain = onPlayAgain,
        contentPadding = contentPadding
    )
}

@Composable
fun Reset(
    apiUiState: WordsViewModel.ApiUiState,
    modifier: Modifier = Modifier,
    retryAction: () -> Unit,
    gameViewModel: GameViewModel,
    refreshKey: Int
) {
    // Show loading or error as usual
    when (apiUiState) {
        is WordsViewModel.ApiUiState.Loading -> {
            LoadingScreen(modifier = modifier.fillMaxSize())
        }

        is WordsViewModel.ApiUiState.Error -> {
            ErrorScreen(modifier = modifier.fillMaxSize(), retryAction = retryAction)
        }

        is WordsViewModel.ApiUiState.Success -> {
            // Whenever Success words change, set them into GameViewModel
            LaunchedEffect(apiUiState.words) {
                gameViewModel.setWords(apiUiState.words)
            }
                Log.d("HomeScreen", "Online")
        }

        is WordsViewModel.ApiUiState.Offline -> {
            LaunchedEffect(apiUiState.words) {
                gameViewModel.setWords(apiUiState.words)

            }
            Log.d("HomeScreen", "Offline")
        }
    }

    // When refreshKey changes (user requested a refresh), trigger a fetch.
    // Important: don't set words here — setting will happen in the LaunchedEffect that watches apiUiState.
    LaunchedEffect(key1 = refreshKey) {
        if (refreshKey > 0) {
            retryAction()
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(200.dp),
            painter = painterResource(R.drawable.loading_img),
            contentDescription = stringResource(R.string.loading),
        )
    }
}

@Composable
fun ErrorScreen(modifier: Modifier = Modifier, retryAction: () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_connection_error),
            contentDescription = stringResource(R.string.loading_failed)
        )
        Text(text = stringResource(R.string.loading_failed), modifier = Modifier.padding(16.dp))
        Button(onClick = retryAction) {
            Text(text = stringResource(R.string.retry))
        }
    }
}
