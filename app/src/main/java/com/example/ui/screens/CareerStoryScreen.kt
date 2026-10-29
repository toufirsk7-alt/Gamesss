package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.TrackCatalog
import com.example.model.TrackSpec
import com.example.model.UserProfile

data class CareerTier(
    val tierNumber: Int,
    val title: String,
    val requiredLevel: Int,
    val trackId: String,
    val rewardCash: Int,
    val rewardRep: Int,
    val description: String
)

data class StoryChapter(
    val chapterNumber: Int,
    val title: String,
    val rivalName: String,
    val rivalCar: String,
    val trackId: String,
    val cutsceneDialogue: String,
    val stakes: String
)

val CAREER_TIERS = listOf(
    CareerTier(1, "Rookie Street Debut", 1, "la_sunset", 5000, 50, "Prove your skills on the Sunset Highway against local tuners."),
    CareerTier(2, "Midnight Shuto Blitz", 2, "tokyo_shuto", 12000, 100, "High-speed night traffic weaving along Tokyo's illuminated C1 ring."),
    CareerTier(3, "Desert Mirage Sprint", 3, "dubai_desert", 20000, 150, "Flat-out top speed acceleration down the infinite Dubai dunes."),
    CareerTier(4, "Alpine Touge Climber", 4, "stelvio_pass", 35000, 250, "Master 48 brutal hairpin turns without touching the guardrails."),
    CareerTier(5, "Green Hell Nordschleife", 5, "nurburgring", 50000, 400, "The ultimate endurance trial through the perilous German forest."),
    CareerTier(6, "World Apex Championship", 6, "spa_francorchamps", 100000, 800, "Compete against international elite racing champions.")
)

val STORY_CHAPTERS = listOf(
    StoryChapter(
        chapterNumber = 1,
        title = "Chapter 1: The New Challenger",
        rivalName = "Ryker 'The Viper'",
        rivalCar = "BMW M8 Gran Coupe",
        trackId = "tokyo_shuto",
        cutsceneDialogue = "Ryker: 'You think you can roll into my city with that stock setup? Let's see if you can keep up on the elevated bypass!'",
        stakes = "Reputation & $15,000 Cash"
    ),
    StoryChapter(
        chapterNumber = 2,
        title = "Chapter 2: Desert Phantom",
        rivalName = "Kaelen 'Phantom'",
        rivalCar = "Porsche 911 GT3 RS",
        trackId = "dubai_desert",
        cutsceneDialogue = "Kaelen: 'Out here in the desert heat, only pure aerodynamic downforce survives. Meet me on the dunes at dusk.'",
        stakes = "Titanium Turbocharger Upgrade"
    ),
    StoryChapter(
        chapterNumber = 3,
        title = "Chapter 3: The Mountain King",
        rivalName = "Devraj 'Titan'",
        rivalCar = "Mahindra Scorpio Classic Dark",
        trackId = "leh_ladakh_pass",
        cutsceneDialogue = "Devraj: 'These icy Himalayan cliffs don't forgive mistakes. High ground clearance and raw diesel torque rule these peaks!'",
        stakes = "Heavy-Duty Suspension & $30,000"
    ),
    StoryChapter(
        chapterNumber = 4,
        title = "Chapter 4: The Black Series Shadow",
        rivalName = "Elena 'Valkyrie'",
        rivalCar = "Mercedes-AMG GT Black Series",
        trackId = "nurburgring",
        cutsceneDialogue = "Elena: 'You've reached the Green Hell. If you win, you take the trophy. If you lose, leave your keys on the hood.'",
        stakes = "Pink Slip Vehicle Ownership & World Crown"
    )
)

@Composable
fun CareerStoryScreen(
    userProfile: UserProfile,
    onStartRace: (TrackSpec, GameMode) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Career, 1: Story

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090C12))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("career_story_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text("CAMPAIGN & STORY", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("CAREER PROGRESSION • LEVEL ${userProfile.careerLevel}", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF0F141F),
            contentColor = Color(0xFF00E5FF),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF00E5FF)
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("CAREER TIERS", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("RIVAL STORY", fontWeight = FontWeight.Bold) }
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            if (selectedTab == 0) {
                // Career Tiers
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(CAREER_TIERS) { tier ->
                        val isUnlocked = userProfile.careerLevel >= tier.requiredLevel
                        val track = TrackCatalog.getTrack(tier.trackId)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnlocked) Color(0xFF141C2B) else Color(0xFF0F1218)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "TIER ${tier.tierNumber}",
                                            color = if (isUnlocked) Color(0xFF00E5FF) else Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• ${track.name}",
                                            color = Color.LightGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = tier.title,
                                        color = if (isUnlocked) Color.White else Color.Gray,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tier.description,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Reward: $${tier.rewardCash} • +${tier.rewardRep} REP",
                                        color = Color(0xFFFFD600),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isUnlocked) {
                                    Button(
                                        onClick = { onStartRace(track, GameMode.CAREER) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                        Text("RACE", color = Color.Black, fontWeight = FontWeight.Black)
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Rival Story Mode
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(STORY_CHAPTERS) { chapter ->
                        val track = TrackCatalog.getTrack(chapter.trackId)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141926)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(chapter.title.uppercase(), color = Color(0xFFFF1744), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Text("RIVAL: ${chapter.rivalName}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        Text("Drives: ${chapter.rivalCar}", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onStartRace(track, GameMode.STORY) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("DUEL", color = Color.White, fontWeight = FontWeight.Black)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Cutscene Dialogue Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0A0D14))
                                        .padding(10.dp)
                                ) {
                                    Text(chapter.cutsceneDialogue, color = Color(0xFFCFD8DC), fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("High Stakes: ${chapter.stakes}", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
