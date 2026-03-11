package com.app.gectyping

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.PI
import kotlin.math.sin

/**
 * ============================================================================
 * SOUND MANAGER — Synthesized SFX for all game events
 *
 * All sounds are generated programmatically (no raw files needed).
 * Each sound is short, clean, and non-intrusive.
 *
 * Events:
 *  - success():    Clean high "ding" — correct answer
 *  - fail():       Short dull "boom" — wrong answer
 *  - shopBuy():    Cash register coin jingle — avatar/wallpaper purchase
 *  - lifeBuy():    Magical healing chime — heart purchase
 *  - claim():      Triumphant fanfare — achievement claimed
 * ============================================================================
 */
object SoundManager {

    @Volatile private var globalGain: Float = 1f

    fun setGlobalGain(gain: Float) {
        globalGain = gain.coerceIn(0f, 1f)
    }

    private const val SAMPLE_RATE = 44100

    // ─── Success: crisp ascending "ding-ding-ding" ───────────────────
    fun success() = playAsync {
        val dur = 0.28
        val n = (dur * SAMPLE_RATE).toInt()
        val samples = ShortArray(n)
        val c6 = 1047.0; val e6 = 1319.0; val g6 = 1568.0

        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            if (t < 0.09) {
                val a = if (t < 0.003) t / 0.003 else 1.0
                val d = Math.exp(-t / 0.04)
                s += sin(2 * PI * c6 * t) * 0.50 * a * d
                s += sin(2 * PI * c6 * 2.0 * t) * 0.12 * a * d
            }
            if (t in 0.07..0.18) {
                val t2 = t - 0.07
                val a = if (t2 < 0.003) t2 / 0.003 else 1.0
                val d = Math.exp(-t2 / 0.05)
                s += sin(2 * PI * e6 * t2) * 0.55 * a * d
            }
            if (t >= 0.14) {
                val t3 = t - 0.14
                val a = if (t3 < 0.004) t3 / 0.004 else 1.0
                val d = Math.exp(-t3 / 0.08)
                s += sin(2 * PI * g6 * t3) * 0.60 * a * d
                s += sin(2 * PI * g6 * 3.0 * t3) * 0.05 * a * d
            }
            val env = when {
                t < 0.002 -> t / 0.002
                t > dur - 0.03 -> (dur - t) / 0.03
                else -> 1.0
            }
            samples[i] = (s * env * 0.7 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        emit(samples, 0.70f, 350L)
    }

    // ─── Fail: short dull "bumm" (not annoying) ─────────────────────
    fun fail() = playAsync {
        val dur = 0.22
        val n = (dur * SAMPLE_RATE).toInt()
        val samples = ShortArray(n)

        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 180.0 - 100.0 * (t / dur)           // descending sweep
            var s = sin(2 * PI * freq * t) * 0.35
            s += sin(2 * PI * freq * 3.0 * t) * 0.08       // harmonic
            val env = when {
                t < 0.01 -> t / 0.01
                t > dur - 0.08 -> (dur - t) / 0.08
                else -> 1.0
            }
            samples[i] = (s * env * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        emit(samples, 0.55f, 280L)
    }

    // ─── Shop purchase: soft magical chime ("дзынь") ────────────────
    fun shopBuy() = playAsync {
        val dur = 0.50
        val n = (dur * SAMPLE_RATE).toInt()
        val samples = ShortArray(n)
        // Soft bell-like ascending tones: E5 → G#5 → B5 (sparkle)
        val f1 = 659.25; val f2 = 830.61; val f3 = 987.77

        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            // First soft bell
            if (t < 0.20) {
                val a = if (t < 0.005) t / 0.005 else 1.0
                val d = Math.exp(-t / 0.12)
                s += sin(2 * PI * f1 * t) * 0.25 * a * d
                s += sin(2 * PI * f1 * 2.0 * t) * 0.05 * a * d
            }
            // Second sparkle (offset 100ms)
            if (t in 0.10..0.35) {
                val t2 = t - 0.10
                val a = if (t2 < 0.005) t2 / 0.005 else 1.0
                val d = Math.exp(-t2 / 0.12)
                s += sin(2 * PI * f2 * t2) * 0.22 * a * d
                s += sin(2 * PI * f2 * 3.0 * t2) * 0.03 * a * d
            }
            // Third shimmer (offset 200ms)
            if (t >= 0.20) {
                val t3 = t - 0.20
                val a = if (t3 < 0.005) t3 / 0.005 else 1.0
                val d = Math.exp(-t3 / 0.15)
                s += sin(2 * PI * f3 * t3) * 0.18 * a * d
                s += sin(2 * PI * f3 * 2.0 * t3) * 0.04 * a * d
            }
            val env = when {
                t < 0.003 -> t / 0.003
                t > dur - 0.06 -> (dur - t) / 0.06
                else -> 1.0
            }
            samples[i] = (s * env * 0.7 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        emit(samples, 0.60f, 550L)
    }

    // ─── Life purchase: magical healing chime ───────────────────────
    fun lifeBuy() = playAsync {
        val dur = 0.50
        val n = (dur * SAMPLE_RATE).toInt()
        val samples = ShortArray(n)
        // Ascending sparkle: C5 → E5 → G5 → C6 (arpeggiated, airy)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val starts = doubleArrayOf(0.0, 0.10, 0.20, 0.30)

        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            for (k in notes.indices) {
                if (t >= starts[k] && t < starts[k] + 0.18) {
                    val tk = t - starts[k]
                    val a = if (tk < 0.005) tk / 0.005 else 1.0
                    val d = Math.exp(-tk / 0.07)
                    s += sin(2 * PI * notes[k] * tk) * 0.30 * a * d
                    // Add shimmer harmonic
                    s += sin(2 * PI * notes[k] * 2.0 * tk) * 0.06 * a * d
                }
            }
            val env = when {
                t < 0.002 -> t / 0.002
                t > dur - 0.05 -> (dur - t) / 0.05
                else -> 1.0
            }
            samples[i] = (s * env * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        emit(samples, 0.60f, 550L)
    }

    // ─── Click: short UI click sound for shop items ─────────────────
    fun click() = playAsync {
        val dur = 0.06
        val n = (dur * SAMPLE_RATE).toInt()
        val samples = ShortArray(n)
        val freq = 1200.0

        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val s = sin(2 * PI * freq * t) * 0.3
            val env = when {
                t < 0.005 -> t / 0.005
                t > dur - 0.02 -> (dur - t) / 0.02
                else -> 1.0
            }
            samples[i] = (s * env * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        emit(samples, 0.40f, 100L)
    }

    // ─── Gift open: triumphant fanfare for rewards ─────────────────────
    fun giftOpen() = playAsync {
        val dur = 0.85
        val n = (dur * SAMPLE_RATE).toInt()
        val samples = ShortArray(n)
        // Triumphant fanfare: G4 → C5 → E5 → G5 → C6 (major chord arpeggio)
        val notes = doubleArrayOf(392.0, 523.25, 659.25, 783.99, 1046.50)
        val starts = doubleArrayOf(0.0, 0.12, 0.24, 0.36, 0.50)
        val durations = doubleArrayOf(0.20, 0.18, 0.18, 0.22, 0.30)

        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            for (k in notes.indices) {
                if (t >= starts[k] && t < starts[k] + durations[k]) {
                    val tk = t - starts[k]
                    val a = if (tk < 0.008) tk / 0.008 else 1.0
                    val d = Math.exp(-tk / (durations[k] * 0.6))
                    s += sin(2 * PI * notes[k] * tk) * 0.28 * a * d
                    // Brass-like harmonics
                    s += sin(2 * PI * notes[k] * 2.0 * tk) * 0.12 * a * d
                    s += sin(2 * PI * notes[k] * 3.0 * tk) * 0.05 * a * d
                }
            }
            // Add final shimmer on last note
            if (t >= 0.50) {
                val tk = t - 0.50
                val shimmer = sin(2 * PI * 12.0 * tk) * 0.15 + 1.0
                s *= shimmer
            }
            val env = when {
                t < 0.005 -> t / 0.005
                t > dur - 0.10 -> (dur - t) / 0.10
                else -> 1.0
            }
            samples[i] = (s * env * 0.75 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        emit(samples, 0.75f, 900L)
    }

    // ─── Claim achievement: inspiring crystal reward sound ───────────
    fun claim() = playAsync {
        val dur = 0.70
        val n = (dur * SAMPLE_RATE).toInt()
        val samples = ShortArray(n)
        // Crystal ascending: C5 → E5 → G5 → C6 → E6 (shimmering)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
        val starts = doubleArrayOf(0.0, 0.10, 0.20, 0.30, 0.42)

        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            for (k in notes.indices) {
                if (t >= starts[k]) {
                    val tk = t - starts[k]
                    val a = if (tk < 0.004) tk / 0.004 else 1.0
                    val sustain = if (k == notes.lastIndex) 0.18 else 0.10
                    val d = Math.exp(-tk / sustain)
                    s += sin(2 * PI * notes[k] * tk) * 0.22 * a * d
                    // Crystal shimmer overtone
                    s += sin(2 * PI * notes[k] * 2.0 * tk) * 0.08 * a * d
                    s += sin(2 * PI * notes[k] * 3.0 * tk) * 0.03 * a * d
                }
            }
            val env = when {
                t < 0.003 -> t / 0.003
                t > dur - 0.08 -> (dur - t) / 0.08
                else -> 1.0
            }
            samples[i] = (s * env * 0.80 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        emit(samples, 0.70f, 750L)
    }

    // ─── Internal helpers ───────────────────────────────────────────

    private fun createTrack(numSamples: Int): AudioTrack {
        val channelMask = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, channelMask, encoding).coerceAtLeast(0)
        val bufferBytes = (numSamples * 2).coerceAtLeast(minBuf)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(channelMask)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferBytes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                channelMask,
                encoding,
                bufferBytes,
                AudioTrack.MODE_STATIC
            )
        }
    }

    private fun emit(samples: ShortArray, volume: Float, releaseAfterMs: Long) {
        try {
            val track = createTrack(samples.size)
            track.write(samples, 0, samples.size)

            try {
                val g = (volume * globalGain).coerceIn(0f, 1f)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    track.setVolume(g)
                } else {
                    @Suppress("DEPRECATION")
                    track.setStereoVolume(g, g)
                }
            } catch (_: Throwable) {}

            track.play()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { track.stop() } catch (_: Throwable) {}
                try { track.release() } catch (_: Throwable) {}
            }, releaseAfterMs)
        } catch (_: Throwable) {
            // Silent fallback
        }
    }


    private fun playAsync(block: () -> Unit) {
        Thread { block() }.start()
    }
}
