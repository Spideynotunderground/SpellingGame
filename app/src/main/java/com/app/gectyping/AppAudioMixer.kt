package com.app.gectyping

import kotlin.math.log10
import kotlin.math.pow

/**
 * Lightweight "Audio Mixer" abstraction.
 *
 * Slider is [0..1]. We convert to dB via:
 * db = log10(value) * 20
 */
object AppAudioMixer {

    private const val MIN_SLIDER = 0.0001f  // avoids log10(0)

    fun sliderToDb(value: Float): Float {
        val v = value.coerceIn(MIN_SLIDER, 1f)
        return (log10(v.toDouble()) * 20.0).toFloat()
    }

    fun dbToGain(db: Float): Float {
        val gain = 10.0.pow((db / 20.0).toDouble()).toFloat()
        return gain.coerceIn(0f, 1f)
    }

    fun sliderToGain(value: Float): Float = dbToGain(sliderToDb(value))
}
