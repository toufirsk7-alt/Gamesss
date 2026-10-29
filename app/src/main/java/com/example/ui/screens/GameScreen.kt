package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.AiRaceEngineer
import com.example.audio.SoundEngine
import com.example.engine.Racing3DEngine
import com.example.model.AiRivalCar
import com.example.model.CameraView
import com.example.model.CarSpec
import com.example.model.ControlScheme
import com.example.model.GameMode
import com.example.model.GarageCar
import com.example.model.PedestrianNpc
import com.example.model.ScoreRecord
import com.example.model.TelemetrySnapshot
import com.example.model.TrackSpec
import com.example.model.TrafficVehicle
import com.example.model.TransmissionMode
import com.example.model.UserProfile
import com.example.model.WeatherCondition
import com.example.physics.CarPhysics
import com.example.ui.components.AiEngineerDialog
import com.example.ui.components.GameHudOverlay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun GameScreen(
    track: TrackSpec,
    carSpec: CarSpec,
    garageCar: GarageCar?,
    userProfile: UserProfile,
    gameMode: GameMode,
    aiRaceEngineer: AiRaceEngineer,
    onFinishGame: (cashWon: Int, repWon: Int, ScoreRecord) -> Unit,
    onExitGame: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    val soundEngine = remember { SoundEngine() }
    val renderEngine = remember { Racing3DEngine() }
    val physics = remember {
        CarPhysics(baseSpec = carSpec, garageCar = garageCar).apply {
            transmissionMode = TransmissionMode.AUTOMATIC
        }
    }

    var cameraView by remember { mutableStateOf(CameraView.THIRD_PERSON) }
    var controlScheme by remember { mutableStateOf(ControlScheme.TOUCH_BUTTONS) }
    var currentWeather by remember { mutableStateOf(track.defaultWeather) }

    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var showAiEngineerDialog by remember { mutableStateOf(false) }

    var currentLap by remember { mutableIntStateOf(1) }
    val totalLaps = if (gameMode == GameMode.RACING) 3 else 1
    var lapStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var currentLapTimeMs by remember { mutableLongStateOf(0L) }
    var bestLapTimeMs by remember { mutableLongStateOf(0L) }

    var racePosition by remember { mutableIntStateOf(8) }
    val totalRacers = 8
    var pitRadioText by remember { mutableStateOf("") }
    var activeNpcInteraction by remember { mutableStateOf<PedestrianNpc?>(null) }
    var elapsedTimeSec by remember { mutableFloatStateOf(0f) }

    // Free roam traffic cars
    val trafficList = remember {
        mutableListOf(
            TrafficVehicle(1, -0.6f, 300f, 65f, "#ECEFF1", "sedan"),
            TrafficVehicle(2, 0.5f, 650f, 80f, "#FFEB3B", "cab"),
            TrafficVehicle(3, 0.0f, 1100f, 95f, "#E53935", "sports"),
            TrafficVehicle(4, -0.7f, 1500f, 55f, "#424242", "suv"),
            TrafficVehicle(5, 0.6f, 1900f, 75f, "#00ACC1", "truck")
        )
    }

    // Interactive pedestrian NPCs in Free Roam
    val npcList = remember {
        mutableListOf(
            PedestrianNpc(1, -1.35f, 180f, 1.2f, "Kenji", "Speed radar active ahead! Watch the revs!"),
            PedestrianNpc(2, 1.35f, 520f, 0.9f, "Mia", "That exhaust flame is sick! What engine is in there?"),
            PedestrianNpc(3, -1.35f, 980f, 1.0f, "Marcus", "Ryker's crew is hanging out near the sea bridge!"),
            PedestrianNpc(4, 1.35f, 1420f, 1.4f, "Aria", "Take the inner hairpin line to gain 2 seconds!")
        )
    }

    // AI Rival Cars in Racing Mode
    val rivalsList = remember {
        mutableListOf(
            AiRivalCar(1, "Viper", "bmw_m8", -0.3f, 400f, 180f, 1, 1, 0f, "#FF1744", 1.15f),
            AiRivalCar(2, "Phantom", "porsche_911", 0.3f, 350f, 175f, 2, 1, 0f, "#FFD600", 1.10f),
            AiRivalCar(3, "ApexGT", "mercedes_amg", -0.6f, 280f, 170f, 3, 1, 0f, "#00E5FF", 1.05f),
            AiRivalCar(4, "Thunder", "audi_r8", 0.5f, 220f, 165f, 4, 1, 0f, "#76FF03", 1.02f),
            AiRivalCar(5, "Titan", "scorpio_classic", -0.2f, 160f, 150f, 5, 1, 0f, "#424242", 0.98f),
            AiRivalCar(6, "Volt", "tata_sierra_ev", 0.6f, 110f, 155f, 6, 1, 0f, "#00E676", 0.95f),
            AiRivalCar(7, "Blaze", "mahindra_xuv3xo", -0.5f, 60f, 145f, 7, 1, 0f, "#D50000", 0.90f)
        )
    }

    // Start audio on mount & stop on dispose
    DisposableEffect(Unit) {
        soundEngine.soundProfile = carSpec.soundProfile
        soundEngine.start()
        onDispose {
            soundEngine.stop()
        }
    }

    // Back handler opens pause menu
    BackHandler {
        if (!isGameOver) isPaused = true else onExitGame()
    }

    // Weather change loop during long races
    LaunchedEffect(Unit) {
        var weatherIndex = WeatherCondition.entries.indexOf(track.defaultWeather)
        while (isActive) {
            kotlinx.coroutines.delay(45000L) // change weather every 45s for dynamic environment
            weatherIndex = (weatherIndex + 1) % WeatherCondition.entries.size
            currentWeather = WeatherCondition.entries[weatherIndex]
        }
    }

    // Periodic Pit Radio Commentary
    LaunchedEffect(Unit) {
        while (isActive) {
            kotlinx.coroutines.delay(20000L)
            if (!isPaused && !isGameOver) {
                pitRadioText = aiRaceEngineer.getPitRadioCallout(
                    speedKmh = physics.speedKmh,
                    position = racePosition,
                    lap = currentLap,
                    damagePercent = physics.damagePercent,
                    isDrifting = physics.isDrifting
                )
            }
        }
    }

    // Main 60 FPS Physics & Simulation Loop
    LaunchedEffect(isPaused, isGameOver) {
        var lastNano = System.nanoTime()
        while (isActive && !isPaused && !isGameOver) {
            withFrameNanos { now ->
                val dt = ((now - lastNano) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
                lastNano = now
                elapsedTimeSec += dt
                currentLapTimeMs = System.currentTimeMillis() - lapStartTime

                // 1. Update Player Physics
                physics.update(
                    deltaTimeSec = dt,
                    weather = currentWeather,
                    onGearShift = { soundEngine.triggerShift() }
                )

                // 2. Update Sound Engine parameters
                soundEngine.targetRpm = physics.rpm
                soundEngine.isAccelerating = physics.throttleInput > 0.1f
                soundEngine.isBraking = physics.brakeInput > 0.1f
                soundEngine.isDrifting = physics.isDrifting
                soundEngine.isNitroActive = physics.isNitroActive
                soundEngine.isHornActive = physics.hornActive

                // 3. Collision with Road Barriers
                if (abs(physics.carPositionX) > 2.05f && physics.speedKmh > 20f) {
                    physics.applyCollisionDamage(0.35f)
                    soundEngine.triggerCrash()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    physics.carPositionX = if (physics.carPositionX > 0) 1.95f else -1.95f
                }

                // 4. Update Free Roam Traffic
                for (traffic in trafficList) {
                    traffic.z += (traffic.speedKmh / 3.6f) * dt
                    val relZ = traffic.z - physics.trackDistanceMeters
                    // Collision check
                    if (abs(relZ) < 7f && abs(physics.carPositionX - traffic.x) < 0.55f && physics.speedKmh > traffic.speedKmh) {
                        physics.applyCollisionDamage(0.6f)
                        soundEngine.triggerCrash()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                    }
                }

                // 5. Update AI Rival Cars (Racing Mode)
                if (gameMode == GameMode.RACING) {
                    var playerPos = 1
                    for (rival in rivalsList) {
                        rival.z += (rival.speedKmh / 3.6f) * dt
                        if (rival.z > physics.trackDistanceMeters) {
                            playerPos++
                        }
                        // Collision with rival
                        val relZ = rival.z - physics.trackDistanceMeters
                        if (abs(relZ) < 8f && abs(physics.carPositionX - rival.x) < 0.55f) {
                            physics.applyCollisionDamage(0.4f)
                            soundEngine.triggerCrash()
                        }
                    }
                    racePosition = playerPos.coerceIn(1, totalRacers)
                }

                // 6. Check NPC Interactions (Free Roam)
                if (gameMode == GameMode.FREE_ROAM) {
                    var nearbyNpc: PedestrianNpc? = null
                    for (npc in npcList) {
                        val dist = abs(npc.z - physics.trackDistanceMeters)
                        if (dist < 25f && physics.speedKmh < 40f) {
                            nearbyNpc = npc
                            break
                        }
                    }
                    activeNpcInteraction = nearbyNpc
                }

                // 7. Check Lap Completion
                if (physics.trackDistanceMeters >= track.lengthMeters * currentLap) {
                    if (bestLapTimeMs == 0L || currentLapTimeMs < bestLapTimeMs) {
                        bestLapTimeMs = currentLapTimeMs
                    }
                    if (currentLap >= totalLaps && gameMode != GameMode.FREE_ROAM) {
                        isGameOver = true
                        val wonCash = if (racePosition == 1) 25000 else 8000
                        val wonRep = if (racePosition == 1) 150 else 50
                        val scoreRecord = ScoreRecord(
                            id = "",
                            userId = userProfile.userId,
                            displayName = userProfile.displayName,
                            trackId = track.name,
                            carModel = carSpec.modelName,
                            lapTimeMs = currentLapTimeMs.toInt(),
                            driftScore = physics.totalDriftScore,
                            gameMode = if (gameMode == GameMode.DRIFT) "drift" else "race"
                        )
                        onFinishGame(wonCash, wonRep, scoreRecord)
                    } else {
                        currentLap++
                        lapStartTime = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 3D Graphics Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            renderEngine.renderScene(
                drawScope = this,
                physics = physics,
                cameraView = cameraView,
                weather = currentWeather,
                trafficList = trafficList,
                npcList = npcList,
                rivalsList = rivalsList,
                elapsedTimeSec = elapsedTimeSec,
                activeNpcInteraction = activeNpcInteraction
            )
        }

        // 2. HUD Instrument Cluster & Driving Pedals
        GameHudOverlay(
            physics = physics,
            gameMode = gameMode,
            cameraView = cameraView,
            controlScheme = controlScheme,
            lapTimeMs = currentLapTimeMs,
            currentLap = currentLap,
            totalLaps = totalLaps,
            racePosition = racePosition,
            totalRacers = totalRacers,
            pitRadioText = pitRadioText,
            onCameraToggle = {
                cameraView = when (cameraView) {
                    CameraView.THIRD_PERSON -> CameraView.FIRST_PERSON_COCKPIT
                    CameraView.FIRST_PERSON_COCKPIT -> CameraView.HOOD_CAM
                    CameraView.HOOD_CAM -> CameraView.THIRD_PERSON
                }
            },
            onPauseClick = { isPaused = true },
            onOpenAiEngineer = { showAiEngineerDialog = true },
            onShiftUp = { physics.shiftUp { soundEngine.triggerShift() } },
            onShiftDown = { physics.shiftDown { soundEngine.triggerShift() } }
        )

        // 3. AI Engineer Thinking Dialog
        if (showAiEngineerDialog) {
            val telemetry = TelemetrySnapshot(
                speedKmh = physics.speedKmh,
                rpm = physics.rpm,
                gear = "${physics.currentGear}",
                throttle = physics.throttleInput,
                brake = physics.brakeInput,
                steeringAngle = physics.steeringInput,
                nitroPercent = physics.nitroAmount,
                driftAngle = physics.driftAngle,
                driftScore = physics.totalDriftScore,
                damagePercent = physics.damagePercent,
                lapTimeMs = currentLapTimeMs,
                currentLap = currentLap,
                position = racePosition
            )
            AiEngineerDialog(
                telemetry = telemetry,
                carName = carSpec.modelName,
                trackName = track.name,
                aiRaceEngineer = aiRaceEngineer,
                onDismiss = { showAiEngineerDialog = false }
            )
        }

        // 4. Pause Menu Dialog
        if (isPaused) {
            PauseMenuDialog(
                controlScheme = controlScheme,
                transmissionMode = physics.transmissionMode,
                weather = currentWeather,
                onControlChange = { controlScheme = it },
                onTransmissionToggle = {
                    physics.transmissionMode = if (physics.transmissionMode == TransmissionMode.AUTOMATIC)
                        TransmissionMode.MANUAL else TransmissionMode.AUTOMATIC
                },
                onWeatherChange = { currentWeather = it },
                onResume = { isPaused = false },
                onRestart = {
                    physics.speedKmh = 0f
                    physics.trackDistanceMeters = 0f
                    physics.carPositionX = 0f
                    physics.damagePercent = 0f
                    currentLap = 1
                    lapStartTime = System.currentTimeMillis()
                    isPaused = false
                },
                onExit = onExitGame
            )
        }

        // 5. Game Over / Podium Dialog
        if (isGameOver) {
            RaceFinishedDialog(
                gameMode = gameMode,
                position = racePosition,
                lapTimeMs = currentLapTimeMs,
                driftScore = physics.totalDriftScore,
                onContinue = onExitGame
            )
        }
    }
}

@Composable
fun PauseMenuDialog(
    controlScheme: ControlScheme,
    transmissionMode: TransmissionMode,
    weather: WeatherCondition,
    onControlChange: (ControlScheme) -> Unit,
    onTransmissionToggle: () -> Unit,
    onWeatherChange: (WeatherCondition) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E131E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("GAME PAUSED", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)

                Text("CONTROLS SETUP", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ControlScheme.entries.forEach { scheme ->
                        val isSelected = scheme == controlScheme
                        Button(
                            onClick = { onControlChange(scheme) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2838)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(scheme.label, color = if (isSelected) Color.Black else Color.White, fontSize = 10.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TRANSMISSION", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onTransmissionToggle,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838))
                    ) {
                        Text(transmissionMode.name, color = Color(0xFFFFD600), fontWeight = FontWeight.Bold)
                    }
                }

                Text("WEATHER CYCLE OVERRIDE", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(WeatherCondition.SUNNY, WeatherCondition.RAIN, WeatherCondition.SNOW, WeatherCondition.MIDNIGHT_NEON).forEach { w ->
                        val isSel = w == weather
                        Button(
                            onClick = { onWeatherChange(w) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFFFF6D00) else Color(0xFF1E2838)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(w.label, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("RESUME RACE", color = Color.Black, fontWeight = FontWeight.Black)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                    ) {
                        Text("RESTART", color = Color.White)
                    }
                    Button(
                        onClick = onExit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                    ) {
                        Text("EXIT TO MENU", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RaceFinishedDialog(
    gameMode: GameMode,
    position: Int,
    lapTimeMs: Long,
    driftScore: Int,
    onContinue: () -> Unit
) {
    Dialog(onDismissRequest = onContinue) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E131E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(54.dp))

                Text(
                    text = if (position == 1) "VICTORY! 1ST PLACE" else "RACE COMPLETED: P$position",
                    color = if (position == 1) Color(0xFFFFD600) else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                val secs = lapTimeMs / 1000f
                Text(
                    text = "Finish Time: ${String.format("%.2f", secs)}s",
                    color = Color(0xFF00E5FF),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                if (gameMode == GameMode.DRIFT) {
                    Text(
                        text = "Total Drift Score: $driftScore PTS",
                        color = Color(0xFFFF6D00),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = "+$25,000 Cash • +150 Reputation Earned",
                    color = Color(0xFF76FF03),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CONTINUE TO GARAGE", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
