package com.app.gectyping

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import kotlin.math.PI
import kotlin.math.sin

object BackgroundMusicPlayer {
    @Volatile
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var fallbackTrack: AudioTrack? = null

    @Volatile
    private var currentVolume: Float = 0.28f

    @Volatile
    var currentTrackId: String = "default"
        private set

    private var appContext: Context? = null

    /**
     * Fully release current player resources.
     */
    fun release() {
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        try { fallbackTrack?.stop() } catch (_: Exception) {}
        try { fallbackTrack?.release() } catch (_: Exception) {}
        fallbackTrack = null
    }

    /**
     * Force reload the audio source — fixes stuck/silent MediaPlayer.
     */
    fun reload(context: Context) {
        release()
        appContext = context.applicationContext
        ensureStarted(context)
    }

    /**
     * Play a synthesized track (ShortArray samples) as background music.
     * Used by Music Shop for purchased synthesized tracks.
     */
    fun playSynthesized(trackId: String, samples: ShortArray) {
        release()
        currentTrackId = trackId

        val sampleRate = 44100
        val channelMask = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding).coerceAtLeast(0)
        val bufBytes = (samples.size * 2).coerceAtLeast(minBuf)

        val at = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufBytes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate, channelMask, encoding,
                bufBytes, AudioTrack.MODE_STATIC
            )
        }
        at.write(samples, 0, samples.size)
        at.setLoopPoints(0, samples.size, -1)
        try { at.setVolume(currentVolume) } catch (_: Exception) {}
        fallbackTrack = at
        start()
    }

    /**
     * Switch to a different track by raw resource ID.
     * Used by Music Shop to change background music.
     */
    fun switchTrack(context: Context, trackId: String, rawResId: Int) {
        release()
        currentTrackId = trackId
        appContext = context.applicationContext
        try {
            val mp = MediaPlayer.create(context.applicationContext, rawResId)
            if (mp != null) {
                mp.isLooping = true
                mp.setVolume(currentVolume, currentVolume)
                mediaPlayer = mp
                start()
                return
            }
        } catch (_: Throwable) {}
        // If resource loading fails, fall back to default
        currentTrackId = "default"
        ensureStarted(context)
    }

    /**
     * 3) Background music
     * - Primary: plays app/src/main/res/raw/bg_music.* (wav/mp3/ogg) in a loop
     * - Fallback: a tiny synthesized loop so the app still runs even if you remove the file
     */
    fun ensureStarted(context: Context) {
        appContext = context.applicationContext
        // If already running and healthy, skip
        if (mediaPlayer != null || fallbackTrack != null) return

        // Try a real music file first: res/raw/bg_music.(wav|mp3|ogg)
        try {
            val mp = MediaPlayer.create(context.applicationContext, R.raw.bg_music)
            if (mp != null) {
                mp.isLooping = true
                mp.setVolume(currentVolume, currentVolume)
                mp.setOnErrorListener { _, _, _ ->
                    // Auto-reload on error
                    reload(context)
                    true
                }
                mediaPlayer = mp
                start()
                return
            }
        } catch (_: Throwable) {
            // ignore and fallback below
        }

        // Fallback: synthesized loop (keeps the game playable even without a file)
        val sampleRate = 44100
        val durationSeconds = 6
        val numSamples = sampleRate * durationSeconds
        val samples = ShortArray(numSamples)

        val f1 = 110.0
        val f2 = 220.0
        val f3 = 164.81
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val s =
                (sin(2 * PI * f1 * t) * 0.16) +
                        (sin(2 * PI * f2 * t) * 0.08) +
                        (sin(2 * PI * f3 * t) * 0.06)
            val envelope = when {
                t < 0.8 -> (t / 0.8)
                t > durationSeconds - 0.8 -> ((durationSeconds - t) / 0.8)
                else -> 1.0
            }
            samples[i] = (s * envelope * Short.MAX_VALUE * 0.22).toInt().coerceIn(
                Short.MIN_VALUE.toInt(),
                Short.MAX_VALUE.toInt()
            ).toShort()
        }

        val channelMask = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
            .coerceAtLeast(0)
        val bufferBytes = (samples.size * 2).coerceAtLeast(minBuf)

        val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
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
                sampleRate,
                channelMask,
                encoding,
                bufferBytes,
                AudioTrack.MODE_STATIC
            )
        }
        audioTrack.write(samples, 0, samples.size)
        audioTrack.setLoopPoints(0, samples.size, -1)
        try {
            audioTrack.setVolume(currentVolume)
        } catch (_: Exception) {
        }
        fallbackTrack = audioTrack
        start()
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        currentVolume = v
        try {
            mediaPlayer?.setVolume(v, v)
        } catch (_: Exception) {
        }
        try {
            fallbackTrack?.setVolume(v)
        } catch (_: Exception) {
        }
    }

    fun start() {
        mediaPlayer?.let {
            try {
                if (!it.isPlaying) it.start()
            } catch (_: Exception) {
            }
        }
        fallbackTrack?.let {
            if (it.playState != AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    it.play()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stop() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.pause()
            } catch (_: Exception) {
            }
        }
        fallbackTrack?.let {
            try {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.pause()
                }
                // IMPORTANT: do NOT flush() a MODE_STATIC AudioTrack.
                // On some devices this discards the static buffer and the track becomes silent after resume.
            } catch (_: Exception) {
            }
        }
    }
}
