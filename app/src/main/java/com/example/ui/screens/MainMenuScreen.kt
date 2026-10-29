package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CarSpec
import com.example.model.GameMode
import com.example.model.GarageCar
import com.example.model.UserProfile

@Composable
fun MainMenuScreen(
    userProfile: UserProfile,
    activeCar: CarSpec,
    garageCar: GarageCar?,
    onSelectMode: (GameMode) -> Unit,
    onOpenGarage: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090C12))
    ) {
        // Top Player Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF131926))
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_launcher_icon_1793263170163),
                        contentDescription = "Racer Avatar",
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = userProfile.displayName.ifBlank { "Apex Driver" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "TIER ${userProfile.careerLevel} RACER • ${userProfile.reputation} REP",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141926))
                        .border(1.dp, Color(0xFFFFD600), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$${userProfile.cash}",
                        color = Color(0xFFFFD600),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onSignOut, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color.Gray
                    )
                }
            }
        }

        // Active Vehicle Showcase Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onOpenGarage),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121826)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE MACHINE",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = garageCar?.name ?: activeCar.modelName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${activeCar.brand} • ${activeCar.horsepower} HP • ${activeCar.topSpeedKmh} KM/H • ${activeCar.drivetrain}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onOpenGarage,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TUNE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Game Modes Grid / List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ModeCard(
                    title = "FREE ROAM OPEN CITY",
                    subtitle = "Explore open metropolis with live AI traffic, walking NPCs & speed traps",
                    icon = Icons.Default.Explore,
                    badgeColor = Color(0xFF76FF03),
                    testTag = "mode_free_roam",
                    onClick = { onSelectMode(GameMode.FREE_ROAM) }
                )
            }
            item {
                ModeCard(
                    title = "CIRCUIT RACING MODE",
                    subtitle = "Battle 7 aggressive AI rivals on 30 real-world circuits for the trophy",
                    icon = Icons.Default.SportsScore,
                    badgeColor = Color(0xFF00E5FF),
                    testTag = "mode_racing",
                    onClick = { onSelectMode(GameMode.RACING) }
                )
            }
            item {
                ModeCard(
                    title = "CAREER CHAMPIONSHIP",
                    subtitle = "Rise through 6 racing tiers from street rookie to World Champion",
                    icon = Icons.Default.EmojiEvents,
                    badgeColor = Color(0xFFFFD600),
                    testTag = "mode_career",
                    onClick = { onSelectMode(GameMode.CAREER) }
                )
            }
            item {
                ModeCard(
                    title = "DRIFT MULTIPLIER CHALLENGE",
                    subtitle = "Chain massive drift angles and score points with countersteering physics",
                    icon = Icons.Default.FlashOn,
                    badgeColor = Color(0xFFFF6D00),
                    testTag = "mode_drift",
                    onClick = { onSelectMode(GameMode.DRIFT) }
                )
            }
            item {
                ModeCard(
                    title = "STORY RIVAL DUELS",
                    subtitle = "Chapter narratives with notorious boss rivals and high-stakes pink slips",
                    icon = Icons.Default.DirectionsCar,
                    badgeColor = Color(0xFFFF1744),
                    testTag = "mode_story",
                    onClick = { onSelectMode(GameMode.STORY) }
                )
            }
            item {
                ModeCard(
                    title = "MAP CREATOR STUDIO",
                    subtitle = "Draw custom track corners, elevation jumps & hazards, then save to cloud",
                    icon = Icons.Default.Map,
                    badgeColor = Color(0xFFB388FF),
                    testTag = "mode_map_builder",
                    onClick = { onSelectMode(GameMode.MAP_BUILDER) }
                )
            }
            item {
                ModeCard(
                    title = "GLOBAL HALL OF FAME",
                    subtitle = "View fastest lap records and top drift scores worldwide",
                    icon = Icons.Default.EmojiEvents,
                    badgeColor = Color(0xFFFFD600),
                    testTag = "mode_leaderboard",
                    onClick = onOpenLeaderboard
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .border(1.dp, Color(0xFF1B2232), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111624))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.5.dp, badgeColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
