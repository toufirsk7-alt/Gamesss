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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CarCatalog
import com.example.model.CarSpec
import com.example.model.Drivetrain
import com.example.model.EngineType
import com.example.model.GarageCar
import com.example.model.UserProfile
import kotlin.math.roundToInt

@Composable
fun GarageScreen(
    userProfile: UserProfile,
    ownedCars: List<GarageCar>,
    selectedCarSpec: CarSpec,
    selectedGarageCar: GarageCar?,
    onSelectCar: (CarSpec) -> Unit,
    onSaveGarageCar: (GarageCar) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Select Car, 1: Performance Tuning, 2: Visual Customization, 3: Build Your Own Car

    val availableColors = listOf(
        "#00E5FF" to "Cyan Neon",
        "#FF1744" to "Apex Red",
        "#FFD600" to "Cyber Gold",
        "#76FF03" to "Acid Green",
        "#FF6D00" to "Nitro Orange",
        "#E040FB" to "Ultraviolet",
        "#ECEFF1" to "Pure White",
        "#111418" to "Carbon Stealth"
    )

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
                    modifier = Modifier.testTag("garage_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "APEX GARAGE",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "CUSTOMIZE & TUNE VEHICLES",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cash and Reputation Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141923))
                    .border(1.dp, Color(0xFFFFD600), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$${userProfile.cash}",
                    color = Color(0xFFFFD600),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 3D Showroom Rotating Preview Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF141A28), Color(0xFF0A0D15))
                    )
                )
                .border(1.dp, Color(0xFF1E2838), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val paintHex = selectedGarageCar?.paintColorHex ?: selectedCarSpec.primaryColorHex
            val underglowHex = selectedGarageCar?.underglowColorHex ?: "#00E5FF"

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f + 25f

                // Underglow Neon Spotlight
                drawOval(
                    color = parseColor(underglowHex).copy(alpha = 0.45f),
                    topLeft = Offset(cx - 170f, cy + 25f),
                    size = Size(340f, 40f)
                )

                // Showroom Floor Turntable Ring
                drawOval(
                    color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    topLeft = Offset(cx - 210f, cy + 15f),
                    size = Size(420f, 60f),
                    style = Stroke(width = 2f)
                )

                // 3D Car Showroom Silhouette
                val carW = 280f
                val carH = 110f
                val bodyColor = parseColor(paintHex)

                // Main Chassis
                val carPath = Path().apply {
                    moveTo(cx - carW * 0.46f, cy + carH * 0.25f)
                    lineTo(cx - carW * 0.44f, cy - carH * 0.12f)
                    lineTo(cx - carW * 0.32f, cy - carH * 0.48f)
                    lineTo(cx + carW * 0.32f, cy - carH * 0.48f)
                    lineTo(cx + carW * 0.44f, cy - carH * 0.12f)
                    lineTo(cx + carW * 0.46f, cy + carH * 0.25f)
                    close()
                }
                drawPath(carPath, bodyColor, style = Fill)
                drawPath(carPath, Color.Black.copy(alpha = 0.35f), style = Stroke(width = 3f))

                // Windshield Glass
                val glassPath = Path().apply {
                    moveTo(cx - carW * 0.36f, cy - carH * 0.10f)
                    lineTo(cx - carW * 0.26f, cy - carH * 0.44f)
                    lineTo(cx + carW * 0.26f, cy - carH * 0.44f)
                    lineTo(cx + carW * 0.36f, cy - carH * 0.10f)
                    close()
                }
                drawPath(glassPath, Color(0xFF1E2838), style = Fill)

                // Wheels
                drawRoundRect(
                    color = Color(0xFF111111),
                    topLeft = Offset(cx - carW * 0.48f, cy + carH * 0.20f),
                    size = Size(34f, 46f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                )
                drawRoundRect(
                    color = Color(0xFF111111),
                    topLeft = Offset(cx + carW * 0.48f - 34f, cy + carH * 0.20f),
                    size = Size(34f, 46f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                )

                // Taillights
                drawRoundRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(cx - carW * 0.42f, cy - carH * 0.05f),
                    size = Size(42f, 12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
                drawRoundRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(cx + carW * 0.42f - 42f, cy - carH * 0.05f),
                    size = Size(42f, 12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )

                // Rear GT Spoiler
                drawRoundRect(
                    color = Color(0xFF101318),
                    topLeft = Offset(cx - carW * 0.44f, cy - carH * 0.58f),
                    size = Size(carW * 0.88f, 12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
            }

            // Car Specs Overlay at Top
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = selectedCarSpec.brand.uppercase(),
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedCarSpec.modelName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${selectedCarSpec.horsepower} HP | ${selectedCarSpec.topSpeedKmh} KM/H | 0-100: ${selectedCarSpec.zeroToHundredSeconds}s",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Tabs
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
                text = { Text("SELECT CAR", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("PERFORMANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("VISUALS", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("BUILD CUSTOM", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            when (selectedTab) {
                0 -> {
                    // Car Selector Roster
                    CarSelectorList(
                        cars = CarCatalog.ALL_CARS,
                        selectedCar = selectedCarSpec,
                        onSelectCar = onSelectCar
                    )
                }
                1 -> {
                    // Performance Component Tuning
                    PerformanceTuningTab(
                        carSpec = selectedCarSpec,
                        garageCar = selectedGarageCar ?: GarageCar(
                            id = "car_${selectedCarSpec.key}",
                            userId = userProfile.userId,
                            carModelKey = selectedCarSpec.key,
                            name = selectedCarSpec.modelName
                        ),
                        cash = userProfile.cash,
                        onSaveGarageCar = onSaveGarageCar
                    )
                }
                2 -> {
                    // Visual Customization (Paint, Underglow, Spoilers, Bodykit, Window Tint)
                    VisualCustomizationTab(
                        garageCar = selectedGarageCar ?: GarageCar(
                            id = "car_${selectedCarSpec.key}",
                            userId = userProfile.userId,
                            carModelKey = selectedCarSpec.key,
                            name = selectedCarSpec.modelName
                        ),
                        availableColors = availableColors,
                        onSaveGarageCar = onSaveGarageCar
                    )
                }
                3 -> {
                    // Build Your Own Car
                    BuildYourOwnCarTab(
                        userProfile = userProfile,
                        onCarCreated = { newCar ->
                            onSaveGarageCar(newCar)
                            onSelectCar(CarCatalog.getCar("custom_builder"))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CarSelectorList(
    cars: List<CarSpec>,
    selectedCar: CarSpec,
    onSelectCar: (CarSpec) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(cars) { car ->
            val isSelected = car.key == selectedCar.key
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectCar(car) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2636),
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF152030) else Color(0xFF101520)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = car.brand,
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${car.category}",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = car.modelName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${car.horsepower} HP | Top: ${car.topSpeedKmh} km/h | 0-100: ${car.zeroToHundredSeconds}s | ${car.drivetrain}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Car",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceTuningTab(
    carSpec: CarSpec,
    garageCar: GarageCar,
    cash: Int,
    onSaveGarageCar: (GarageCar) -> Unit
) {
    var engineLevel by remember { mutableIntStateOf(garageCar.engineLevel) }
    var turboLevel by remember { mutableIntStateOf(garageCar.turboLevel) }
    var suspensionLevel by remember { mutableIntStateOf(garageCar.suspensionLevel) }
    var transmissionLevel by remember { mutableIntStateOf(garageCar.transmissionLevel) }
    var brakesLevel by remember { mutableIntStateOf(garageCar.brakesLevel) }
    var nitroLevel by remember { mutableIntStateOf(garageCar.nitroLevel) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TuningItemCard(
                    title = "Engine ECU & Intake",
                    level = engineLevel,
                    maxLevel = 5,
                    description = "+8% Horsepower & Throttle Response per tier",
                    onUpgrade = {
                        if (engineLevel < 5) engineLevel++
                    }
                )
            }
            item {
                TuningItemCard(
                    title = "Turbocharger / Supercharger",
                    level = turboLevel,
                    maxLevel = 5,
                    description = "+10% High-RPM boost & Top Speed per tier",
                    onUpgrade = {
                        if (turboLevel < 5) turboLevel++
                    }
                )
            }
            item {
                TuningItemCard(
                    title = "Coilovers & Anti-Roll Suspension",
                    level = suspensionLevel,
                    maxLevel = 5,
                    description = "+10% Cornering Grip & Drift Control",
                    onUpgrade = {
                        if (suspensionLevel < 5) suspensionLevel++
                    }
                )
            }
            item {
                TuningItemCard(
                    title = "Sequential Transmission",
                    level = transmissionLevel,
                    maxLevel = 5,
                    description = "Quicker shift timing & optimized gear ratios",
                    onUpgrade = {
                        if (transmissionLevel < 5) transmissionLevel++
                    }
                )
            }
            item {
                TuningItemCard(
                    title = "Carbon Ceramic Brakes & ABS",
                    level = brakesLevel,
                    maxLevel = 5,
                    description = "+12% Stopping Power and brake fade resistance",
                    onUpgrade = {
                        if (brakesLevel < 5) brakesLevel++
                    }
                )
            }
            item {
                TuningItemCard(
                    title = "Nitrous Oxide Injection Bottle",
                    level = nitroLevel,
                    maxLevel = 5,
                    description = "Boost capacity and duration multiplier",
                    onUpgrade = {
                        if (nitroLevel < 5) nitroLevel++
                    }
                )
            }
        }

        Button(
            onClick = {
                onSaveGarageCar(
                    garageCar.copy(
                        engineLevel = engineLevel,
                        turboLevel = turboLevel,
                        suspensionLevel = suspensionLevel,
                        transmissionLevel = transmissionLevel,
                        brakesLevel = brakesLevel,
                        nitroLevel = nitroLevel
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_tuning_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("INSTALL & SAVE TUNING", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun TuningItemCard(
    title: String,
    level: Int,
    maxLevel: Int,
    description: String,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(description, color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..maxLevel) {
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= level) Color(0xFF00E5FF) else Color(0xFF263238))
                        )
                    }
                }
            }

            if (level < maxLevel) {
                Button(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ TIER", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("MAX", color = Color(0xFFFFD600), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun VisualCustomizationTab(
    garageCar: GarageCar,
    availableColors: List<Pair<String, String>>,
    onSaveGarageCar: (GarageCar) -> Unit
) {
    var selectedPaint by remember { mutableStateOf(garageCar.paintColorHex) }
    var selectedUnderglow by remember { mutableStateOf(garageCar.underglowColorHex) }
    var spoilerType by remember { mutableIntStateOf(garageCar.spoilerType) }
    var tintLevel by remember { mutableIntStateOf(garageCar.windowTintLevel) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Car Paint Palette
        Text("CAR BODY PAINT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(availableColors) { (hex, name) ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(parseColor(hex))
                        .clickable { selectedPaint = hex }
                        .border(
                            width = if (selectedPaint == hex) 3.dp else 1.dp,
                            color = if (selectedPaint == hex) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }

        // Underglow Neon Palette
        Text("NEON UNDERGLOW ILLUMINATION", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(availableColors) { (hex, name) ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(parseColor(hex))
                        .clickable { selectedUnderglow = hex }
                        .border(
                            width = if (selectedUnderglow == hex) 3.dp else 1.dp,
                            color = if (selectedUnderglow == hex) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }

        // Window Tint
        Text("WINDOW TINT LEVEL: $tintLevel%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0, 30, 60, 90).forEach { tint ->
                Button(
                    onClick = { tintLevel = tint },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tintLevel == tint) Color(0xFF00E5FF) else Color(0xFF1E2838)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("$tint%", color = if (tintLevel == tint) Color.Black else Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onSaveGarageCar(
                    garageCar.copy(
                        paintColorHex = selectedPaint,
                        underglowColorHex = selectedUnderglow,
                        windowTintLevel = tintLevel,
                        spoilerType = spoilerType
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_paint_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("APPLY CUSTOM LOOK", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun BuildYourOwnCarTab(
    userProfile: UserProfile,
    onCarCreated: (GarageCar) -> Unit
) {
    var customCarName by remember { mutableStateOf("Apex Phantom") }
    var selectedEngine by remember { mutableStateOf(EngineType.V8_BITURBO) }
    var selectedDrivetrain by remember { mutableStateOf(Drivetrain.AWD) }
    var selectedPaint by remember { mutableStateOf("#00E5FF") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "CHASSIS & POWERTRAIN ASSEMBLY",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )

        OutlinedTextField(
            value = customCarName,
            onValueChange = { customCarName = it },
            label = { Text("Vehicle Project Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF263238),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Text("SELECT ENGINE PLATFORM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(EngineType.V8_BITURBO, EngineType.V10_NATURALLY_ASPIRATED, EngineType.INLINE_6_TWIN_TURBO, EngineType.DUAL_ELECTRIC_MOTOR)) { engine ->
                val isSelected = engine == selectedEngine
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2838))
                        .clickable { selectedEngine = engine }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = engine.name.replace("_", " "),
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text("DRIVETRAIN", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Drivetrain.RWD, Drivetrain.AWD, Drivetrain.FWD).forEach { dt ->
                val isSelected = dt == selectedDrivetrain
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2838))
                        .clickable { selectedDrivetrain = dt }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = dt.name,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val newCar = GarageCar(
                    id = "custom_${System.currentTimeMillis()}",
                    userId = userProfile.userId,
                    carModelKey = "custom_builder",
                    name = customCarName,
                    isCustomBuild = true,
                    paintColorHex = selectedPaint,
                    engineLevel = 2,
                    turboLevel = 1,
                    suspensionLevel = 2,
                    transmissionLevel = 2,
                    brakesLevel = 2,
                    nitroLevel = 2
                )
                onCarCreated(newCar)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("build_car_submit_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("FORGE & ASSEMBLE VEHICLE", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        Color(0xFF00E5FF)
    }
}
