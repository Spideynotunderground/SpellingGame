package com.app.gectyping

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

/**
 * ============================================================================
 * MUSIC SHOP MANAGER — 7 Ambient / Lo-Fi / Chillstep tracks
 *
 * All tracks are procedurally synthesized, loopable, and designed to be
 * NON-DISTRACTING background music. Volume is capped at 35% (below voice).
 * ============================================================================
 */

data class MusicTrackItem(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val priceDiamonds: Int,
    val iconRes: Int? = null
)

val MUSIC_TRACKS = listOf(
    MusicTrackItem("ambient_drift",  "Ambient Drift",   "🎵", "Soft evolving pads",           60,  R.drawable.icons8_undertale_heart_96),
    MusicTrackItem("lofi_study",     "Lo-Fi Study",     "📚", "Warm mellow lo-fi beats",      80,  R.drawable.icons8_books_96),
    MusicTrackItem("ocean_calm",     "Ocean Calm",      "🌊", "Slow waves & deep tones",      80,  R.drawable.icons8_sail_boat_96),
    MusicTrackItem("forest_rain",    "Forest Rain",     "🌳", "Gentle tremolo & nature feel", 100, R.drawable.icons8_oak_tree_96),
    MusicTrackItem("chill_step",     "Chill Step",      "🎧", "Deep bass with airy highs",    120, R.drawable.icons8_pixel_cat_96),
    MusicTrackItem("crystal_cave",   "Crystal Cave",    "💎", "Ethereal bell-like harmonics", 140, R.drawable.icons8_crystal_96),
    MusicTrackItem("night_drive",    "Night Drive",     "🌙", "Retro synth cruise vibes",     160, R.drawable.icons8_moon_and_stars_96),
)

/** Max volume multiplier — music stays 30-40% below voice */
const val MUSIC_VOL_CAP = 0.35f

object MusicShopManager {

    private const val SR = 44100

    @Volatile
    private var previewTrack: AudioTrack? = null

    fun synthesize(trackId: String, durationSeconds: Int): ShortArray {
        val n = SR * durationSeconds
        val out = ShortArray(n)
        when (trackId) {
            "ambient_drift" -> synthAmbientDrift(out, n)
            "lofi_study"    -> synthLofiStudy(out, n)
            "ocean_calm"    -> synthOceanCalm(out, n)
            "forest_rain"   -> synthForestRain(out, n)
            "chill_step"    -> synthChillStep(out, n)
            "crystal_cave"  -> synthCrystalCave(out, n)
            "night_drive"   -> synthNightDrive(out, n)
            else            -> synthAmbientDrift(out, n)
        }
        return out
    }

    fun preview(trackId: String, gain: Float) {
        stopPreview()
        val samples = synthesize(trackId, 5)
        val at = buildTrack(samples)
        setTrackVolume(at, gain)
        previewTrack = at
        try { at.play() } catch (_: Exception) {}
    }

    /** Live updates while preview is playing (slider changes). */
    fun setPreviewVolume(gain: Float) {
        val t = previewTrack ?: return
        setTrackVolume(t, gain)
    }

    fun stopPreview() {
        try { previewTrack?.stop() } catch (_: Exception) {}
        try { previewTrack?.release() } catch (_: Exception) {}
        previewTrack = null
    }

    private fun setTrackVolume(track: AudioTrack, gain: Float) {
        val v = (gain * MUSIC_VOL_CAP).coerceIn(0f, MUSIC_VOL_CAP)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                track.setVolume(v)
            } else {
                @Suppress("DEPRECATION")
                track.setStereoVolume(v, v)
            }
        } catch (_: Exception) {}
    }


    // ================================================================
    // TRACK SYNTHESIS — all ambient / non-distracting
    // ================================================================

    /** � Ambient Drift — evolving pad with slow LFO */
    private fun synthAmbientDrift(out: ShortArray, n: Int) {
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val lfo = 1.0 + 0.25 * sin(2 * PI * 0.08 * t)
            val s = sin(2 * PI * 130.81 * t) * 0.12 * lfo +
                    sin(2 * PI * 196.0 * t) * 0.08 * lfo +
                    sin(2 * PI * 261.63 * t) * 0.05 * (1.0 + 0.3 * sin(2 * PI * 0.12 * t)) +
                    sin(2 * PI * 65.41 * t) * 0.07
            out[i] = samp(s * env(t, n))
        }
    }

    /** 📚 Lo-Fi Study — warm mellow beats with filtered feel */
    private fun synthLofiStudy(out: ShortArray, n: Int) {
        val beatLen = SR / 2  // 120 BPM (0.5s per beat)
        val chords = doubleArrayOf(130.81, 146.83, 164.81, 146.83) // C3 D3 E3 D3
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val beatIdx = (i / beatLen) % chords.size
            val freq = chords[beatIdx]
            val localT = (i % beatLen).toDouble() / SR
            val decay = Math.exp(-localT / 0.25)
            val s = sin(2 * PI * freq * t) * 0.12 * decay +
                    sin(2 * PI * freq * 1.5 * t) * 0.04 * decay +
                    sin(2 * PI * 82.41 * t) * 0.06 * (0.8 + 0.2 * sin(2 * PI * 2.0 * t))
            out[i] = samp(s * env(t, n))
        }
    }

    /** 🌊 Ocean Calm — slow waves & deep sine tones */
    private fun synthOceanCalm(out: ShortArray, n: Int) {
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val wave = 1.0 + 0.35 * sin(2 * PI * 0.12 * t)
            val s = sin(2 * PI * 82.41 * t) * 0.14 * wave +
                    sin(2 * PI * 123.47 * t) * 0.08 * wave +
                    sin(2 * PI * 55.0 * t) * 0.10 +
                    sin(2 * PI * 196.0 * t) * 0.03 * (0.5 + 0.5 * sin(2 * PI * 0.06 * t))
            out[i] = samp(s * env(t, n))
        }
    }

    /** �️ Forest Rain — gentle tremolo & nature-like harmonics */
    private fun synthForestRain(out: ShortArray, n: Int) {
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val trem = 0.6 + 0.4 * sin(2 * PI * 3.5 * t)
            val s = sin(2 * PI * 146.83 * t) * 0.10 * trem +
                    sin(2 * PI * 220.0 * t) * 0.06 * trem +
                    sin(2 * PI * 110.0 * t) * 0.08 +
                    sin(2 * PI * 329.63 * t) * 0.03 * (0.5 + 0.5 * cos(2 * PI * 0.2 * t))
            out[i] = samp(s * env(t, n))
        }
    }

    /** 🎧 Chill Step — deep sub-bass with airy high pads */
    private fun synthChillStep(out: ShortArray, n: Int) {
        val stepLen = SR  // 60 BPM
        val bassNotes = doubleArrayOf(55.0, 61.74, 65.41, 55.0)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val stepIdx = (i / stepLen) % bassNotes.size
            val bass = bassNotes[stepIdx]
            val localT = (i % stepLen).toDouble() / SR
            val kick = if (localT < 0.08) sin(2 * PI * (120 - 80 * localT / 0.08) * localT) * 0.15 * Math.exp(-localT / 0.03) else 0.0
            val pad = sin(2 * PI * bass * 4 * t) * 0.04 * (0.6 + 0.4 * sin(2 * PI * 0.1 * t))
            val sub = sin(2 * PI * bass * t) * 0.14 * Math.exp(-localT / 0.6)
            out[i] = samp((kick + sub + pad) * env(t, n))
        }
    }

    /** 💎 Crystal Cave — bell-like ethereal harmonics */
    private fun synthCrystalCave(out: ShortArray, n: Int) {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 880.0, 659.25, 523.25)
        val noteLen = SR * 3 / 4  // slower arpeggios
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val noteIdx = (i / noteLen) % notes.size
            val freq = notes[noteIdx]
            val localT = (i % noteLen).toDouble() / SR
            val bell = sin(2 * PI * freq * localT) * 0.08 * Math.exp(-localT / 0.35)
            val shimmer = sin(2 * PI * freq * 2.0 * localT) * 0.03 * Math.exp(-localT / 0.20)
            val pad = sin(2 * PI * 220.0 * t) * 0.06 * (0.7 + 0.3 * sin(2 * PI * 0.09 * t))
            out[i] = samp((bell + shimmer + pad) * env(t, n))
        }
    }

    /** 🌃 Night Drive — retro synth, steady pulse */
    private fun synthNightDrive(out: ShortArray, n: Int) {
        val pulseLen = SR / 3  // ~180 BPM pulse
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val localT = (i % pulseLen).toDouble() / SR
            val pulse = sin(2 * PI * 110.0 * localT) * 0.10 * Math.exp(-localT / 0.10)
            val synthPad = sin(2 * PI * 164.81 * t) * 0.08 * (0.7 + 0.3 * sin(2 * PI * 0.15 * t)) +
                           sin(2 * PI * 246.94 * t) * 0.05 * (0.6 + 0.4 * sin(2 * PI * 0.1 * t))
            val bass = sin(2 * PI * 55.0 * t) * 0.09
            out[i] = samp((pulse + synthPad + bass) * env(t, n))
        }
    }

    // ================================================================
    // Helpers
    // ================================================================

    private fun env(t: Double, n: Int): Double {
        val dur = n.toDouble() / SR
        val fadeIn = 0.8; val fadeOut = 0.8
        return when {
            t < fadeIn        -> t / fadeIn
            t > dur - fadeOut -> (dur - t) / fadeOut
            else              -> 1.0
        }.coerceIn(0.0, 1.0)
    }

    private fun samp(v: Double): Short =
        (v * Short.MAX_VALUE * 0.80).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

    private fun buildTrack(samples: ShortArray): AudioTrack {
        val ch = AudioFormat.CHANNEL_OUT_MONO
        val enc = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioTrack.getMinBufferSize(SR, ch, enc).coerceAtLeast(0)
        val buf = (samples.size * 2).coerceAtLeast(minBuf)
        val at = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(enc).setSampleRate(SR).setChannelMask(ch).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(buf).build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(AudioManager.STREAM_MUSIC, SR, ch, enc, buf, AudioTrack.MODE_STATIC)
        }
        at.write(samples, 0, samples.size)
        return at
    }
}
