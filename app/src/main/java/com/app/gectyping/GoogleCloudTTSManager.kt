package com.app.gectyping

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Google Cloud Text-to-Speech Manager
 *
 * Synthesizes speech via the Google Cloud TTS REST API using SSML for
 * maximum pronunciation clarity — critical for a spelling/vocabulary game.
 *
 * Key features:
 *  • Studio-quality voice: en-US-Neural2-D (male, very clear diction)
 *  • SSML with <say-as interpret-as="characters"> for letter-by-letter spelling
 *  • Dynamic rate/pitch from in-app settings (finally wired through to API)
 *  • Pronunciation pattern: word → pause → word (helps learners hear it twice)
 *  • Results cached to disk — each unique phrase hits the API only once
 *  • Falls back to Android built-in TTS when Cloud API is unreachable
 *
 * ── Quick Setup ──────────────────────────────────────────────────────────────
 *  1. Go to https://console.cloud.google.com
 *  2. Enable "Cloud Text-to-Speech API"
 *  3. Create an API key under APIs & Services → Credentials
 *  4. Paste it into API_KEY below and rebuild
 * ─────────────────────────────────────────────────────────────────────────────
 */
object GoogleCloudTTSManager {

    private const val TAG = "GoogleCloudTTSManager"

    // ─── PASTE YOUR GOOGLE CLOUD API KEY HERE ────────────────────────────────
    private const val API_KEY = "AIzaSyCF5QGVmfCdFMi4-7sFqt9Dt_m5zFskQD0"
    // ─────────────────────────────────────────────────────────────────────────

    private const val TTS_ENDPOINT =
        "https://texttospeech.googleapis.com/v1/text:synthesize"

    // ── Voice selection ───────────────────────────────────────────────────────
    //
    // Chirp 3: HD voices — most natural, human-like pronunciation available.
    // These are the best choice for a language learning game.
    //
    // ⚠️  Chirp 3 HD does NOT support: SSML, speakingRate, pitch, effectsProfileId
    //     The code below detects Chirp voices and sends a plain-text request.
    //
    // Female Chirp 3 HD options (all en-US):
    //   "en-US-Chirp3-HD-Aoede"       — warm, clear, excellent for education ✅ (default)
    //   "en-US-Chirp3-HD-Leda"        — bright, energetic American female
    //   "en-US-Chirp3-HD-Kore"        — softer, gentle tone
    //   "en-US-Chirp3-HD-Schedar"     — authoritative, crisp
    //   "en-US-Chirp3-HD-Sulafat"     — smooth, natural
    //   "en-US-Chirp3-HD-Vindemiatrix"— expressive, lively
    //
    // Male Chirp 3 HD options:
    //   "en-US-Chirp3-HD-Charon"      — deep, clear male
    //   "en-US-Chirp3-HD-Fenrir"      — strong, confident
    //   "en-US-Chirp3-HD-Orus"        — neutral, professional
    //   "en-US-Chirp3-HD-Puck"        — friendly, natural
    // ──────────────────────────────────────────────────────────────────────────
    private const val VOICE_NAME     = "en-US-Chirp3-HD-Aoede"
    private const val VOICE_LANGUAGE = "en-US"
    private const val VOICE_GENDER   = "FEMALE"

    // True when the selected voice is a Chirp3-HD model (different API request format)
    private val isChirp3Voice get() = VOICE_NAME.contains("Chirp3")

    // ── Pronunciation defaults (overridden at runtime by user settings) ───────
    //
    // SPEAKING_RATE: 0.80 is deliberately slower than natural speech.
    // At this rate every syllable is fully pronounced — ideal for spelling games.
    // Range: 0.25 (very slow) – 4.0 (very fast). Natural = 1.0.
    //
    // PITCH: Slight lowering (-1.0 semitone) makes the voice sound more
    // authoritative and easier to distinguish on phone speakers.
    // Range: -20.0 to +20.0 semitones.
    //
    // VOLUME_GAIN_DB: +2 dB boost so the voice cuts through background music.
    // Range: -96.0 to +16.0 dB. 0.0 = no change.
    //
    private const val DEFAULT_SPEAKING_RATE  = 1.0
    private const val DEFAULT_PITCH          = -1.0
    private const val VOLUME_GAIN_DB         = 2.0

    // Rate used when player taps the speaker button to replay
    private const val REPLAY_SPEAKING_RATE   = 0.90

    // Runtime-adjustable rate and pitch (updated from Settings screen)
    @Volatile private var speakingRate: Double = DEFAULT_SPEAKING_RATE
    @Volatile private var pitch: Double = DEFAULT_PITCH

    // ─────────────────────────────────────────────────────────────────────────

    private var appContext: Context? = null
    private var mediaPlayer: MediaPlayer? = null

    // Android built-in TTS — used only when Cloud API is unavailable
    private var fallbackTts: TextToSpeech? = null
    private var fallbackReady = false

    // Disk cache folder inside internal storage (not cleared by OS without user action)
    private var audioCacheDir: File? = null

    // ── Coroutine scope ───────────────────────────────────────────────────────
    private var scope: CoroutineScope = buildScope()
    private fun buildScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Kept for API compatibility with callers that use setMode()
    @Suppress("MemberVisibilityCanBePrivate")
    var currentMode: String = "learn"
        private set

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Must be called once before any speak() call.
     * Safe to call multiple times (e.g. on activity recreate).
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext

        if (!scope.isActive) scope = buildScope()

        audioCacheDir = File(context.applicationContext.cacheDir, "gcloud_tts").also {
            if (!it.exists()) it.mkdirs()
        }

        // Clear any cache files that were generated with the old double-pronunciation SSML.
        // We detect them by the absence of the rate/pitch suffix in the filename (old format: "word.mp3").
        // New format: "word_r100_pm10.mp3" — always has underscores after the word part.
        audioCacheDir?.listFiles()?.forEach { file ->
            val name = file.nameWithoutExtension
            // Old cached files don't have the "_rXXX_" rate tag — delete them
            if (!name.contains(Regex("_r\\d+_"))) {
                file.delete()
                Log.d(TAG, "Deleted legacy cache file: ${file.name}")
            }
        }

        if (fallbackTts == null) {
            fallbackTts = TextToSpeech(context.applicationContext) { status ->
                fallbackReady = (status == TextToSpeech.SUCCESS)
                if (fallbackReady) {
                    fallbackTts?.language = Locale.US
                    // Fallback also uses slower, clearer settings
                    fallbackTts?.setSpeechRate(speakingRate.toFloat())
                    fallbackTts?.setPitch(1.0f)
                    Log.d(TAG, "Android TTS fallback ready")
                } else {
                    Log.w(TAG, "Android TTS fallback init failed (status=$status)")
                }
            }
        }

        Log.d(TAG, "GoogleCloudTTSManager initialized, voice=$VOICE_NAME rate=$speakingRate pitch=$pitch")
    }

    /**
     * Update speaking rate and pitch at runtime from the Settings screen.
     *
     * These values are forwarded directly to the Google Cloud TTS API on every
     * new word fetch. Already-cached words are NOT re-fetched automatically —
     * call [clearCache] after changing settings if you want all words re-synthesized
     * with the new parameters.
     *
     * @param rate  Speaking rate: 0.25 (very slow) – 4.0 (very fast). 1.0 = normal.
     * @param pitchSemitones  Pitch shift in semitones: -20.0 to +20.0. 0.0 = no change.
     */
    fun updateVoiceSettings(rate: Float, pitchSemitones: Float) {
        speakingRate = rate.toDouble().coerceIn(0.25, 4.0)
        pitch = pitchSemitones.toDouble().coerceIn(-20.0, 20.0)
        // Also update fallback TTS if it's ready
        if (fallbackReady) {
            fallbackTts?.setSpeechRate(rate.coerceIn(0.25f, 4.0f))
        }
        Log.d(TAG, "Voice settings updated: rate=$speakingRate pitch=$pitch")
    }

    /** Kept for API compatibility — no longer changes the voice */
    fun setMode(mode: String) {
        currentMode = if (mode == "spell") "spell" else "learn"
    }

    // Tracks the currently active fetch job so we can cancel it if speak() is called again
    private var activeSpeakJob: kotlinx.coroutines.Job? = null
    // Tracks last spoken text to prevent duplicate calls for same word
    private var lastSpokenText: String = ""
    private var lastSpeakTime: Long = 0L

    /**
     * Synthesize [text] and play it.
     *
     * @param slow  Pass true when the player taps the replay/speaker button.
     *              The word is spoken at 0.90 speed so every syllable is clear.
     *              Normal auto-play (new word shown) uses rate 1.0 by default.
     *
     * Audio is cached to disk — normal and slow variants are cached separately.
     * Any previous in-flight fetch is cancelled to prevent stale audio playback.
     */
    fun speak(text: String, slow: Boolean = false, onComplete: (() -> Unit)? = null) {
        val clean = cleanText(text)
        if (clean.isBlank()) { onComplete?.invoke(); return }

        val ctx = appContext
        if (ctx == null) {
            Log.e(TAG, "speak() called before initialize()")
            onComplete?.invoke()
            return
        }

        // Защита от двойного воспроизведения: если то же слово запрошено менее чем через 600ms — игнорируем
        val now = System.currentTimeMillis()
        if (clean == lastSpokenText && !slow && (now - lastSpeakTime) < 1200L) {
            Log.d(TAG, "Duplicate speak() ignored for \"$clean\" (${now - lastSpeakTime}ms ago)")
            return
        }
        lastSpokenText = clean
        lastSpeakTime = now

        if (!scope.isActive) scope = buildScope()

        val cached = cacheFile(clean, slow)

        activeSpeakJob?.cancel()
        activeSpeakJob = null
        stopMediaOnly() // always kill any in-flight audio before starting new word

        if (cached.exists() && cached.length() > 0) {
            Log.d(TAG, "Cache hit [slow=$slow]: \"$clean\"")
            playFile(cached, onComplete)
        } else {
            activeSpeakJob = scope.launch {
                val bytes = fetchFromCloud(clean, slow)
                if (bytes != null && bytes.isNotEmpty()) {
                    try { cached.writeBytes(bytes) } catch (e: Exception) {
                        Log.e(TAG, "Failed to write cache: ${e.message}")
                    }
                    withContext(Dispatchers.Main) { playFile(cached, onComplete) }
                } else {
                    Log.w(TAG, "Cloud TTS failed for \"$clean\", falling back")
                    withContext(Dispatchers.Main) { speakWithFallback(clean, onComplete) }
                }
            }
        }
    }

    /**
     * Stop any in-progress playback immediately.
     * Does NOT reset lastSpokenText — the 600ms dedup window expires on its own.
     * Resetting it here would let a second speak() call bypass dedup and cause
     * the word to be pronounced twice.
     */
    fun stop() {
        activeSpeakJob?.cancel()
        activeSpeakJob = null
        stopMediaOnly()
    }

    /** Stops media without touching dedup state. Called internally by playFile(). */
    private fun stopMediaOnly() {
        try {
            mediaPlayer?.run {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopMediaOnly() error: ${e.message}")
        } finally {
            mediaPlayer = null
        }
        try { fallbackTts?.stop() } catch (_: Exception) {}
    }

    /**
     * Release all resources.
     * Call from Activity.onDestroy(). initialize() must be called again before next use.
     */
    fun shutdown() {
        stop()
        lastSpokenText = ""   // safe to clear on full shutdown
        scope.cancel()
        fallbackTts?.shutdown()
        fallbackTts = null
        fallbackReady = false
        Log.d(TAG, "GoogleCloudTTSManager shut down")
    }

    /**
     * Pre-fetch and cache a list of words in the background.
     * Words already cached are skipped. Call this once when a lesson loads
     * so audio is ready before the user reaches each word.
     */
    fun preCacheWords(words: List<String>) {
        if (!scope.isActive) scope = buildScope()
        words.forEach { word ->
            val clean = cleanText(word)
            if (clean.isBlank()) return@forEach
            // Кэшируем normal (slow=false) версию — именно её использует авто-воспроизведение
            val file = cacheFile(clean, slow = false)
            if (!file.exists() || file.length() == 0L) {
                scope.launch {
                    val bytes = fetchFromCloud(clean, slow = false)
                    if (bytes != null && bytes.isNotEmpty()) {
                        try {
                            file.writeBytes(bytes)
                            Log.d(TAG, "Pre-cached: \"$clean\"")
                        } catch (e: Exception) {
                            Log.e(TAG, "Pre-cache write error for \"$clean\": ${e.message}")
                        }
                    }
                }
            }
        }
    }

    /** Delete all cached MP3 files to free up disk space */
    fun clearCache() {
        val count = audioCacheDir?.listFiles()?.count { it.delete() } ?: 0
        Log.d(TAG, "Cache cleared ($count files deleted)")
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Build SSML markup for a word or phrase.
     *
     * @param slow  When true (replay button), uses REPLAY_SPEAKING_RATE (0.90)
     *              so the user hears every syllable more clearly on demand.
     *              On auto-play (new word shown) uses the normal speakingRate (1.0).
     */
    private fun buildSsml(text: String, slow: Boolean = false): String {
        val escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        val rate = if (slow) REPLAY_SPEAKING_RATE else speakingRate
        val pitchTag = if (pitch >= 0) "+${pitch}st" else "${pitch}st"

        return """
            <speak>
                <prosody rate="$rate" pitch="$pitchTag">$escaped</prosody>
            </speak>
        """.trimIndent()
    }

    /**
     * POST to the Google Cloud TTS REST API.
     *
     * Automatically selects the correct request format:
     *  • Chirp 3 HD voices → plain text input, no speakingRate/pitch/effectsProfileId
     *  • Neural2 / Studio  → SSML input with rate, pitch, volumeGainDb
     */
    private fun fetchFromCloud(text: String, slow: Boolean = false): ByteArray? {
        if (API_KEY.startsWith("YOUR_")) {
            Log.e(TAG, "API KEY NOT SET — open GoogleCloudTTSManager.kt and set API_KEY.")
            return null
        }

        return try {
            val conn = URL("$TTS_ENDPOINT?key=$API_KEY")
                .openConnection() as HttpURLConnection

            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 15_000
            }

            val requestBody = if (isChirp3Voice) {
                // ── Chirp 3 HD: plain text only, no SSML / rate / pitch ────────
                JSONObject().apply {
                    put("input", JSONObject().put("text", text))
                    put("voice", JSONObject().apply {
                        put("languageCode", VOICE_LANGUAGE)
                        put("name", VOICE_NAME)
                    })
                    put("audioConfig", JSONObject().apply {
                        put("audioEncoding", "MP3")
                        put("volumeGainDb", VOLUME_GAIN_DB)
                    })
                }.toString()
            } else {
                // ── Neural2 / Studio: full SSML with rate and pitch ───────────
                val ssml = buildSsml(text, slow)
                JSONObject().apply {
                    put("input", JSONObject().put("ssml", ssml))
                    put("voice", JSONObject().apply {
                        put("languageCode", VOICE_LANGUAGE)
                        put("name", VOICE_NAME)
                        put("ssmlGender", VOICE_GENDER)
                    })
                    put("audioConfig", JSONObject().apply {
                        put("audioEncoding", "MP3")
                        put("speakingRate", speakingRate)
                        put("pitch", pitch)
                        put("volumeGainDb", VOLUME_GAIN_DB)
                        put("sampleRateHertz", 24000)
                        put("effectsProfileId", org.json.JSONArray().put("small-bluetooth-speaker-class-device"))
                    })
                }.toString()
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(requestBody) }

            val httpCode = conn.responseCode
            if (httpCode != HttpURLConnection.HTTP_OK) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "(no body)"
                Log.e(TAG, "Cloud TTS HTTP $httpCode for \"$text\": $errorBody")
                conn.disconnect()
                return null
            }

            val responseJson = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val base64Audio = JSONObject(responseJson).getString("audioContent")
            Base64.decode(base64Audio, Base64.DEFAULT)

        } catch (e: Exception) {
            Log.e(TAG, "fetchFromCloud error for \"$text\": ${e.message}", e)
            null
        }
    }

    /** Play an MP3 file through MediaPlayer */
    private fun playFile(file: File, onComplete: (() -> Unit)?) {
        stopMediaOnly() // release previous player without resetting dedup state
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)

                // Respect the voice volume slider from settings
                val vol = AppAudioManager.ttsVolumeGain().coerceIn(0f, 1f)
                setVolume(vol, vol)

                setOnCompletionListener {
                    onComplete?.invoke()
                    it.release()
                    if (mediaPlayer == it) mediaPlayer = null
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    onComplete?.invoke()
                    mp.release()
                    if (mediaPlayer == mp) mediaPlayer = null
                    true
                }

                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "playFile error for ${file.name}: ${e.message}", e)
            mediaPlayer = null
            onComplete?.invoke()
        }
    }

    /** Last-resort: Android built-in TTS */
    private fun speakWithFallback(text: String, onComplete: (() -> Unit)?) {
        if (!fallbackReady || fallbackTts == null) {
            Log.w(TAG, "Fallback TTS not ready — skipping speech")
            onComplete?.invoke()
            return
        }
        try {
            val vol = AppAudioManager.ttsVolumeGain().coerceIn(0f, 1f)
            val params = android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, vol)
            }
            fallbackTts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tts_${System.currentTimeMillis()}")

            // Android TTS has no reliable completion callback for QUEUE_FLUSH, so estimate
            val estimatedMs = (text.length * 80L + 400L).coerceIn(400L, 4000L)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                onComplete?.invoke()
            }, estimatedMs)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback TTS error: ${e.message}")
            onComplete?.invoke()
        }
    }

    /** Strip characters that would produce bad synthesis or unsafe filenames.
     *  Also removes BOM (\uFEFF) and other invisible Unicode that TTS engines
     *  can misread as "LE" or other artefacts. */
    private fun cleanText(text: String): String {
        return text
            .replace("\uFEFF", "")           // BOM (byte-order mark)
            .replace("\u200B", "")           // zero-width space
            .replace("\u200C", "")           // zero-width non-joiner
            .replace("\u200D", "")           // zero-width joiner
            .replace("\u00AD", "")           // soft hyphen
            .replace("\u2028", "")           // line separator
            .replace("\u2029", "")           // paragraph separator
            .trim()
            .replace(Regex("[^\\w\\s'\\-]"), "")
            .trim()
    }

    /**
     * Derive a stable, filesystem-safe cache filename from the text + voice settings.
     * Rate and pitch are included so that changing settings auto-invalidates old cache.
     * Example: "credit card" at rate=0.80 pitch=-1.0 → "credit_card_r80_pm10.mp3"
     */
    private fun cacheFile(text: String, slow: Boolean = false): File {
        val safeName = text.lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(60)
        val rateTag  = "r${(speakingRate * 100).toInt()}"
        val pitchTag = if (pitch >= 0) "p${(pitch * 10).toInt()}" else "m${(-pitch * 10).toInt()}"
        // "slow" variant cached separately so both are available without re-fetching
        val slowTag  = if (slow) "_slow" else ""
        return File(audioCacheDir ?: appContext!!.cacheDir, "${safeName}_${rateTag}_${pitchTag}${slowTag}.mp3")
    }
}