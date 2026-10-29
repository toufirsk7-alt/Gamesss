package com.example

import com.example.model.CarCatalog
import com.example.model.TrackCatalog
import com.example.model.WeatherCondition
import com.example.physics.CarPhysics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun carCatalog_containsMandatedVehicles() {
        val requiredKeys = listOf(
            "bmw_m4", "bmw_m8", "bmw_m3", "bmw_i7", "bmw_i4", "bmw_i3", "bmw_x7",
            "porsche_911", "porsche_macan",
            "scorpio_classic", "mahindra_be6", "mahindra_xev9e", "mahindra_xuv3xo", "mahindra_xuv7xo",
            "tata_harrier", "tata_sierra_ev", "tata_punch",
            "mercedes_amg", "mercedes_maybach",
            "audi_r8", "audi_q8"
        )
        for (key in requiredKeys) {
            val car = CarCatalog.getCar(key)
            assertNotNull("Vehicle $key must exist", car)
            assertTrue("Vehicle $key must have positive horsepower", car.horsepower > 0)
            assertTrue("Vehicle $key must have top speed > 100", car.topSpeedKmh > 100)
        }
    }

    @Test
    fun trackCatalog_contains30Tracks() {
        assertEquals(30, TrackCatalog.ALL_TRACKS.size)
    }

    @Test
    fun carPhysics_accelerationAndBraking() {
        val car = CarCatalog.getCar("bmw_m4")
        val physics = CarPhysics(baseSpec = car)
        assertEquals(0f, physics.speedKmh, 0.01f)

        // Accelerate
        physics.throttleInput = 1.0f
        physics.update(0.1f, WeatherCondition.SUNNY)
        assertTrue("Car must accelerate forward", physics.speedKmh > 0f)

        // Brake
        val speedBeforeBrake = physics.speedKmh
        physics.throttleInput = 0f
        physics.brakeInput = 1.0f
        physics.update(0.1f, WeatherCondition.SUNNY)
        assertTrue("Car must slow down under braking", physics.speedKmh < speedBeforeBrake)
    }

    @Test
    fun carPhysics_collisionDamageCalculation() {
        val car = CarCatalog.getCar("porsche_911")
        val physics = CarPhysics(baseSpec = car)
        assertEquals(0f, physics.damagePercent, 0.01f)

        physics.applyCollisionDamage(0.5f)
        assertTrue("Damage must increase after crash impact", physics.damagePercent > 0f)
    }
}
