package com.example.physics

import com.example.model.CarSpec
import com.example.model.GarageCar
import com.example.model.TransmissionMode
import com.example.model.WeatherCondition
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CarPhysics(
    val baseSpec: CarSpec,
    val garageCar: GarageCar? = null
) {
    // Current dynamic states
    var speedKmh: Float = 0f
    var rpm: Float = 900f
    var currentGear: Int = 1 // -1 = R, 0 = N, 1..6
    var transmissionMode: TransmissionMode = TransmissionMode.AUTOMATIC

    var throttleInput: Float = 0f
    var brakeInput: Float = 0f
    var steeringInput: Float = 0f // -1.0 (full left) to +1.0 (full right)
    var isHandbrakeActive: Boolean = false
    var isNitroActive: Boolean = false

    var carPositionX: Float = 0f // -2.0 to 2.0 on road
    var trackDistanceMeters: Float = 0f

    // Drift physics
    var driftAngle: Float = 0f // in degrees
    var isDrifting: Boolean = false
    var currentDriftScore: Int = 0
    var totalDriftScore: Int = 0
    var driftMultiplier: Float = 1.0f

    // Nitro bottle
    var nitroAmount: Float = 100f // 0 to 100%
    val maxNitro: Float = 100f

    // Damage & Crash
    var damagePercent: Float = 0f // 0 to 100%
    var recentImpact: Boolean = false

    // Car accessories & controls
    var headlightsOn: Boolean = false
    var doorsOpen: Boolean = false
    var hornActive: Boolean = false
    var wipersActive: Boolean = false

    // Gear top speeds (km/h) for default tuning
    private val gearMaxSpeeds = floatArrayOf(
        0f,   // index 0 unused
        65f,  // 1st
        115f, // 2nd
        165f, // 3rd
        220f, // 4th
        275f, // 5th
        340f  // 6th
    )

    // Upgrades multiplier
    private val engineMultiplier: Float
        get() = 1.0f + ((garageCar?.engineLevel ?: 1) - 1) * 0.08f + ((garageCar?.turboLevel ?: 0)) * 0.10f

    private val brakesMultiplier: Float
        get() = 1.0f + ((garageCar?.brakesLevel ?: 1) - 1) * 0.12f

    private val suspensionMultiplier: Float
        get() = 1.0f + ((garageCar?.suspensionLevel ?: 1) - 1) * 0.10f

    val effectiveTopSpeedKmh: Float
        get() = (baseSpec.topSpeedKmh * engineMultiplier).coerceAtMost(380f)

    fun update(deltaTimeSec: Float, weather: WeatherCondition, onGearShift: () -> Unit = {}) {
        val dt = deltaTimeSec.coerceIn(0.001f, 0.1f)
        val grip = weather.roadGripFactor * suspensionMultiplier

        // 1. Transmission & Gear shifting
        if (transmissionMode == TransmissionMode.AUTOMATIC) {
            if (currentGear in 1..5 && speedKmh > gearMaxSpeeds[currentGear] * 0.92f && throttleInput > 0.5f) {
                currentGear++
                onGearShift()
            } else if (currentGear in 2..6 && speedKmh < gearMaxSpeeds[currentGear - 1] * 0.70f) {
                currentGear--
                onGearShift()
            }
        }

        // 2. Nitro Boost usage & regeneration
        val nitroBoost = if (isNitroActive && nitroAmount > 1f) {
            nitroAmount = max(0f, nitroAmount - 25f * dt)
            1.6f
        } else {
            isNitroActive = false
            1.0f
        }

        // Slowly regenerate nitro when driving fast and drifting
        if (!isNitroActive && nitroAmount < maxNitro) {
            val regenRate = if (isDrifting) 8f else 1.5f
            nitroAmount = min(maxNitro, nitroAmount + regenRate * dt)
        }

        // 3. Acceleration & Braking
        val effectiveHp = baseSpec.horsepower * engineMultiplier * nitroBoost
        val baseAccel = (effectiveHp / (baseSpec.weightKg * 0.18f)) * grip

        if (currentGear == -1) {
            // Reverse
            if (throttleInput > 0.05f) {
                speedKmh = max(-50f, speedKmh - baseAccel * 0.4f * throttleInput * dt * 3.6f)
            } else if (brakeInput > 0.05f) {
                speedKmh = min(0f, speedKmh + 40f * brakesMultiplier * brakeInput * dt)
            } else {
                speedKmh *= (1f - 0.5f * dt)
            }
        } else if (currentGear == 0) {
            // Neutral
            speedKmh *= (1f - 0.3f * dt)
        } else {
            // Forward gears
            if (throttleInput > 0.05f) {
                val speedRatio = (speedKmh / effectiveTopSpeedKmh).coerceIn(0f, 1f)
                val gearTorqueRatio = (7 - currentGear).toFloat() / 6f
                val accel = baseAccel * (1f - speedRatio * 0.6f) * gearTorqueRatio * throttleInput
                speedKmh = min(effectiveTopSpeedKmh, speedKmh + accel * dt * 3.6f)
            }

            if (brakeInput > 0.05f) {
                val brakeForce = 55f * brakesMultiplier * brakeInput
                speedKmh = max(0f, speedKmh - brakeForce * dt * 3.6f)
            }

            if (isHandbrakeActive) {
                speedKmh = max(0f, speedKmh - 75f * brakesMultiplier * dt * 3.6f)
            }

            // Natural aerodynamic drag & rolling resistance
            val drag = (0.00045f * speedKmh * speedKmh + 2.5f) * dt
            speedKmh = max(0f, speedKmh - drag)
        }

        // 4. Engine RPM Calculation
        if (currentGear == 0) {
            // Free revving in neutral
            val targetRpm = if (throttleInput > 0.1f) 8200f * throttleInput else 900f
            rpm += (targetRpm - rpm) * 8f * dt
        } else if (currentGear == -1) {
            rpm = (900f + abs(speedKmh) / 50f * 5500f).coerceIn(900f, 7500f)
        } else {
            val maxGearSpeed = gearMaxSpeeds[currentGear]
            val prevGearSpeed = if (currentGear > 1) gearMaxSpeeds[currentGear - 1] * 0.7f else 0f
            val ratio = ((speedKmh - prevGearSpeed) / (maxGearSpeed - prevGearSpeed)).coerceIn(0f, 1.1f)
            val computedRpm = 1200f + ratio * 6800f
            rpm = computedRpm.coerceIn(850f, 8800f)
        }

        // 5. Steering & Lateral Movement
        val steerSpeedFactor = (speedKmh / 160f).coerceIn(0.2f, 1.0f)
        val lateralMove = steeringInput * (speedKmh / 3.6f) * 0.015f * steerSpeedFactor * grip * dt
        carPositionX = (carPositionX + lateralMove).coerceIn(-2.2f, 2.2f)

        // 6. Drift Physics
        val lateralSlip = abs(steeringInput) * (speedKmh / 70f)
        if ((isHandbrakeActive || (lateralSlip > 1.4f && grip < 0.95f)) && speedKmh > 35f) {
            isDrifting = true
            val targetAngle = steeringInput * 35f * (1.1f - grip * 0.2f)
            driftAngle += (targetAngle - driftAngle) * 6f * dt
            driftMultiplier = min(4.0f, driftMultiplier + 0.5f * dt)
            val addedScore = (abs(driftAngle) * 5f * driftMultiplier * dt).toInt()
            currentDriftScore += addedScore
        } else {
            if (isDrifting && currentDriftScore > 0) {
                totalDriftScore += currentDriftScore
                currentDriftScore = 0
                driftMultiplier = 1.0f
            }
            isDrifting = false
            driftAngle *= (1f - 8f * dt)
        }

        // 7. Track progression
        val distanceTraveled = (speedKmh / 3.6f) * dt
        trackDistanceMeters += distanceTraveled
    }

    fun applyCollisionDamage(intensity: Float) {
        val clampIntensity = intensity.coerceIn(0.1f, 1.0f)
        damagePercent = min(100f, damagePercent + clampIntensity * 20f)
        speedKmh = max(0f, speedKmh * (1f - clampIntensity * 0.45f))
        recentImpact = true
    }

    fun shiftUp(onShift: () -> Unit) {
        if (currentGear < 6) {
            currentGear++
            onShift()
        }
    }

    fun shiftDown(onShift: () -> Unit) {
        if (currentGear > -1) {
            currentGear--
            onShift()
        }
    }

    fun toggleHeadlights() {
        headlightsOn = !headlightsOn
    }

    fun toggleDoors() {
        doorsOpen = !doorsOpen
    }

    fun toggleWipers() {
        wipersActive = !wipersActive
    }
}
