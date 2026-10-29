package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraView
import com.example.model.ControlScheme
import com.example.model.GameMode
import com.example.model.TransmissionMode
import com.example.physics.CarPhysics
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GameHudOverlay(
    physics: CarPhysics,
    gameMode: GameMode,
    cameraView: CameraView,
    controlScheme: ControlScheme,
    lapTimeMs: Long,
    currentLap: Int,
    totalLaps: Int,
    racePosition: Int,
    totalRacers: Int,
    pitRadioText: String,
    onCameraToggle: () -> Unit,
    onPauseClick: () -> Unit,
    onOpenAiEngineer: () -> Unit,
    onShiftUp: () -> Unit,
    onShiftDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // TOP HUD BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Speed & Gear Cluster (Top Left)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0B0E14))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${physics.speedKmh.roundToInt()}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "KM/H",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // RPM and Gear
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val gearText = when (physics.currentGear) {
                        -1 -> "R"
                        0 -> "N"
                        else -> "${physics.currentGear}"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (gearText == "R") Color(0xFFFF1744) else Color(0xFF00E5FF))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "GEAR $gearText",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "${physics.rpm.toInt()} RPM",
                        color = if (physics.rpm > 7000f) Color(0xFFFF1744) else Color(0xFFB0BEC5),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Nitro bar
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Nitro",
                        tint = Color(0xFFFF6D00),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    LinearProgressIndicator(
                        progress = { physics.nitroAmount / 100f },
                        modifier = Modifier
                            .width(90.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFF6D00),
                        trackColor = Color(0xFF263238),
                    )
                }
            }

            // Center: Lap, Position, or Mode status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0B0E14))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                if (gameMode == GameMode.RACING) {
                    Text(
                        text = "POS $racePosition / $totalRacers",
                        color = if (racePosition == 1) Color(0xFFFFD600) else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "LAP $currentLap / $totalLaps",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (gameMode == GameMode.DRIFT) {
                    Text(
                        text = "DRIFT POINTS",
                        color = Color(0xFFFF6D00),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${physics.totalDriftScore + physics.currentDriftScore}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = gameMode.title.uppercase(),
                        color = Color(0xFF76FF03),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val mins = (lapTimeMs / 60000)
                val secs = (lapTimeMs % 60000) / 1000
                val millis = (lapTimeMs % 1000) / 10
                Text(
                    text = String.format("%02d:%02d.%02d", mins, secs, millis),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Top Right Action Buttons (Camera, AI Engineer, Pause)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onOpenAiEngineer,
                    modifier = Modifier
                        .testTag("ai_engineer_button")
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD00E5FF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "AI Race Engineer",
                        tint = Color.Black
                    )
                }

                IconButton(
                    onClick = onCameraToggle,
                    modifier = Modifier
                        .testTag("camera_toggle_button")
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0B0E14))
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .testTag("pause_button")
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0B0E14))
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause Game",
                        tint = Color.White
                    )
                }
            }
        }

        // Mid-screen Live Pit Radio Callouts
        if (pitRadioText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEE111827))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "📻 PIT RADIO: $pitRadioText",
                    color = Color(0xFFE0F7FA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Active Drift Score Multiplier Banner
        if (physics.isDrifting) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DRIFT +${physics.currentDriftScore}",
                    color = Color(0xFFFFD600),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "MULTIPLIER x${String.format("%.1f", physics.driftMultiplier)}",
                    color = Color(0xFFFF3D00),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // BOTTOM CONTROLS & PEDALS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // LEFT: Steering Controls
            when (controlScheme) {
                ControlScheme.TOUCH_BUTTONS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Steer Left Button
                        Box(
                            modifier = Modifier
                                .testTag("steer_left_button")
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (physics.steeringInput < -0.2f) Color(0xFF00E5FF) else Color(0x991E2430))
                                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            physics.steeringInput = -1.0f
                                            tryAwaitRelease()
                                            physics.steeringInput = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◀", color = Color.White, fontSize = 28.sp)
                        }

                        // Steer Right Button
                        Box(
                            modifier = Modifier
                                .testTag("steer_right_button")
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (physics.steeringInput > 0.2f) Color(0xFF00E5FF) else Color(0x991E2430))
                                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            physics.steeringInput = 1.0f
                                            tryAwaitRelease()
                                            physics.steeringInput = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", color = Color.White, fontSize = 28.sp)
                        }
                    }
                }
                ControlScheme.STEERING_WHEEL, ControlScheme.JOYSTICK_SLIDER -> {
                    // Touch Joystick Slider
                    var dragOffset by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0x991E2430))
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(28.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset = (dragOffset + dragAmount.x).coerceIn(-60f, 60f)
                                        physics.steeringInput = dragOffset / 60f
                                    },
                                    onDragEnd = {
                                        dragOffset = 0f
                                        physics.steeringInput = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = ((dragOffset / 60f) * 45f + 45f).dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                        )
                    }
                }
                ControlScheme.TILT_SENSOR -> {
                    Text(
                        text = "📱 Tilt device to steer",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // CENTER: Vehicle Accessories (Headlights, Doors, Horn, Handbrake, Nitro)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Headlights Toggle
                IconButton(
                    onClick = { physics.toggleHeadlights() },
                    modifier = Modifier
                        .testTag("headlights_button")
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (physics.headlightsOn) Color(0xFFFFD600) else Color(0x991A202C))
                ) {
                    Icon(
                        imageVector = Icons.Default.Highlight,
                        contentDescription = "Headlights",
                        tint = if (physics.headlightsOn) Color.Black else Color.White
                    )
                }

                // Doors Toggle
                IconButton(
                    onClick = { physics.toggleDoors() },
                    modifier = Modifier
                        .testTag("doors_button")
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (physics.doorsOpen) Color(0xFF00E5FF) else Color(0x991A202C))
                ) {
                    Text("🚪", fontSize = 18.sp)
                }

                // Horn Button (press & hold)
                Box(
                    modifier = Modifier
                        .testTag("horn_button")
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (physics.hornActive) Color(0xFFFF1744) else Color(0x991A202C))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    physics.hornActive = true
                                    tryAwaitRelease()
                                    physics.hornActive = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Horn",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Handbrake / Drift Initiator
                Box(
                    modifier = Modifier
                        .testTag("handbrake_button")
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (physics.isHandbrakeActive) Color(0xFFFF3D00) else Color(0x992E1B1B))
                        .border(1.5.dp, Color(0xFFFF3D00), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    physics.isHandbrakeActive = true
                                    tryAwaitRelease()
                                    physics.isHandbrakeActive = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("DRIFT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                // Nitro Boost Bottle Button
                Box(
                    modifier = Modifier
                        .testTag("nitro_button")
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            if (physics.isNitroActive) Brush.radialGradient(listOf(Color(0xFFFF3D00), Color(0xFF00E5FF)))
                            else Brush.radialGradient(listOf(Color(0xFF0091EA), Color(0xFF0D47A1)))
                        )
                        .border(2.dp, Color(0xFF00E5FF), CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    if (physics.nitroAmount > 5f) {
                                        physics.isNitroActive = true
                                        tryAwaitRelease()
                                        physics.isNitroActive = false
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Nitro Boost",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "NITRO",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // RIGHT: Pedals (Brake & Throttle) + Shifters
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                // If Manual transmission, show Paddle Shifter Down & Up
                if (physics.transmissionMode == TransmissionMode.MANUAL) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onShiftUp,
                            modifier = Modifier.size(width = 46.dp, height = 34.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onShiftDown,
                            modifier = Modifier.size(width = 46.dp, height = 34.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Brake Pedal
                Box(
                    modifier = Modifier
                        .testTag("brake_pedal")
                        .size(width = 58.dp, height = 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (physics.brakeInput > 0.1f) Color(0xFFFF1744) else Color(0x99B71C1C))
                        .border(2.dp, Color(0xFFFF5252), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    physics.brakeInput = 1.0f
                                    tryAwaitRelease()
                                    physics.brakeInput = 0f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BRAKE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Throttle / Gas Pedal
                Box(
                    modifier = Modifier
                        .testTag("gas_pedal")
                        .size(width = 68.dp, height = 100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (physics.throttleInput > 0.1f) Brush.verticalGradient(listOf(Color(0xFF00E676), Color(0xFF00C853)))
                            else Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF0A2B0F)))
                        )
                        .border(2.dp, Color(0xFF00E676), RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    physics.throttleInput = 1.0f
                                    tryAwaitRelease()
                                    physics.throttleInput = 0f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DRIVE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
