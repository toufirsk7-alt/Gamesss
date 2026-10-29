package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.AiRivalCar
import com.example.model.CameraView
import com.example.model.PedestrianNpc
import com.example.model.TrafficVehicle
import com.example.model.WeatherCondition
import com.example.physics.CarPhysics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class RoadSegment(
    val index: Int,
    val worldZ: Float,
    val curve: Float,
    val elevation: Float
)

class Racing3DEngine {
    val totalSegments = 400
    val segmentLength = 25f
    val roadWidth = 2400f
    val rumbleWidth = 220f
    val laneCount = 3

    val segments = ArrayList<RoadSegment>(totalSegments)

    init {
        generateTrackCurvature()
    }

    fun generateTrackCurvature() {
        segments.clear()
        for (i in 0 until totalSegments) {
            val z = i * segmentLength
            // Varied procedural track curvature and elevation waves
            val curve = sin(i * 0.045f) * 2.8f + cos(i * 0.08f) * 1.5f
            val elevation = sin(i * 0.03f) * 600f + cos(i * 0.07f) * 300f
            segments.add(RoadSegment(i, z, curve, elevation))
        }
    }

    fun renderScene(
        drawScope: DrawScope,
        physics: CarPhysics,
        cameraView: CameraView,
        weather: WeatherCondition,
        trafficList: List<TrafficVehicle>,
        npcList: List<PedestrianNpc>,
        rivalsList: List<AiRivalCar>,
        elapsedTimeSec: Float,
        activeNpcInteraction: PedestrianNpc?
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val halfW = width / 2f
        val horizonY = height * 0.44f

        // 1. Draw Sky & Atmosphere
        drawSkybox(drawScope, width, height, horizonY, weather, elapsedTimeSec)

        // 2. Draw 3D Road
        val camZ = physics.trackDistanceMeters
        val baseSegmentIdx = (camZ / segmentLength).toInt() % totalSegments
        val cameraHeight = when (cameraView) {
            CameraView.THIRD_PERSON -> 1200f
            CameraView.FIRST_PERSON_COCKPIT -> 750f
            CameraView.HOOD_CAM -> 500f
        }
        val camX = physics.carPositionX * (roadWidth * 0.45f)

        // Project and render road segments from back to front
        val drawDistance = 85
        var maxVisibleY = height

        data class ProjectedSegment(
            val screenX: Float,
            val screenY: Float,
            val screenW: Float,
            val segment: RoadSegment,
            val depthScale: Float
        )

        val projectedList = mutableListOf<ProjectedSegment>()
        var accumulatedCurve = 0f

        for (n in 0 until drawDistance) {
            val segIdx = (baseSegmentIdx + n) % totalSegments
            val seg = segments[segIdx]
            val relativeZ = (n * segmentLength) + (segmentLength - (camZ % segmentLength))
            if (relativeZ <= 5f) continue

            accumulatedCurve += seg.curve * 1.8f
            val scale = 380f / relativeZ
            val projX = halfW + (accumulatedCurve - camX) * scale
            val projY = horizonY + (cameraHeight - seg.elevation * 0.2f) * scale
            val projW = roadWidth * scale

            projectedList.add(ProjectedSegment(projX, projY, projW, seg, scale))
        }

        // Render road polygons back to front
        for (i in (projectedList.size - 1) downTo 1) {
            val p1 = projectedList[i - 1]
            val p2 = projectedList[i]

            if (p1.screenY >= maxVisibleY) continue

            val isAlternate = (p1.segment.index / 3) % 2 == 0

            // Grass / Ground
            val grassColor = when (weather) {
                WeatherCondition.SNOW -> if (isAlternate) Color(0xFFE0E5EC) else Color(0xFFCED4DE)
                WeatherCondition.GOLDEN_HOUR -> if (isAlternate) Color(0xFFC29B63) else Color(0xFFB58E55)
                WeatherCondition.MIDNIGHT_NEON -> if (isAlternate) Color(0xFF0F1524) else Color(0xFF0A0D18)
                else -> if (isAlternate) Color(0xFF1B381A) else Color(0xFF142C13)
            }
            drawScope.drawRect(
                color = grassColor,
                topLeft = Offset(0f, p2.screenY),
                size = Size(width, p1.screenY - p2.screenY)
            )

            // Rumble Strips / Curbs (Red and White)
            val rumbleColor = if (isAlternate) Color(0xFFD32F2F) else Color(0xFFECEFF1)
            val r1 = p1.screenW * 1.15f
            val r2 = p2.screenW * 1.15f
            val curbPath = Path().apply {
                moveTo(p1.screenX - r1, p1.screenY)
                lineTo(p2.screenX - r2, p2.screenY)
                lineTo(p2.screenX + r2, p2.screenY)
                lineTo(p1.screenX + r1, p1.screenY)
                close()
            }
            drawScope.drawPath(curbPath, rumbleColor, style = Fill)

            // Road Asphalt
            val roadColor = when (weather) {
                WeatherCondition.RAIN, WeatherCondition.THUNDERSTORM ->
                    if (isAlternate) Color(0xFF1A1C22) else Color(0xFF14151B) // Wet dark asphalt
                WeatherCondition.MIDNIGHT_NEON ->
                    if (isAlternate) Color(0xFF161A28) else Color(0xFF111420)
                else ->
                    if (isAlternate) Color(0xFF373B44) else Color(0xFF2C3038)
            }
            val roadPath = Path().apply {
                moveTo(p1.screenX - p1.screenW, p1.screenY)
                lineTo(p2.screenX - p2.screenW, p2.screenY)
                lineTo(p2.screenX + p2.screenW, p2.screenY)
                lineTo(p1.screenX + p1.screenW, p1.screenY)
                close()
            }
            drawScope.drawPath(roadPath, roadColor, style = Fill)

            // Lane Markings (White dashed lines)
            if (isAlternate) {
                val laneOffset1 = p1.screenW * 0.35f
                val laneOffset2 = p2.screenW * 0.35f
                // Left lane divider
                drawScope.drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(p1.screenX - laneOffset1, p1.screenY),
                    end = Offset(p2.screenX - laneOffset2, p2.screenY),
                    strokeWidth = 3f * p1.depthScale * 10f
                )
                // Right lane divider
                drawScope.drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(p1.screenX + laneOffset1, p1.screenY),
                    end = Offset(p2.screenX + laneOffset2, p2.screenY),
                    strokeWidth = 3f * p1.depthScale * 10f
                )
            }

            // Roadside Streetlights or Neon Towers every 12 segments
            if (p1.segment.index % 12 == 0) {
                val poleH = 750f * p1.depthScale
                val poleW = 8f * p1.depthScale
                // Left streetlight pole
                drawScope.drawRect(
                    color = Color(0xFF78909C),
                    topLeft = Offset(p1.screenX - p1.screenW * 1.25f, p1.screenY - poleH),
                    size = Size(poleW, poleH)
                )
                // Streetlight glowing fixture
                val lampColor = if (weather == WeatherCondition.MIDNIGHT_NEON) Color(0xFF00E5FF) else Color(0xFFFFD54F)
                drawScope.drawCircle(
                    color = lampColor.copy(alpha = 0.9f),
                    radius = 16f * p1.depthScale,
                    center = Offset(p1.screenX - p1.screenW * 1.25f + poleW / 2f, p1.screenY - poleH)
                )
            }
        }

        // 3. Headlights cone illumination in night or rain
        if (physics.headlightsOn || weather == WeatherCondition.MIDNIGHT_NEON || weather == WeatherCondition.THUNDERSTORM) {
            val beamPath = Path().apply {
                moveTo(halfW - 80f, height * 0.85f)
                lineTo(halfW - width * 0.45f, height * 0.48f)
                lineTo(halfW + width * 0.45f, height * 0.48f)
                lineTo(halfW + 80f, height * 0.85f)
                close()
            }
            drawScope.drawPath(
                beamPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x55E0F7FA), Color(0x00E0F7FA)),
                    startY = height * 0.85f,
                    endY = height * 0.48f
                )
            )
        }

        // 4. Draw Traffic Vehicles (Free roam mode)
        for (traffic in trafficList) {
            val relZ = traffic.z - camZ
            if (relZ in 5f..1800f) {
                val scale = 380f / relZ
                val screenX = halfW + (traffic.x * roadWidth * 0.4f - camX) * scale
                val screenY = horizonY + cameraHeight * scale
                draw3DCarModel(drawScope, screenX, screenY, scale * 1.4f, parseColor(traffic.colorHex), false, 0f, 0f, false, traffic.modelType)
            }
        }

        // 5. Draw Pedestrian NPCs on sidewalks
        for (npc in npcList) {
            val relZ = npc.z - camZ
            if (relZ in 5f..1200f) {
                val scale = 380f / relZ
                val screenX = halfW + (npc.x * roadWidth * 0.55f - camX) * scale
                val screenY = horizonY + cameraHeight * scale
                drawNpcFigure(drawScope, screenX, screenY, scale, npc, elapsedTimeSec)
            }
        }

        // 6. Draw AI Rival Cars (Racing mode)
        for (rival in rivalsList) {
            val relZ = rival.z - camZ
            if (relZ in 5f..2000f) {
                val scale = 380f / relZ
                val screenX = halfW + (rival.x * roadWidth * 0.4f - camX) * scale
                val screenY = horizonY + cameraHeight * scale
                draw3DCarModel(drawScope, screenX, screenY, scale * 1.45f, parseColor(rival.colorHex), false, 0f, 0f, false, rival.carModelKey)
            }
        }

        // 7. Render Player Vehicle or 1st Person Cockpit
        when (cameraView) {
            CameraView.THIRD_PERSON -> {
                val playerScale = 1.0f
                val playerScreenX = halfW + (physics.carPositionX * 40f)
                val playerScreenY = height * 0.78f

                // Underglow Neon on asphalt
                val underglowColor = parseColor(physics.garageCar?.underglowColorHex ?: "#00E5FF")
                drawScope.drawOval(
                    color = underglowColor.copy(alpha = 0.5f),
                    topLeft = Offset(playerScreenX - 160f, playerScreenY + 60f),
                    size = Size(320f, 45f)
                )

                // Draw 3D Player Car with Drift Angle & Damage
                draw3DCarModel(
                    drawScope = drawScope,
                    x = playerScreenX,
                    y = playerScreenY,
                    scale = playerScale,
                    bodyColor = parseColor(physics.garageCar?.paintColorHex ?: physics.baseSpec.primaryColorHex),
                    isPlayer = true,
                    driftAngle = physics.driftAngle,
                    damagePercent = physics.damagePercent,
                    isBraking = physics.brakeInput > 0.1f || physics.isHandbrakeActive,
                    modelKey = physics.baseSpec.key,
                    doorsOpen = physics.doorsOpen,
                    isNitro = physics.isNitroActive,
                    headlightsOn = physics.headlightsOn
                )
            }
            CameraView.FIRST_PERSON_COCKPIT -> {
                drawFirstPersonCockpit(drawScope, width, height, physics, weather, elapsedTimeSec)
            }
            CameraView.HOOD_CAM -> {
                // Hood reflection & wiper
                drawHoodView(drawScope, width, height, physics, weather)
            }
        }

        // 8. Rain / Snow particle overlays
        drawWeatherParticles(drawScope, width, height, weather, elapsedTimeSec)

        // 9. Interactive NPC dialogue banner if player stopped near an NPC
        if (activeNpcInteraction != null) {
            drawNpcDialogueBubble(drawScope, width, height, activeNpcInteraction)
        }
    }

    private fun drawSkybox(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        horizonY: Float,
        weather: WeatherCondition,
        elapsedTimeSec: Float
    ) {
        val skyBrush = when (weather) {
            WeatherCondition.SUNNY -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0288D1), Color(0xFF4FC3F7), Color(0xFFB3E5FC)),
                startY = 0f,
                endY = horizonY
            )
            WeatherCondition.GOLDEN_HOUR -> Brush.verticalGradient(
                colors = listOf(Color(0xFFE65100), Color(0xFFFF8F00), Color(0xFFFFCC80)),
                startY = 0f,
                endY = horizonY
            )
            WeatherCondition.RAIN -> Brush.verticalGradient(
                colors = listOf(Color(0xFF263238), Color(0xFF37474F), Color(0xFF546E7A)),
                startY = 0f,
                endY = horizonY
            )
            WeatherCondition.THUNDERSTORM -> {
                val lightningFlash = (elapsedTimeSec * 4f).toInt() % 17 == 0
                if (lightningFlash) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFF90A4AE)),
                        startY = 0f,
                        endY = horizonY
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF101318), Color(0xFF1C222C), Color(0xFF2A3442)),
                        startY = 0f,
                        endY = horizonY
                    )
                }
            }
            WeatherCondition.SNOW -> Brush.verticalGradient(
                colors = listOf(Color(0xFF78909C), Color(0xFFB0BEC5), Color(0xFFECEFF1)),
                startY = 0f,
                endY = horizonY
            )
            WeatherCondition.MIDNIGHT_NEON -> Brush.verticalGradient(
                colors = listOf(Color(0xFF070A12), Color(0xFF0D1424), Color(0xFF1A1F38)),
                startY = 0f,
                endY = horizonY
            )
        }

        drawScope.drawRect(brush = skyBrush, topLeft = Offset.Zero, size = Size(width, horizonY))

        // Distant mountains / city skyline on horizon
        val skylinePath = Path().apply {
            moveTo(0f, horizonY)
            var currentX = 0f
            while (currentX < width) {
                val buildingW = 40f + (currentX.toInt() % 60)
                val buildingH = 30f + (currentX.toInt() % 90)
                lineTo(currentX, horizonY - buildingH)
                lineTo(currentX + buildingW, horizonY - buildingH)
                currentX += buildingW
            }
            lineTo(width, horizonY)
            close()
        }
        val skylineColor = if (weather == WeatherCondition.MIDNIGHT_NEON) Color(0x880C1427) else Color(0x662C3440)
        drawScope.drawPath(skylinePath, skylineColor, style = Fill)
    }

    private fun draw3DCarModel(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        scale: Float,
        bodyColor: Color,
        isPlayer: Boolean,
        driftAngle: Float,
        damagePercent: Float,
        isBraking: Boolean,
        modelKey: String,
        doorsOpen: Boolean = false,
        isNitro: Boolean = false,
        headlightsOn: Boolean = false
    ) {
        val w = 220f * scale
        val h = 100f * scale

        drawScope.rotate(degrees = driftAngle * 0.4f, pivot = Offset(x, y)) {
            // Shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(x - w * 0.52f, y + h * 0.35f),
                size = Size(w * 1.04f, h * 0.32f)
            )

            // Exhaust Flames when Nitro active
            if (isNitro) {
                val flameW = 28f * scale
                val flameH = 50f * scale
                // Left exhaust
                drawOval(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFF3D00), Color(0xFF00E5FF), Color.Transparent)),
                    topLeft = Offset(x - w * 0.32f - flameW / 2f, y + h * 0.42f),
                    size = Size(flameW, flameH)
                )
                // Right exhaust
                drawOval(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFF3D00), Color(0xFF00E5FF), Color.Transparent)),
                    topLeft = Offset(x + w * 0.32f - flameW / 2f, y + h * 0.42f),
                    size = Size(flameW, flameH)
                )
            }

            // Tire Smoke on drift
            if (abs(driftAngle) > 6f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = 35f * scale,
                    center = Offset(x - w * 0.45f, y + h * 0.35f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = 35f * scale,
                    center = Offset(x + w * 0.45f, y + h * 0.35f)
                )
            }

            // 1. Rear bumper / lower chassis
            val bumperY = y + h * 0.1f
            drawRoundRect(
                color = Color(0xFF1E222A),
                topLeft = Offset(x - w * 0.48f, bumperY),
                size = Size(w * 0.96f, h * 0.42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * scale)
            )

            // 2. Main Body Shell
            val bodyPath = Path().apply {
                moveTo(x - w * 0.46f, bumperY)
                lineTo(x - w * 0.44f, y - h * 0.15f)
                // Cabin slope
                lineTo(x - w * 0.32f, y - h * 0.52f)
                lineTo(x + w * 0.32f, y - h * 0.52f)
                lineTo(x + w * 0.44f, y - h * 0.15f)
                lineTo(x + w * 0.46f, bumperY)
                close()
            }
            drawPath(bodyPath, bodyColor, style = Fill)
            drawPath(bodyPath, Color.Black.copy(alpha = 0.3f), style = Stroke(width = 2.5f * scale))

            // 3. Rear Windshield Glass
            val windowPath = Path().apply {
                moveTo(x - w * 0.38f, y - h * 0.12f)
                lineTo(x - w * 0.28f, y - h * 0.48f)
                lineTo(x + w * 0.28f, y - h * 0.48f)
                lineTo(x + w * 0.38f, y - h * 0.12f)
                close()
            }
            drawPath(
                windowPath,
                brush = Brush.verticalGradient(listOf(Color(0xFF1F2430), Color(0xFF384357))),
                style = Fill
            )

            // 4. Wheels & Alloys
            val wheelW = 28f * scale
            val wheelH = 46f * scale
            // Left Wheel
            drawRoundRect(
                color = Color(0xFF111111),
                topLeft = Offset(x - w * 0.50f, y + h * 0.18f),
                size = Size(wheelW, wheelH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale)
            )
            drawCircle(
                color = Color(0xFF757575),
                radius = 10f * scale,
                center = Offset(x - w * 0.50f + wheelW / 2f, y + h * 0.18f + wheelH / 2f)
            )
            // Right Wheel
            drawRoundRect(
                color = Color(0xFF111111),
                topLeft = Offset(x + w * 0.50f - wheelW, y + h * 0.18f),
                size = Size(wheelW, wheelH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale)
            )
            drawCircle(
                color = Color(0xFF757575),
                radius = 10f * scale,
                center = Offset(x + w * 0.50f - wheelW / 2f, y + h * 0.18f + wheelH / 2f)
            )

            // 5. LED Taillights (glow red, intensifies on braking)
            val tailLightColor = if (isBraking) Color(0xFFFF1744) else Color(0xFFD50000)
            val lightW = 38f * scale
            val lightH = 12f * scale
            // Left taillight
            drawRoundRect(
                color = tailLightColor,
                topLeft = Offset(x - w * 0.42f, y - h * 0.04f),
                size = Size(lightW, lightH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale)
            )
            // Right taillight
            drawRoundRect(
                color = tailLightColor,
                topLeft = Offset(x + w * 0.42f - lightW, y - h * 0.04f),
                size = Size(lightW, lightH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale)
            )

            // 6. GT Spoiler / Rear Wing
            val spoilerW = w * 0.88f
            val spoilerH = 10f * scale
            drawRoundRect(
                color = Color(0xFF121418), // Carbon fiber spoiler
                topLeft = Offset(x - spoilerW / 2f, y - h * 0.60f),
                size = Size(spoilerW, spoilerH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale)
            )
            // Spoiler mounts
            drawLine(
                color = Color(0xFF212121),
                start = Offset(x - spoilerW * 0.28f, y - h * 0.60f),
                end = Offset(x - spoilerW * 0.28f, y - h * 0.48f),
                strokeWidth = 3f * scale
            )
            drawLine(
                color = Color(0xFF212121),
                start = Offset(x + spoilerW * 0.28f, y - h * 0.60f),
                end = Offset(x + spoilerW * 0.28f, y - h * 0.48f),
                strokeWidth = 3f * scale
            )

            // 7. Animated Open Doors
            if (doorsOpen) {
                // Left scissor door
                drawLine(
                    color = bodyColor,
                    start = Offset(x - w * 0.35f, y - h * 0.25f),
                    end = Offset(x - w * 0.65f, y - h * 0.80f),
                    strokeWidth = 6f * scale
                )
                // Right scissor door
                drawLine(
                    color = bodyColor,
                    start = Offset(x + w * 0.35f, y - h * 0.25f),
                    end = Offset(x + w * 0.65f, y - h * 0.80f),
                    strokeWidth = 6f * scale
                )
            }

            // 8. Crash Damage Scratches & Sparks
            if (damagePercent > 10f) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.7f),
                    start = Offset(x - w * 0.2f, y),
                    end = Offset(x - w * 0.05f, y + h * 0.15f),
                    strokeWidth = 2.5f * scale
                )
            }
            if (damagePercent > 35f) {
                // Sparks showering
                for (s in 0..4) {
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = 2.5f * scale,
                        center = Offset(x - w * 0.35f + s * 14f, y + h * 0.32f + (s % 2) * 8f)
                    )
                }
            }
        }
    }

    private fun drawFirstPersonCockpit(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        physics: CarPhysics,
        weather: WeatherCondition,
        elapsedTimeSec: Float
    ) {
        val dashY = height * 0.60f

        // 1. Windshield Frame & A-Pillars
        val pillarPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(width * 0.12f, dashY)
            lineTo(0f, dashY)
            close()
        }
        drawScope.drawPath(pillarPath, Color(0xFF181B22), style = Fill)

        val rightPillarPath = Path().apply {
            moveTo(width, 0f)
            lineTo(width * 0.88f, dashY)
            lineTo(width, dashY)
            close()
        }
        drawScope.drawPath(rightPillarPath, Color(0xFF181B22), style = Fill)

        // 2. Windshield Wipers Animation during Rain
        if (weather == WeatherCondition.RAIN || weather == WeatherCondition.THUNDERSTORM || physics.wipersActive) {
            val wiperAngle = sin(elapsedTimeSec * 6f) * 45f - 20f
            drawScope.rotate(degrees = wiperAngle, pivot = Offset(width * 0.35f, dashY)) {
                drawScope.drawLine(
                    color = Color(0xFF212121),
                    start = Offset(width * 0.35f, dashY),
                    end = Offset(width * 0.35f, dashY - height * 0.42f),
                    strokeWidth = 6f
                )
            }
        }

        // 3. Cracked Windshield Spiderweb if car has taken crash damage
        if (physics.damagePercent > 20f) {
            val crackCenter = Offset(width * 0.65f, height * 0.32f)
            for (i in 0..6) {
                val rad = (i * 60f) * PI.toFloat() / 180f
                drawScope.drawLine(
                    color = Color.White.copy(alpha = 0.65f),
                    start = crackCenter,
                    end = Offset(crackCenter.x + cos(rad) * 90f, crackCenter.y + sin(rad) * 90f),
                    strokeWidth = 2.5f
                )
            }
        }

        // 4. Main Dashboard Base
        val dashPath = Path().apply {
            moveTo(0f, dashY)
            lineTo(width * 0.12f, dashY)
            lineTo(width * 0.30f, dashY + 40f)
            lineTo(width * 0.70f, dashY + 40f)
            lineTo(width * 0.88f, dashY)
            lineTo(width, dashY)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawScope.drawPath(
            dashPath,
            brush = Brush.verticalGradient(listOf(Color(0xFF1E222A), Color(0xFF111418))),
            style = Fill
        )

        // 5. In-Dash Instrument Cluster (Speedometer, Tachometer, Gear)
        val clusterW = 320f
        val clusterH = 120f
        val clusterX = width / 2f - clusterW / 2f
        val clusterY = dashY + 60f

        drawScope.drawRoundRect(
            color = Color(0xFF0A0D14),
            topLeft = Offset(clusterX, clusterY),
            size = Size(clusterW, clusterH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f)
        )
        drawScope.drawRoundRect(
            color = Color(0xFF00E5FF).copy(alpha = 0.4f),
            topLeft = Offset(clusterX, clusterY),
            size = Size(clusterW, clusterH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f),
            style = Stroke(width = 2f)
        )

        // RPM Tachometer Bar
        val rpmRatio = (physics.rpm / 8500f).coerceIn(0f, 1f)
        drawScope.drawRect(
            color = if (rpmRatio > 0.85f) Color(0xFFFF1744) else Color(0xFF00E5FF),
            topLeft = Offset(clusterX + 20f, clusterY + 20f),
            size = Size((clusterW - 40f) * rpmRatio, 16f)
        )

        // 6. Realistic 3D Steering Wheel (Rotates dynamically with steering input!)
        val wheelCenter = Offset(width / 2f, height * 0.84f)
        val wheelRadius = 140f
        val wheelRotation = physics.steeringInput * 65f

        drawScope.rotate(degrees = wheelRotation, pivot = wheelCenter) {
            // Outer Rim
            drawCircle(
                color = Color(0xFF1F2430),
                radius = wheelRadius,
                center = wheelCenter,
                style = Stroke(width = 26f)
            )
            // Carbon Stitching Accent
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = wheelRadius - 12f,
                center = wheelCenter,
                style = Stroke(width = 2f)
            )
            // Steering Spokes (3-spoke performance wheel)
            drawLine(
                color = Color(0xFF2C3240),
                start = wheelCenter,
                end = Offset(wheelCenter.x - wheelRadius, wheelCenter.y),
                strokeWidth = 22f
            )
            drawLine(
                color = Color(0xFF2C3240),
                start = wheelCenter,
                end = Offset(wheelCenter.x + wheelRadius, wheelCenter.y),
                strokeWidth = 22f
            )
            drawLine(
                color = Color(0xFF2C3240),
                start = wheelCenter,
                end = Offset(wheelCenter.x, wheelCenter.y + wheelRadius),
                strokeWidth = 22f
            )
            // Center Hub & Emblem
            drawCircle(
                color = Color(0xFF111418),
                radius = 36f,
                center = wheelCenter
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 16f,
                center = wheelCenter
            )
        }
    }

    private fun drawHoodView(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        physics: CarPhysics,
        weather: WeatherCondition
    ) {
        val hoodY = height * 0.80f
        val hoodPath = Path().apply {
            moveTo(width * 0.15f, height)
            lineTo(width * 0.28f, hoodY)
            lineTo(width * 0.72f, hoodY)
            lineTo(width * 0.85f, height)
            close()
        }
        val carColor = parseColor(physics.garageCar?.paintColorHex ?: physics.baseSpec.primaryColorHex)
        drawScope.drawPath(hoodPath, carColor, style = Fill)
    }

    private fun drawNpcFigure(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        scale: Float,
        npc: PedestrianNpc,
        elapsedTimeSec: Float
    ) {
        val h = 110f * scale
        val w = 32f * scale

        // Head
        drawScope.drawCircle(
            color = Color(0xFFFFCC80),
            radius = 12f * scale,
            center = Offset(x, y - h)
        )
        // Torso / Jacket
        drawScope.drawRoundRect(
            color = Color(0xFF1E88E5),
            topLeft = Offset(x - w / 2f, y - h + 14f * scale),
            size = Size(w, 42f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale)
        )
        // Walking Legs
        val legSwing = sin(elapsedTimeSec * 8f) * 12f * scale
        drawScope.drawLine(
            color = Color(0xFF212121),
            start = Offset(x - 6f * scale, y - h + 56f * scale),
            end = Offset(x - 6f * scale - legSwing, y),
            strokeWidth = 7f * scale
        )
        drawScope.drawLine(
            color = Color(0xFF212121),
            start = Offset(x + 6f * scale, y - h + 56f * scale),
            end = Offset(x + 6f * scale + legSwing, y),
            strokeWidth = 7f * scale
        )

        // Interactive speech exclamation marker
        drawScope.drawCircle(
            color = Color(0xFFFFD600),
            radius = 10f * scale,
            center = Offset(x, y - h - 18f * scale)
        )
    }

    private fun drawNpcDialogueBubble(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        npc: PedestrianNpc
    ) {
        val bubbleW = min(width * 0.85f, 480f)
        val bubbleH = 80f
        val bubbleX = width / 2f - bubbleW / 2f
        val bubbleY = height * 0.22f

        drawScope.drawRoundRect(
            color = Color(0xEE0B0E14),
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleW, bubbleH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f)
        )
        drawScope.drawRoundRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleW, bubbleH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f),
            style = Stroke(width = 2f)
        )
    }

    private fun drawWeatherParticles(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        weather: WeatherCondition,
        elapsedTimeSec: Float
    ) {
        when (weather) {
            WeatherCondition.RAIN, WeatherCondition.THUNDERSTORM -> {
                for (i in 0..50) {
                    val px = ((i * 47 + (elapsedTimeSec * 1600f).toInt()) % width.toInt()).toFloat()
                    val py = ((i * 83 + (elapsedTimeSec * 2200f).toInt()) % height.toInt()).toFloat()
                    drawScope.drawLine(
                        color = Color(0x99B0BEC5),
                        start = Offset(px, py),
                        end = Offset(px - 14f, py + 32f),
                        strokeWidth = 2f
                    )
                }
            }
            WeatherCondition.SNOW -> {
                for (i in 0..40) {
                    val px = ((i * 61 + (sin(elapsedTimeSec + i) * 60f)) % width).toFloat()
                    val py = ((i * 73 + (elapsedTimeSec * 200f).toInt()) % height.toInt()).toFloat()
                    drawScope.drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = 3.5f,
                        center = Offset(px, py)
                    )
                }
            }
            else -> {}
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
}
