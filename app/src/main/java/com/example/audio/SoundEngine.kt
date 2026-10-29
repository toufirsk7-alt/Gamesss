package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance real-time Audio Synthesis Engine.
 * Generates procedural audio for engine RPM harmonics, turbo blowoff, tire screech,
 * crash impacts, horn, and nitro boosts via low-latency AudioTrack.
 */
class SoundEngine {
    private val sampleRate = 22050
    private var engineTrack: AudioTrack? = null
    private var sfxTrack: AudioTrack? = null

    private var audioJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile var targetRpm: Float = 900f
    @Volatile var soundProfile: String = "v8_rumble"
    @Volatile var isAccelerating: Boolean = false
    @Volatile var isBraking: Boolean = false
    @Volatile var isDrifting: Boolean = false
    @Volatile var isNitroActive: Boolean = false
    @Volatile var isHornActive: Boolean = false
    @Volatile var triggerCrashCount: Int = 0
    @Volatile var triggerShiftCount: Int = 0

    private var isPlaying = false

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        try {
            engineTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            engineTrack?.play()

            audioJob = scope.launch {
                val chunkSize = 512
                val buffer = ShortArray(chunkSize)
                var phase1 = 0.0
                var phase2 = 0.0
                var phase3 = 0.0
                var blowoffSamples = 0
                var crashSamples = 0
                var lastShiftTrigger = 0
                var lastCrashTrigger = 0

                while (isActive && isPlaying) {
                    if (triggerShiftCount != lastShiftTrigger) {
                        lastShiftTrigger = triggerShiftCount
                        blowoffSamples = sampleRate / 3 // 330ms blow-off hiss
                    }
                    if (triggerCrashCount != lastCrashTrigger) {
                        lastCrashTrigger = triggerCrashCount
                        crashSamples = sampleRate / 2 // 500ms crash crunch
                    }

                    val rpm = targetRpm.coerceIn(800f, 9000f)

                    // Base frequency from RPM
                    val baseFreq = when (soundProfile) {
                        "v10_scream" -> (rpm / 60.0) * 2.5
                        "v8_rumble" -> (rpm / 60.0) * 2.0
                        "ev_whine" -> (rpm / 60.0) * 4.0
                        "diesel_grunt" -> (rpm / 60.0) * 1.5
                        else -> (rpm / 60.0) * 1.8 // i6_turbo
                    }

                    for (i in 0 until chunkSize) {
                        val dt1 = 2.0 * PI * baseFreq / sampleRate
                        val dt2 = 2.0 * PI * (baseFreq * 2.0) / sampleRate
                        val dt3 = 2.0 * PI * (baseFreq * 3.0) / sampleRate

                        phase1 += dt1
                        phase2 += dt2
                        phase3 += dt3

                        if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                        if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
                        if (phase3 > 2.0 * PI) phase3 -= 2.0 * PI

                        var sample = 0.0

                        if (soundProfile == "ev_whine") {
                            // High-pitch pure electric motor whine
                            sample = sin(phase1) * 0.4 + sin(phase2) * 0.2
                        } else {
                            // Rich internal combustion harmonics
                            sample = sin(phase1) * 0.5 +
                                    sin(phase2) * 0.25 +
                                    sin(phase3) * 0.15

                            // Add exhaust distortion when accelerating hard
                            if (isAccelerating) {
                                sample += sin(phase1 * 0.5) * 0.2
                                if (sample > 0.8) sample = 0.8
                                if (sample < -0.8) sample = -0.8
                            }
                        }

                        // Nitro boost high-octane hiss & resonance
                        if (isNitroActive) {
                            val nitroNoise = (Random.nextFloat() * 2f - 1f) * 0.25
                            sample = sample * 0.8 + nitroNoise + sin(phase3 * 1.5) * 0.2
                        }

                        // Tire screech when drifting
                        if (isDrifting) {
                            val tireNoise = (Random.nextFloat() * 2f - 1f) * 0.35
                            sample += tireNoise * 0.4
                        }

                        // Turbo blow-off valve hiss on gear change
                        if (blowoffSamples > 0) {
                            blowoffSamples--
                            val hiss = (Random.nextFloat() * 2f - 1f) * (blowoffSamples.toFloat() / (sampleRate / 3)) * 0.5
                            sample += hiss
                        }

                        // Crash crunch & metal impact
                        if (crashSamples > 0) {
                            crashSamples--
                            val crashFactor = crashSamples.toFloat() / (sampleRate / 2)
                            val crunch = (Random.nextFloat() * 2f - 1f) * 0.8 * crashFactor
                            sample = crunch + sample * 0.2
                        }

                        // Car Horn dual-tone (440Hz + 554Hz)
                        if (isHornActive) {
                            val hornSample = (sin(2.0 * PI * 440.0 * i / sampleRate) + sin(2.0 * PI * 554.0 * i / sampleRate)) * 0.4
                            sample = sample * 0.4 + hornSample
                        }

                        val pcm = (sample.coerceIn(-1.0, 1.0) * 30000.0).toInt().toShort()
                        buffer[i] = pcm
                    }

                    engineTrack?.write(buffer, 0, chunkSize)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundEngine", "Failed to start AudioTrack synthesis: ${e.message}")
        }
    }

    fun triggerShift() {
        triggerShiftCount++
    }

    fun triggerCrash() {
        triggerCrashCount++
    }

    fun stop() {
        isPlaying = false
        audioJob?.cancel()
        audioJob = null
        try {
            engineTrack?.stop()
            engineTrack?.release()
            engineTrack = null
        } catch (_: Exception) {}
    }
}
