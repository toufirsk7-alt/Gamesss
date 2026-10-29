package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomMap
import com.example.model.TrackSpec
import com.example.model.UserProfile
import com.example.model.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

data class TrackSegmentPoint(val type: String, val curvature: Float, val elevation: Float)

@Composable
fun MapBuilderScreen(
    userProfile: UserProfile,
    customMaps: List<CustomMap>,
    onSaveMap: (CustomMap) -> Unit,
    onDeleteMap: (String) -> Unit,
    onTestDriveMap: (CustomMap) -> Unit,
    onBack: () -> Unit
) {
    var mapName by remember { mutableStateOf("Neon Thunder Ring") }
    var selectedEnv by remember { mutableStateOf("neon_night") }
    var selectedWeather by remember { mutableStateOf("rain") }
    var targetLaps by remember { mutableIntStateOf(3) }
    var obstaclesCount by remember { mutableIntStateOf(16) }

    val trackSegments = remember {
        mutableStateOf(
            listOf(
                TrackSegmentPoint("Straight", 0f, 0f),
                TrackSegmentPoint("Right Curve", 1.8f, 20f),
                TrackSegmentPoint("Chicane", -2.2f, -10f),
                TrackSegmentPoint("Hairpin", 3.5f, 40f),
                TrackSegmentPoint("Straight Boost", 0f, 0f),
                TrackSegmentPoint("S-Bend", -1.5f, 15f),
                TrackSegmentPoint("Sweeping Left", -2.8f, -20f),
                TrackSegmentPoint("Final Straight", 0f, 0f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090C12))
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("map_builder_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("MAP CREATOR STUDIO", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("DESIGN, SAVE & CLOUD SYNC", color = Color(0xFF76FF03), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    val map = CustomMap(
                        id = "map_${System.currentTimeMillis()}",
                        userId = userProfile.userId,
                        name = mapName,
                        environment = selectedEnv,
                        weather = selectedWeather,
                        trackPointsJson = trackSegments.value.joinToString(";") { "${it.type},${it.curvature},${it.elevation}" },
                        obstaclesCount = obstaclesCount,
                        laps = targetLaps
                    )
                    onSaveMap(map)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_custom_map_button")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SAVE MAP", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        // 2D Track Blueprint Preview Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF101622))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Draw Track Layout Preview Line
                val path = Path()
                var px = cx - 110f
                var py = cy
                path.moveTo(px, py)

                var angle = 0f
                trackSegments.value.forEach { seg ->
                    angle += seg.curvature * 18f
                    val rad = angle * (Math.PI / 180.0).toFloat()
                    val dist = 55f
                    px += cos(rad) * dist
                    py += sin(rad) * dist
                    path.lineTo(px, py)
                }

                drawPath(
                    path = path,
                    color = Color(0xFF00E5FF),
                    style = Stroke(width = 6f)
                )

                // Start / Finish Line marker
                drawCircle(color = Color(0xFFFF1744), radius = 8f, center = Offset(cx - 110f, cy))
            }

            Text(
                text = "BLUEPRINT: ${trackSegments.value.size} SECTORS • ${obstaclesCount} HAZARDS",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = mapName,
                    onValueChange = { mapName = it },
                    label = { Text("Track Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF76FF03),
                        unfocusedBorderColor = Color(0xFF263238),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            item {
                Text("ADD TRACK CORNERS & SECTORS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AddSectorChip("+ STRAIGHT") {
                            trackSegments.value = trackSegments.value + TrackSegmentPoint("Straight", 0f, 0f)
                        }
                    }
                    item {
                        AddSectorChip("+ RIGHT HAIRPIN") {
                            trackSegments.value = trackSegments.value + TrackSegmentPoint("Right Hairpin", 3.2f, 20f)
                        }
                    }
                    item {
                        AddSectorChip("+ LEFT CHICANE") {
                            trackSegments.value = trackSegments.value + TrackSegmentPoint("Left Chicane", -2.8f, -10f)
                        }
                    }
                    item {
                        AddSectorChip("+ UPHILL S-BEND") {
                            trackSegments.value = trackSegments.value + TrackSegmentPoint("S-Bend", 2.0f, 50f)
                        }
                    }
                }
            }

            item {
                Text("ENVIRONMENT THEME", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("neon_night" to "Midnight Neon", "city" to "City Highway", "desert" to "Desert Sands", "mountain" to "Alpine Pass").forEach { (envKey, label) ->
                        val isSelected = selectedEnv == envKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF182030))
                                .clickable { selectedEnv = envKey }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("WEATHER CONDITIONS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sunny" to "Sunny", "rain" to "Heavy Rain", "snow" to "Snow Flurries", "fog" to "Thunderstorm").forEach { (wKey, label) ->
                        val isSelected = selectedWeather == wKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFF6D00) else Color(0xFF182030))
                                .clickable { selectedWeather = wKey }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Saved Custom Maps List
            if (customMaps.isNotEmpty()) {
                item {
                    Text("YOUR CLUSTER OF SAVED MAPS (${customMaps.size})", color = Color(0xFFFFD600), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                items(customMaps) { map ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(map.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${map.environment} • Weather: ${map.weather} • ${map.laps} Laps", color = Color.Gray, fontSize = 11.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onTestDriveMap(map) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("TEST DRIVE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }

                                IconButton(
                                    onClick = { onDeleteMap(map.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF1744))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSectorChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E283A))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
