package com.amanagarwal.unscramble.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amanagarwal.unscramble.R
import com.amanagarwal.unscramble.viewmodels.GameUiState
import com.amanagarwal.unscramble.viewmodels.GameViewModel

@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is GameUiState.Loading -> LoadingScreen(modifier = Modifier.fillMaxSize())
            is GameUiState.Error -> ErrorScreen(
                modifier = Modifier.fillMaxSize(),
                retryAction = { gameViewModel.fetchWords() }
            )
            is GameUiState.Success -> GameContent(
                state = state,
                gameViewModel = gameViewModel,
                onHome = onHome,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameContent(
    state: GameUiState.Success,
    gameViewModel: GameViewModel,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediumPadding = 16.dp

    Column(
        modifier = modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(mediumPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with Progress and Score
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Word ${state.currentWordCount}/10",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { state.currentWordCount / 10f },
                    modifier = Modifier
                        .width(100.dp)
                        .padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Score: ${state.score}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Scrambled Word Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Unscramble the word",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = state.currentScrambleWord,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                slideInVertically(initialOffsetY = { 40 })).togetherWith(fadeOut(animationSpec = tween(90)))
                    }
                ) { targetWord ->
                    Text(
                        text = targetWord,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.instructions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input Field
        OutlinedTextField(
            value = gameViewModel.userGuess,
            onValueChange = { gameViewModel.updateUserGuess(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            label = { Text("Enter your word") },
            isError = state.isGuessedWordWrong,
            supportingText = {
                if (state.isGuessedWordWrong) {
                    Text(text = "Try again!", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { gameViewModel.checkUserGuess() }),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Actions
        Button(
            onClick = { gameViewModel.checkUserGuess() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Submit Answer", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { gameViewModel.skipWord() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Text("Skip Word", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (state.isGameOver) {
        GameOverDialog(
            score = state.score,
            onPlayAgain = { gameViewModel.resetGame() },
            onHome = onHome
        )
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
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
        Text(
            text = stringResource(R.string.loading_failed), 
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Button(
            onClick = retryAction,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
fun GameOverDialog(
    score: Int,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                Icons.Default.CheckCircle, 
                contentDescription = null, 
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Bravo!",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "You've successfully completed the challenge.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Final Score",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Play Again", style = MaterialTheme.typography.titleMedium)
                }
                
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Back to Home", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    )
}
