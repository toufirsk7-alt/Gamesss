package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.ai.AiRaceEngineer
import com.example.data.AuthManager
import com.example.data.FirestoreRepository
import com.example.data.configureAppCheck
import com.example.model.CarCatalog
import com.example.model.CarSpec
import com.example.model.CustomMap
import com.example.model.GameMode
import com.example.model.GarageCar
import com.example.model.ScoreRecord
import com.example.model.TrackCatalog
import com.example.model.TrackSpec
import com.example.model.UserProfile
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CareerStoryScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MapBuilderScreen
import com.example.ui.screens.TrackSelectScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class Screen {
    data object MainMenu : Screen()
    data class TrackSelect(val gameMode: GameMode) : Screen()
    data class Gameplay(val track: TrackSpec, val gameMode: GameMode) : Screen()
    data object Garage : Screen()
    data object CareerStory : Screen()
    data object MapBuilder : Screen()
    data object Leaderboard : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure Firebase App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090C12)
                ) {
                    Box(modifier = Modifier.safeDrawingPadding()) {
                        AppRoot()
                    }
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Attempt silent auto-sign in on initial launch
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {},
                scope = coroutineScope
            )
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        MainAppFlow(
            userId = currentUser!!.uid,
            displayName = currentUser!!.displayName ?: "Apex Racer",
            onSignOut = {
                AuthManager.signOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {
                        currentUser = null
                    },
                    scope = coroutineScope
                )
            }
        )
    }
}

@Composable
fun MainAppFlow(
    userId: String,
    displayName: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FirestoreRepository(context) }
    val aiRaceEngineer = remember { AiRaceEngineer() }

    var userProfile by remember {
        mutableStateOf(
            UserProfile(
                userId = userId,
                displayName = displayName,
                careerLevel = 1,
                cash = 50000,
                reputation = 100,
                selectedCarId = "bmw_m4"
            )
        )
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
    var selectedCarSpec by remember { mutableStateOf(CarCatalog.ALL_CARS[0]) }
    var selectedGarageCar by remember { mutableStateOf<GarageCar?>(null) }
    var ownedCars by remember { mutableStateOf<List<GarageCar>>(emptyList()) }
    var customMaps by remember { mutableStateOf<List<CustomMap>>(emptyList()) }
    var leaderboardScores by remember { mutableStateOf<List<ScoreRecord>>(emptyList()) }
    var isDataLoaded by remember { mutableStateOf(false) }

    // Real-time synchronization with Firestore
    LaunchedEffect(userId) {
        // Observe Profile
        launch {
            repository.observeUserProfile()
                .catch { /* handled in repo */ }
                .collect { profile ->
                    if (profile != null) {
                        userProfile = profile
                        selectedCarSpec = CarCatalog.getCar(profile.selectedCarId)
                    } else {
                        // First-time user initialization in Firestore
                        val initProfile = UserProfile(
                            userId = userId,
                            displayName = displayName,
                            careerLevel = 1,
                            cash = 50000,
                            reputation = 100,
                            selectedCarId = "bmw_m4"
                        )
                        repository.saveUserProfile(initProfile)
                        userProfile = initProfile
                    }
                    isDataLoaded = true
                }
        }

        // Observe Garage Cars
        launch {
            repository.observeGarageCars()
                .catch { /* handled */ }
                .collect { cars ->
                    ownedCars = cars
                    selectedGarageCar = cars.firstOrNull { it.carModelKey == selectedCarSpec.key }
                }
        }

        // Observe Custom Maps
        launch {
            repository.observeCustomMaps()
                .catch { /* handled */ }
                .collect { maps ->
                    customMaps = maps
                }
        }

        // Observe Leaderboard Scores
        launch {
            repository.observeLeaderboard()
                .catch { /* handled */ }
                .collect { scores ->
                    leaderboardScores = scores
                }
        }
    }

    if (!isDataLoaded) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF090C12)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00E5FF))
        }
    } else {
        when (val screen = currentScreen) {
            is Screen.MainMenu -> {
                MainMenuScreen(
                    userProfile = userProfile,
                    activeCar = selectedCarSpec,
                    garageCar = selectedGarageCar,
                    onSelectMode = { mode ->
                        when (mode) {
                            GameMode.CAREER, GameMode.STORY -> currentScreen = Screen.CareerStory
                            GameMode.MAP_BUILDER -> currentScreen = Screen.MapBuilder
                            else -> currentScreen = Screen.TrackSelect(mode)
                        }
                    },
                    onOpenGarage = { currentScreen = Screen.Garage },
                    onOpenLeaderboard = { currentScreen = Screen.Leaderboard },
                    onSignOut = onSignOut
                )
            }

            is Screen.TrackSelect -> {
                TrackSelectScreen(
                    selectedTrack = TrackCatalog.ALL_TRACKS[0],
                    customMaps = customMaps,
                    gameMode = screen.gameMode,
                    aiRaceEngineer = aiRaceEngineer,
                    onSelectTrack = { track ->
                        currentScreen = Screen.Gameplay(track, screen.gameMode)
                    },
                    onStartGame = { track ->
                        currentScreen = Screen.Gameplay(track, screen.gameMode)
                    },
                    onOpenMapBuilder = { currentScreen = Screen.MapBuilder },
                    onBack = { currentScreen = Screen.MainMenu }
                )
            }

            is Screen.Gameplay -> {
                GameScreen(
                    track = screen.track,
                    carSpec = selectedCarSpec,
                    garageCar = selectedGarageCar,
                    userProfile = userProfile,
                    gameMode = screen.gameMode,
                    aiRaceEngineer = aiRaceEngineer,
                    onFinishGame = { wonCash, wonRep, scoreRecord ->
                        coroutineScope.launch {
                            val updatedCash = userProfile.cash + wonCash
                            val updatedRep = userProfile.reputation + wonRep
                            val updatedLevel = (updatedRep / 100) + 1
                            val updatedProfile = userProfile.copy(
                                cash = updatedCash,
                                reputation = updatedRep,
                                careerLevel = updatedLevel
                            )
                            repository.saveUserProfile(updatedProfile)
                            userProfile = updatedProfile

                            // Submit record to global leaderboard
                            repository.submitScore(scoreRecord)
                        }
                    },
                    onExitGame = {
                        currentScreen = Screen.MainMenu
                    }
                )
            }

            is Screen.Garage -> {
                GarageScreen(
                    userProfile = userProfile,
                    ownedCars = ownedCars,
                    selectedCarSpec = selectedCarSpec,
                    selectedGarageCar = selectedGarageCar,
                    onSelectCar = { car ->
                        selectedCarSpec = car
                        selectedGarageCar = ownedCars.firstOrNull { it.carModelKey == car.key }
                        coroutineScope.launch {
                            val updatedProfile = userProfile.copy(selectedCarId = car.key)
                            repository.saveUserProfile(updatedProfile)
                            userProfile = updatedProfile
                        }
                    },
                    onSaveGarageCar = { updatedCar ->
                        coroutineScope.launch {
                            repository.saveGarageCar(updatedCar)
                            selectedGarageCar = updatedCar
                        }
                    },
                    onBack = { currentScreen = Screen.MainMenu }
                )
            }

            is Screen.CareerStory -> {
                CareerStoryScreen(
                    userProfile = userProfile,
                    onStartRace = { track, mode ->
                        currentScreen = Screen.Gameplay(track, mode)
                    },
                    onBack = { currentScreen = Screen.MainMenu }
                )
            }

            is Screen.MapBuilder -> {
                MapBuilderScreen(
                    userProfile = userProfile,
                    customMaps = customMaps,
                    onSaveMap = { newMap ->
                        coroutineScope.launch {
                            repository.saveCustomMap(newMap)
                        }
                    },
                    onDeleteMap = { mapId ->
                        coroutineScope.launch {
                            repository.deleteCustomMap(mapId)
                        }
                    },
                    onTestDriveMap = { map ->
                        val testTrack = TrackSpec(
                            id = map.id,
                            name = map.name,
                            location = "Custom Circuit",
                            country = "Map Creator",
                            environment = com.example.model.EnvironmentType.CITY_HIGHWAY,
                            lengthMeters = 4000,
                            curvesCount = 12,
                            difficulty = "Custom",
                            defaultWeather = com.example.model.WeatherCondition.MIDNIGHT_NEON,
                            googleMapsCoordinates = "Custom Coordinates",
                            landmark = "Player Crafted Ring",
                            targetLapTimeMs = 90000,
                            description = "User created track from Map Builder."
                        )
                        currentScreen = Screen.Gameplay(testTrack, GameMode.FREE_ROAM)
                    },
                    onBack = { currentScreen = Screen.MainMenu }
                )
            }

            is Screen.Leaderboard -> {
                LeaderboardScreen(
                    scores = leaderboardScores,
                    onBack = { currentScreen = Screen.MainMenu }
                )
            }
        }
    }
}
