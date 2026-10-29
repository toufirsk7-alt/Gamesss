package com.example.model

enum class GameMode(val title: String, val subtitle: String) {
    CAREER("Career Mode", "Rise from street rookie to World Champion"),
    RACING("Circuit Racing", "Compete against 7 fierce AI rivals for victory"),
    DRIFT("Drift Challenge", "Score maximum drift points on technical hairpin turns"),
    STORY("Story Mode", "High-stakes rivalries, crew challenges & pink slip showdowns"),
    FREE_ROAM("Open City Free Roam", "Explore a bustling city with live AI traffic & pedestrian NPCs"),
    MAP_BUILDER("Map Creator", "Design, customize, test drive & share your custom tracks")
}

enum class WeatherCondition(val label: String, val roadGripFactor: Float, val visibilityFactor: Float) {
    SUNNY("Clear Sky", 1.0f, 1.0f),
    GOLDEN_HOUR("Sunset Dusk", 0.98f, 0.92f),
    RAIN("Heavy Rain", 0.75f, 0.70f),
    THUNDERSTORM("Thunderstorm", 0.68f, 0.60f),
    SNOW("Snow & Ice", 0.55f, 0.65f),
    MIDNIGHT_NEON("Midnight Neon", 0.95f, 0.85f)
}

enum class CameraView(val label: String) {
    THIRD_PERSON("3rd Person Chase"),
    FIRST_PERSON_COCKPIT("1st Person Cockpit"),
    HOOD_CAM("Hood / Bumper")
}

enum class TransmissionMode {
    AUTOMATIC,
    MANUAL
}

enum class ControlScheme(val label: String) {
    TOUCH_BUTTONS("Touch Arrows"),
    STEERING_WHEEL("Virtual Wheel"),
    JOYSTICK_SLIDER("Analog Slider"),
    TILT_SENSOR("Gyro Tilt")
}

data class TrafficVehicle(
    val id: Int,
    var x: Float, // Lateral offset from road center (-1.0 to 1.0)
    var z: Float, // Distance along track in meters
    var speedKmh: Float,
    val colorHex: String,
    val modelType: String // "sedan", "suv", "sports", "truck", "cab"
)

data class PedestrianNpc(
    val id: Int,
    var x: Float, // Position on sidewalk or crosswalk (-1.8 to -1.1 or 1.1 to 1.8)
    var z: Float, // Distance along track
    var walkingSpeed: Float,
    val name: String,
    val quote: String,
    var isInteracting: Boolean = false
)

data class AiRivalCar(
    val id: Int,
    val name: String,
    val carModelKey: String,
    var x: Float,
    var z: Float,
    var speedKmh: Float,
    var position: Int = 1,
    var currentLap: Int = 1,
    var lapProgress: Float = 0f,
    val colorHex: String,
    val aggression: Float // 0.8 to 1.2
)

data class TelemetrySnapshot(
    val speedKmh: Float,
    val rpm: Float,
    val gear: String,
    val throttle: Float,
    val brake: Float,
    val steeringAngle: Float,
    val nitroPercent: Float,
    val driftAngle: Float,
    val driftScore: Int,
    val damagePercent: Float,
    val lapTimeMs: Long,
    val currentLap: Int,
    val position: Int
)
