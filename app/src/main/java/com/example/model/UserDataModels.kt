package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val careerLevel: Int = 1,
    val cash: Int = 50000,
    val reputation: Int = 100,
    val selectedCarId: String = "bmw_m4_default",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "careerLevel" to careerLevel,
        "cash" to cash,
        "reputation" to reputation,
        "selectedCarId" to selectedCarId,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}

@IgnoreExtraProperties
data class GarageCar(
    @DocumentId val id: String = "",
    val userId: String = "",
    val carModelKey: String = "bmw_m4",
    val name: String = "BMW M4 Competition",
    val isCustomBuild: Boolean = false,
    val paintColorHex: String = "#00E5FF",
    val engineLevel: Int = 1,
    val turboLevel: Int = 0,
    val suspensionLevel: Int = 1,
    val transmissionLevel: Int = 1,
    val brakesLevel: Int = 1,
    val nitroLevel: Int = 1,
    val spoilerType: Int = 0,
    val bodyKitType: Int = 0,
    val underglowColorHex: String = "#00E5FF",
    val windowTintLevel: Int = 20,
    val wheelType: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "carModelKey" to carModelKey,
        "name" to name,
        "isCustomBuild" to isCustomBuild,
        "paintColorHex" to paintColorHex,
        "engineLevel" to engineLevel,
        "turboLevel" to turboLevel,
        "suspensionLevel" to suspensionLevel,
        "transmissionLevel" to transmissionLevel,
        "brakesLevel" to brakesLevel,
        "nitroLevel" to nitroLevel,
        "spoilerType" to spoilerType,
        "bodyKitType" to bodyKitType,
        "underglowColorHex" to underglowColorHex,
        "windowTintLevel" to windowTintLevel,
        "wheelType" to wheelType,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}

@IgnoreExtraProperties
data class CustomMap(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Neon Skyline Ring",
    val environment: String = "neon_night",
    val weather: String = "sunny",
    val trackPointsJson: String = "[]",
    val obstaclesCount: Int = 12,
    val laps: Int = 3,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "name" to name,
        "environment" to environment,
        "weather" to weather,
        "trackPointsJson" to trackPointsJson,
        "obstaclesCount" to obstaclesCount,
        "laps" to laps,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}

@IgnoreExtraProperties
data class ScoreRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val trackId: String = "",
    val carModel: String = "",
    val lapTimeMs: Int = 0,
    val driftScore: Int = 0,
    val gameMode: String = "race",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "displayName" to displayName,
        "trackId" to trackId,
        "carModel" to carModel,
        "lapTimeMs" to lapTimeMs,
        "driftScore" to driftScore,
        "gameMode" to gameMode,
        "createdAt" to createdAt
    ).filterValues { it != null }
}
