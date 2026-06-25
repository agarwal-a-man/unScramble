# unScramble 🔤

An Android word-unscrambling game built with modern Android architecture patterns.
Fetch words from a live API, unscramble them, score points. Clean Architecture, Hilt DI, Jetpack Compose.

---

## Table of Contents
1. [Features](#features)
2. [Architecture](#architecture)
3. [Module Structure](#module-structure)
4. [Build & Run](#build--run)
5. [Testing](#testing)
6. [Developer Guide](#developer-guide)
7. [Roadmap](#roadmap)
8. [Known Issues / Backlog](#known-issues--backlog)

---

## Features

| Feature | Status |
|---------|--------|
| Fetch live words from API | ✅ |
| Offline fallback (cache → static list) | ✅ |
| Background cache refresh when connectivity restores | ✅ |
| Score system (+20 correct / -10 wrong) | ✅ |
| Animated score delta chip (+20 / -10) | ✅ |
| Word shuffle animation | ✅ |
| Skip word (with score penalty) | ✅ |
| Game Over dialog with performance message | ✅ |
| Wrong-guess text feedback | ✅ |
| Offline banner (auto-dismisses on reconnect) | ✅ |
| Developer mode (reveal answer, skip, live stats) | ✅ (debug builds only) |
| Dark / Light theme | ✅ |
| PvP (same device or online via WebSocket) | 🗓 Phase 4/5 |
| Login / history | 🗓 Phase 2/3 |
| Themed word modes | 🗓 Phase 6 |

---

## Architecture

The app follows **Clean Architecture** with three distinct layers:

```
UI Layer (Compose)
    │
    ▼
ViewModel Layer (GameViewModel + GameUiState)
    │
    ▼
Domain Layer (Use Cases — pure business logic, no Android deps)
    │
    ▼
Data Layer (Repository + API + in-memory cache)
```

### Data flow

```
User taps Submit
  → GameScreen calls gameViewModel.checkUserGuess()
  → ValidateGuessUseCase compares guess to currentWord
  → CalculateScoreUseCase computes new score
  → GameViewModel emits new GameUiState.Success via StateFlow
  → GameScreen recomposes
```

### Dependency injection

All dependencies wired by **Hilt** (migrated from manual AppContainer in Phase 1).

```
SingletonComponent
  NetworkModule     →  Json, OkHttpClient (15s timeout), Retrofit, WordsApiService
  RepositoryModule  →  @Binds WordsRepository → NetworkWordsRepository

ViewModelComponent
  UseCaseModule     →  ShuffleWordUseCase, ValidateGuessUseCase, CalculateScoreUseCase
                       (one instance per ViewModel lifetime — ViewModelScoped)
```

### Offline strategy (stale-while-revalidate)

```
fetchWords() called
  ├─ API responds  →  Live words returned + in-memory cache updated
  └─ API fails
       ├─ cache non-empty  →  Cached words (isOffline = true, banner shown)
       └─ cache empty      →  Static allWords (isOffline = true, banner shown)

backgroundRefreshIfNeeded()  [called by LaunchedEffect in GameScreen]
  → Only runs when isOffline = true
  → Silently retries API in background, no loading state shown
  → On success: cache updated, isOffline flipped false, banner disappears
  → On failure: no-op
```

**Phase 2:** Replace `private var wordCache` in `NetworkWordsRepository` with a Room DAO to persist across restarts.

---

## Module Structure

```
app/src/main/java/com/amanagarwal/unscramble/
│
├── MainActivity.kt               @AndroidEntryPoint
├── WordsApplication.kt           @HiltAndroidApp
│
├── di/
│   ├── NetworkModule.kt          Singleton DI: networking stack
│   ├── RepositoryModule.kt       Singleton DI: repository binding
│   └── UseCaseModule.kt          ViewModelScoped DI: use cases
│
├── data/
│   ├── WordsData.kt              Constants + static fallback word list
│   ├── WordsRepository.kt        Interface + NetworkWordsRepository (with cache)
│   └── WordsResult.kt            Sealed class: Live / Cached / Static
│
├── network/
│   └── WordsApiService.kt        Retrofit → random-word-api.herokuapp.com
│
├── domain/usecase/
│   ├── ValidateGuessUseCase.kt   Result: Correct / Incorrect / Empty
│   ├── ShuffleWordUseCase.kt     Shuffles letters (max 50 attempts)
│   └── CalculateScoreUseCase.kt  Scoring arithmetic
│
├── viewmodels/
│   └── GameViewModel.kt          @HiltViewModel — owns all game state
│
└── ui/
    ├── theme/
    │   ├── UnScrambleApp.kt      NavHost — Start → Game routes
    │   ├── Theme.kt
    │   ├── Color.kt
    │   └── Type.kt
    └── screens/
        ├── GameScreen.kt         Game UI composables + OfflineBanner + ScoreDeltaChip
        └── StartScreen.kt        Landing screen
```

### Key constants (WordsData.kt)

| Constant | Value | Purpose |
|----------|-------|---------|
| `MAX_NO_OF_WORDS` | 10 | Words per game round |
| `SCORE_INCREASE` | 20 | Points for a correct guess |

---

## Build & Run

### Requirements
- Android Studio Narwhal or later
- JDK 17+
- Android SDK 35

### Commands
```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device/emulator
./gradlew testDebugUnitTest      # run all unit tests
./gradlew assembleRelease        # build release AAB
```

### Key dependency versions

| Tool | Version |
|------|---------|
| Kotlin | 2.2.10 |
| AGP | 9.2.1 |
| Hilt | 2.57.1 |
| KSP | 2.2.10-2.0.2 |
| Compose BOM | 2025.01.00 |
| Retrofit | 2.9.0 |
| kotlinx-serialization | 1.7.3 |
| Gradle Wrapper | 9.4.1 |

---

## Testing

### Run tests
```bash
./gradlew testDebugUnitTest
```

### Test layout
```
app/src/test/java/com/amanagarwal/unscramble/
├── domain/usecase/
│   ├── ValidateGuessUseCaseTest.kt    7 tests
│   ├── ShuffleWordUseCaseTest.kt      6 tests
│   └── CalculateScoreUseCaseTest.kt   4 tests
└── viewmodels/
    └── GameViewModelTest.kt           27 tests
                                       Includes regression tests for all fixed bugs
Total: 44 tests, all passing
```

### Test conventions
- `GameViewModel` tests use a **fake `WordsRepository`** — zero real network calls.
- `isReturnDefaultValues = true` in `testOptions` stubs `android.util.Log` for JVM.
- `StandardTestDispatcher` + `advanceUntilIdle()` to control coroutine timing.
- `vm.enableDevModeForTesting()` activates dev mode without `BuildConfig.DEBUG`.

### Adding a new ViewModel test
```kotlin
@Test
fun `describe what you are testing`() = runTest {
    val vm = buildViewModel()   // uses successRepo by default
    advanceUntilIdle()          // let fetchWords() complete

    // arrange, act, assert
}
```

### Adding a new fake repo variant
```kotlin
private val cachedRepo = object : WordsRepository {
    override suspend fun getWords() = WordsResult.Cached(fakeWords)
    override suspend fun refreshCacheInBackground() = false
}
```

---

## Developer Guide

### Activating developer mode (debug builds only)
Type in the guess field:
- `dev_mode=true`  → shows developer overlay
- `dev_mode=false` → hides developer overlay

Overlay shows: correct answer, word pool stats, Reveal + Skip buttons.

### Changing game length
Edit `MAX_NO_OF_WORDS` in `data/WordsData.kt`.
The UI round counter and game-over check both read this constant — no other changes needed.

### Changing scoring
Edit `CalculateScoreUseCase.kt`:
```kotlin
fun onCorrectGuess(score: Int): Int = score + SCORE_INCREASE  // default: +20
fun onWrongGuess(score: Int): Int  = maxOf(0, score - 10)     // default: -10, floor 0
```

### Changing the API endpoint
1. `NetworkModule.kt` → update `.baseUrl(...)` in `provideRetrofit()`
2. `WordsApiService.kt` → update `@GET` path and response type

### Adding a new screen
1. Add a value to the `UnscrambleScreen` enum in `UnScrambleApp.kt`
2. Add a `composable(route = ...)` entry in the `NavHost`
3. Create the screen file in `ui/screens/`
4. If the screen needs a ViewModel, annotate it `@HiltViewModel` + `@Inject constructor`
   and retrieve it in the composable with `hiltViewModel()`

### Adding a new Hilt module
Create a file in `di/`:
```kotlin
@Module
@InstallIn(SingletonComponent::class)  // or ViewModelComponent, etc.
object MyModule {
    @Provides @Singleton
    fun provideMyThing(): MyThing = MyThing()
}
```

### Updating the static fallback word list
Edit `allWords` in `data/WordsData.kt`. Words should be:
- 5+ letters long
- No consecutive repeated letters (shuffle algorithm needs room to permute)
- Lowercase

---

## Roadmap

| Phase | Goal | Status |
|-------|------|--------|
| 1 | Hilt DI migration | ✅ Complete |
| 2 | Room + DataStore (persist word cache + game history) | 🔜 Next |
| 3 | Firebase Auth (user identity — needed for PvP) | ⏳ Planned |
| 4 | Same-device PvP (split screen, 2 players, 1 phone) | ⏳ Planned |
| 5 | Online PvP (backend REST + WebSocket) | ⏳ Planned |
| 6 | Themed word modes (animals, science, sports…) | ⏳ Planned |

### Phase 2 checklist (Room + DataStore)
- [ ] Add Room + DataStore versions to `libs.versions.toml`
- [ ] Create `WordEntity` + `WordDao`
- [ ] Create `AppDatabase` (`@Database`)
- [ ] Add `DatabaseModule` to `di/`
- [ ] Replace `private var wordCache: Set<String>` in `NetworkWordsRepository` with Room DAO
- [ ] DataStore: persist best score, total games played
- [ ] `HistoryRepository` + `HistoryViewModel` for a history screen

### Phase 5 notes (Online PvP)
- Requires a backend (REST + WebSocket)
- API-driven word source is mandatory — static words cannot guarantee fairness between players
- Auth from Phase 3 needed to identify players in matchmaking
- Consider: lobby → match → live round → result flow

---

## Known Issues / Backlog

| # | Severity | Issue | Notes |
|---|----------|-------|-------|
| 5 | 🟠 Medium | Short/repeated-letter words in `allWords` | Needs a filter pass on the static list |
| 6 | 🟠 Medium | `ShuffleWordUseCase` returns original word after 50 failed shuffle attempts | Need fallback strategy |
| 7 | 🟠 Medium | No length/quality filter on API words | API can return 2-letter words |
| 19 | 🔵 Low | Menu button in `GameScreen` is a no-op | Nav action not wired |
| 23 | 🔧 Cleanup | Coil dep declared but unused | Remove from `build.gradle.kts` |
| 26 | 🔧 Cleanup | `package` in `AndroidManifest.xml` is deprecated | Move to `namespace` in build file |
