package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "AiRaceEngineer"

class AiRaceEngineer {

    // General purpose flash model for quick pit radio and real-world track context
    private val flashModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    // High thinking model for deep telemetry analysis, suspension setup & race strategy
    private val argonThinkingModel by lazy {
        try {
            Firebase.ai.generativeModel(
                modelName = "gemini-4-argon"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to flash model: ${e.message}")
            flashModel
        }
    }

    /**
     * Deep telemetry race engineer analysis using high-reasoning Gemini 4 Argon.
     * Evaluates speed curve, RPM shifts, drift angles, damage deformation, and provides pro setup tips.
     */
    suspend fun analyzeTelemetry(
        telemetrySummary: String,
        carName: String,
        trackName: String
    ): String {
        return try {
            withTimeout(45_000L) {
                val prompt = """
                    You are the Chief Race Engineer for Apex Racing Team.
                    Analyze this car telemetry and provide expert setup recommendations:
                    - Car: $carName
                    - Track: $trackName
                    - Telemetry Data: $telemetrySummary
                    
                    Provide a concise, high-impact breakdown:
                    1. Apex & Cornering Efficiency (evaluate lateral drift and slip)
                    2. Powerband & Gear Shift Optimization (RPM efficiency)
                    3. Recommended Garage Tuning Upgrades (Turbo, Suspension stiffness, Brakes, Nitro timing)
                    4. Pro Racing Line Advice for this specific circuit
                """.trimIndent()

                val response = argonThinkingModel.generateContent(prompt)
                response.text ?: "Telemetry recorded cleanly. Maintain optimal tire temperature and clip the apex."
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Telemetry analysis timed out", e)
            "Radio interference: Telemetry upload timed out. Keep pushing!"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Telemetry failure", e)
            "Engineer Radio: Solid lap! Stiffen front suspension by 10% to reduce understeer on high-speed sweeps."
        }
    }

    /**
     * Maps grounding & real-world circuit knowledge using Gemini 3.8 Flash.
     * Fetches real location landmarks, asphalt characteristics, historical weather, and famous corners.
     */
    suspend fun fetchTrackRealWorldDetails(
        trackName: String,
        location: String,
        country: String,
        coordinates: String
    ): String {
        return try {
            withTimeout(30_000L) {
                val prompt = """
                    Act as an international motorsport scout with access to Google Maps real world geographical data.
                    For this racing venue:
                    Track Name: $trackName
                    Location: $location, $country
                    Coordinates: $coordinates
                    
                    Describe:
                    - Real world geographical context and famous landmarks
                    - Elevation profile and typical local weather patterns
                    - Famous real-world corners and tactical overtaking spots
                    Keep it exciting, immersive and under 150 words.
                """.trimIndent()

                val response = flashModel.generateContent(prompt)
                response.text ?: "$trackName in $location features iconic high-speed sections and demanding corner complexes."
            }
        } catch (e: TimeoutCancellationException) {
            "Real-world track satellite data cached. High-speed elevation changes and demanding braking zones ahead."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Track details failure", e)
            "$trackName located in $location, $country: Iconic racing ground featuring challenging elevation shifts."
        }
    }

    /**
     * Dynamic pit wall radio commentary during live races.
     */
    suspend fun getPitRadioCallout(
        speedKmh: Float,
        position: Int,
        lap: Int,
        damagePercent: Float,
        isDrifting: Boolean
    ): String {
        return try {
            withTimeout(15_000L) {
                val prompt = """
                    You are a race pit wall engineer speaking over the radio to the driver right now.
                    Status:
                    - Speed: ${speedKmh.toInt()} km/h
                    - Race Position: P$position
                    - Lap: $lap
                    - Chassis Damage: ${damagePercent.toInt()}%
                    - State: ${if (isDrifting) "Power sliding sideways" else "High-speed grip"}
                    
                    Give ONE punchy, authentic 1-sentence radio callout (e.g. "Box this lap for repairs", "P1 is 2 seconds ahead, use nitrous on the straight!", "Incredible drift angle, keep the momentum!").
                """.trimIndent()

                val response = flashModel.generateContent(prompt)
                response.text?.trim() ?: "Keep your foot down, you're flying!"
            }
        } catch (e: Exception) {
            if (position == 1) "You're leading the pack, bring it home!"
            else if (damagePercent > 40f) "Chassis integrity compromised, take caution on curbs!"
            else "Hit the nitro down the main straight to close the gap!"
        }
    }
}
