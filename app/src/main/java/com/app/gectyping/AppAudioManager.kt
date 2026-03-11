package com.app.gectyping

import android.content.Context
import kotlinx.coroutines.*

/**
 * Central audio state machine:
 *  - Background music vs Store preview never conflict again
 *  - Fade BG -> 0 before preview
 *  - Fade BG back after preview
 *  - Applies Mixer volumes (Master / Music / SFX)
 */
object AppAudioManager {

    private var appContext: Context? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var fadeJob: Job? = null

    private var masterSlider = 1f
    private var musicSlider = 0.28f
    private var sfxSlider = 1f
    private var voiceSlider = 1f  // TTS voice volume

    private var soundEnabled = true
    private var musicEnabled = true

    private var selectedTrackId: String = "default"
    private var isPreviewing = false

    fun attach(context: Context) {
        appContext = context.applicationContext
        BackgroundMusicPlayer.ensureStarted(context.applicationContext)
        applyRouting()
    }

    fun setMixerVolumes(master: Float, music: Float, sfx: Float, voice: Float = voiceSlider) {
        masterSlider = master
        musicSlider = music
        sfxSlider = sfx
        voiceSlider = voice
        applyRouting()
    }
    
    fun setVoiceVolume(voice: Float) {
        voiceSlider = voice
    }

    fun setToggles(soundEnabled: Boolean, musicEnabled: Boolean) {
        this.soundEnabled = soundEnabled
        this.musicEnabled = musicEnabled

        SoundManager.setGlobalGain(if (soundEnabled) sfxGroupGain() else 0f)

        if (!musicEnabled) {
            stopPreview(immediate = true)
            BackgroundMusicPlayer.stop()
            BackgroundMusicPlayer.setVolume(0f)
        } else {
            ensureSelectedTrackLoaded()
            BackgroundMusicPlayer.start()
            if (!isPreviewing) BackgroundMusicPlayer.setVolume(musicGroupGain())
        }
    }

    fun applySelectedTrack(trackId: String) {
        selectedTrackId = trackId
        ensureSelectedTrackLoaded()
        if (musicEnabled) {
            BackgroundMusicPlayer.start()
            if (!isPreviewing) BackgroundMusicPlayer.setVolume(musicGroupGain())
        }
    }

    fun playPreview(trackId: String) {
        if (!musicEnabled) return
        ensureSelectedTrackLoaded()

        val bgFrom = musicGroupGain()
        isPreviewing = true

        fadeBackground(from = bgFrom, to = 0f, durationMs = 220) {
            MusicShopManager.preview(trackId, musicGroupGain())
        }
    }

    fun stopPreview() = stopPreview(immediate = false)

    fun ttsVolumeGain(): Float = voiceGroupGain()

    private fun stopPreview(immediate: Boolean) {
        if (!isPreviewing && !immediate) return
        isPreviewing = false

        MusicShopManager.stopPreview()

        if (!musicEnabled) return
        val target = musicGroupGain()

        if (immediate) {
            BackgroundMusicPlayer.setVolume(target)
        } else {
            fadeBackground(from = 0f, to = target, durationMs = 220)
        }
    }

    private fun applyRouting() {
        if (musicEnabled) {
            BackgroundMusicPlayer.start()
            if (!isPreviewing) BackgroundMusicPlayer.setVolume(musicGroupGain())
        } else {
            BackgroundMusicPlayer.setVolume(0f)
        }

        if (isPreviewing) {
            MusicShopManager.setPreviewVolume(musicGroupGain())
        }

        SoundManager.setGlobalGain(if (soundEnabled) sfxGroupGain() else 0f)
    }

    private fun ensureSelectedTrackLoaded() {
        val ctx = appContext ?: return
        if (selectedTrackId == BackgroundMusicPlayer.currentTrackId) return

        if (selectedTrackId == "default") {
            BackgroundMusicPlayer.reload(ctx)
        } else {
            val samples = MusicShopManager.synthesize(selectedTrackId, 6)
            BackgroundMusicPlayer.playSynthesized(selectedTrackId, samples)
        }
    }

    private fun masterGain() = AppAudioMixer.sliderToGain(masterSlider)
    private fun musicGain() = AppAudioMixer.sliderToGain(musicSlider)
    private fun sfxGain() = AppAudioMixer.sliderToGain(sfxSlider)
    private fun voiceGain() = AppAudioMixer.sliderToGain(voiceSlider)

    private fun musicGroupGain() = (masterGain() * musicGain()).coerceIn(0f, 1f)
    private fun sfxGroupGain() = (masterGain() * sfxGain()).coerceIn(0f, 1f)
    private fun voiceGroupGain() = (masterGain() * voiceGain()).coerceIn(0f, 1f)

    private fun fadeBackground(from: Float, to: Float, durationMs: Long, onEnd: (() -> Unit)? = null) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 12
            val stepDelay = (durationMs / steps).coerceAtLeast(1)
            for (i in 1..steps) {
                val t = i.toFloat() / steps.toFloat()
                val v = (from + (to - from) * t).coerceIn(0f, 1f)
                BackgroundMusicPlayer.setVolume(v)
                delay(stepDelay.toLong())
            }
            BackgroundMusicPlayer.setVolume(to)
            onEnd?.invoke()
        }
    }
}
