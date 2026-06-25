package com.amanagarwal.unscramble.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amanagarwal.unscramble.BuildConfig
import com.amanagarwal.unscramble.R
import com.amanagarwal.unscramble.data.MAX_NO_OF_WORDS
import com.amanagarwal.unscramble.ui.theme.UnscrambleTheme
import com.amanagarwal.unscramble.viewmodels.GameUiState
import com.amanagarwal.unscramble.viewmodels.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()

    // Silently attempt to restore connectivity whenever this screen is shown
    LaunchedEffect(Unit) {
        gameViewModel.backgroundRefreshIfNeeded()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is GameUiState.Loading -> LoadingScreen(modifier = Modifier.fillMaxSize())
            is GameUiState.Error -> ErrorScreen(
                modifier = Modifier.fillMaxSize(),
                retryAction = { gameViewModel.fetchWords() }
            )
            is GameUiState.Success -> GameContent(
                state = state,
                onUserGuessChanged = { gameViewModel.updateUserGuess(it) },
                onGuessSubmitted = { gameViewModel.checkUserGuess() },
                onSkipClicked = { gameViewModel.skipWord() },
                onPlayAgain = { gameViewModel.resetGame() },
                onDevSkip = { gameViewModel.devSkip() },
                onDevReveal = { gameViewModel.devReveal() },
                userGuess = gameViewModel.userGuess,
                onHome = onHome,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun GameContent(
    state: GameUiState.Success,
    onUserGuessChanged: (String) -> Unit,
    onGuessSubmitted: () -> Unit,
    onSkipClicked: () -> Unit,
    onPlayAgain: () -> Unit,
    onDevSkip: () -> Unit,
    onDevReveal: () -> Unit,
    userGuess: String,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    Column(
        modifier = modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dev Mode Overlay
        if (BuildConfig.DEBUG && state.isDevMode) {
            DevModeInfo(
                state = state,
                onDevSkip = onDevSkip,
                onDevReveal = onDevReveal
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Offline Banner — shown when API is unavailable and fallback words are in use
        AnimatedVisibility(
            visible = state.isOffline,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)),
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300))
        ) {
            OfflineBanner()
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Header (Round and Score)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Round",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${state.currentWordCount} / $MAX_NO_OF_WORDS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // Score card with animated delta chip
            Box(contentAlignment = Alignment.TopCenter) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Score",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.score}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                // Animated score delta chip (+20 / -10) that floats up and fades out
                ScoreDeltaChip(delta = state.scoreChangeDelta)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "UNSCRAMBLE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = state.currentScrambleWord,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                slideInVertically(initialOffsetY = { 40 })).togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "ScrambledWordAnimation"
                ) { targetWord ->
                    Text(
                        text = targetWord.uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().background(Color.Transparent),
                        letterSpacing = 6.sp,
                        lineHeight = 48.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Input Card - Consistent color and transparent input field for blending
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Your answer",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userGuess,
                    onValueChange = onUserGuessChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type here...") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    isError = state.isGuessedWordWrong,
                    supportingText = if (state.isGuessedWordWrong) {
                        { Text(
                            text = stringResource(R.string.wrong_guess_hint),
                            color = MaterialTheme.colorScheme.error
                        ) }
                    } else null,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onGuessSubmitted() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onGuessSubmitted,
                modifier = Modifier
                    .weight(2f)
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Submit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onSkipClicked,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    width = 2.dp
                )
            ) {
                Text(
                    text = "Skip", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (state.isGameOver) {
        GameOverDialog(
            score = state.score,
            onPlayAgain = onPlayAgain,
            onHome = onHome
        )
    }
}

@Composable
fun DevModeInfo(
    state: GameUiState.Success,
    onDevSkip: () -> Unit,
    onDevReveal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEVELOPER TOOLS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ans: ${state.correctWord.uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDevReveal,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Reveal", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onDevSkip,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Skip", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * A subtle banner shown when the app is playing offline (API unavailable).
 * Disappears automatically via AnimatedVisibility when connectivity is restored
 * and [GameViewModel.backgroundRefreshIfNeeded] flips [GameUiState.Success.isOffline] to false.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ ${stringResource(R.string.offline_mode)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ErrorScreen(modifier: Modifier = Modifier, retryAction: () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Something went wrong", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = retryAction) { Text("Retry") }
    }
}

/**
 * A floating chip that briefly shows the score change (+20, -10) above the score card.
 * Appears automatically when [delta] is non-zero, then fades away after 1.2 seconds.
 * This is the visual proof that Bug #1 is fixed — the delta survives long enough for
 * the UI to render it.
 */
@Composable
fun ScoreDeltaChip(delta: Int) {
    var visible by remember { mutableStateOf(false) }

    // Each time a new non-zero delta arrives, show the chip and auto-hide after 1.2s
    LaunchedEffect(delta) {
        if (delta != 0) {
            visible = true
            delay(1200)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + slideInVertically(tween(250)) { it / 2 },
        exit = fadeOut(tween(400)) + slideOutVertically(tween(400)) { -it }
    ) {
        val isPositive = delta > 0
        val chipColor = if (isPositive)
            MaterialTheme.colorScheme.tertiaryContainer
        else
            MaterialTheme.colorScheme.errorContainer

        val textColor = if (isPositive)
            MaterialTheme.colorScheme.tertiary
        else
            MaterialTheme.colorScheme.error

        val label = if (isPositive) "+$delta" else "$delta"

        Box(
            modifier = Modifier
                .background(
                    color = chipColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
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
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) },
        title = { Text(text = "Game Over!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.final_score), style = MaterialTheme.typography.labelLarge)
                Text(text = "$score", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPlayAgain, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text(stringResource(R.string.play_again)) }
                OutlinedButton(
                    onClick = onHome, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(16.dp)
                ) { Text(stringResource(R.string.back_to_home)) }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GameContentPreviewLightMode() {
    UnscrambleTheme {
        GameContent(
            state = GameUiState.Success(
                currentScrambleWord = "scramble",
                currentWordCount = 1,
                score = 20,
                isGuessedWordWrong = false,
                isGameOver = false,
                isDevMode = true,
                correctWord = "scramble"
            ),
            onUserGuessChanged = {},
            onGuessSubmitted = {},
            onSkipClicked = {},
            onPlayAgain = {},
            onDevSkip = {},
            onDevReveal = {},
            userGuess = "",
            onHome = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GameContentPreviewDarkMode() {
    UnscrambleTheme {
        GameContent(
            state = GameUiState.Success(
                currentScrambleWord = "scramble",
                currentWordCount = 1,
                score = 20,
                isGuessedWordWrong = false,
                isGameOver = false,
                isDevMode = true,
                correctWord = "scramble"
            ),
            onUserGuessChanged = {},
            onGuessSubmitted = {},
            onSkipClicked = {},
            onPlayAgain = {},
            onDevSkip = {},
            onDevReveal = {},
            userGuess = "",
            onHome = {}
        )
    }
}
