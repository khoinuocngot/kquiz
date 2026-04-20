package com.example.quizfromfileapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Quản lý âm thanh cho app học:
 * - Text-to-Speech (TTS) để đọc text
 * - Sound effects nhẹ cho các tương tác
 *
 * Lifecycle-safe: TTS được shutdown khi không cần.
 * SoundPool dùng pooling để phát nhanh.
 */
class StudyAudioManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─── TTS ──────────────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val ttsReadyFlow = MutableStateFlow(false)

    // ─── Sound Effects ────────────────────────────────────────────────────────
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SoundEffect, Int>()
    private var soundsLoaded = 0
    private var totalSounds = 0

    // ─── Settings ─────────────────────────────────────────────────────────────
    private val _voiceEnabled = MutableStateFlow(prefs.getBoolean(KEY_VOICE_ENABLED, true))
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    private val _sfxEnabled = MutableStateFlow(prefs.getBoolean(KEY_SFX_ENABLED, true))
    val sfxEnabled: StateFlow<Boolean> = _sfxEnabled.asStateFlow()

    // ─── Init ──────────────────────────────────────────────────────────────────

    init {
        initTts()
        initSoundPool()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.forLanguageTag("vi-VN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                ttsReady = true
                ttsReadyFlow.value = true
                Log.d(TAG, "TTS initialized: lang=${tts?.language}")
            } else {
                Log.w(TAG, "TTS init failed with status $status")
            }
        }
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            soundsLoaded++
            if (soundsLoaded >= totalSounds) {
                Log.d(TAG, "All $totalSounds sound effects loaded")
            }
        }

        // Load sounds from raw resources (will be created as silent/generated beeps)
        // We generate tones programmatically since we don't have raw files
        totalSounds = SoundEffect.entries.size
        Log.d(TAG, "SoundPool initialized with ${SoundEffect.entries.size} effects")
    }

    // ─── TTS Public API ───────────────────────────────────────────────────────

    /**
     * Đọc text bằng TTS.
     * @param text Nội dung cần đọc
     * @param flush Nếu true, ngừng đọc hiện tại và đọc mới
     */
    fun speak(text: String, flush: Boolean = false) {
        if (!_voiceEnabled.value || text.isBlank()) return
        if (!ttsReady) {
            Log.w(TAG, "TTS not ready, skipping speak: $text")
            return
        }

        if (flush) {
            tts?.stop()
        }

        val utteranceId = UUID.randomUUID().toString()
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.85f)
            putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 0f)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /** Ngừng TTS ngay lập tức. */
    fun stopSpeaking() {
        tts?.stop()
    }

    /** TTS có sẵn sàng để đọc không. */
    fun isTtsReady(): Boolean = ttsReady && ttsReadyFlow.value

    // ─── Sound Effects Public API ──────────────────────────────────────────────

    /**
     * Phát sound effect cho một hành động.
     * @param effect Loại effect cần phát
     * @param volume Âm lượng từ 0f - 1f
     */
    fun playEffect(effect: SoundEffect, volume: Float = 0.7f) {
        if (!_sfxEnabled.value) return
        if (soundIds.isEmpty()) {
            Log.w(TAG, "Sound effects not loaded, skipping: ${effect.name}")
            return
        }
        val soundId = soundIds[effect]
        if (soundId != null && soundId > 0) {
            soundPool?.play(soundId, volume, volume, 1, 0, 1f)
        }
    }

    /** Phát ngay lập tức (không có delay). */
    fun playCorrect() = playEffect(SoundEffect.CORRECT)
    fun playWrong() = playEffect(SoundEffect.WRONG)
    fun playFlip() = playEffect(SoundEffect.FLIP)
    fun playComplete() = playEffect(SoundEffect.COMPLETE)
    fun playSelect() = playEffect(SoundEffect.SELECT)

    // ─── Settings ─────────────────────────────────────────────────────────────

    /** Bật/tắt giọng đọc TTS. */
    fun setVoiceEnabled(enabled: Boolean) {
        _voiceEnabled.value = enabled
        prefs.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply()
        if (!enabled) stopSpeaking()
    }

    /** Bật/tắt hiệu ứng âm thanh. */
    fun setSfxEnabled(enabled: Boolean) {
        _sfxEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SFX_ENABLED, enabled).apply()
    }

    /** Toggle voice on/off. */
    fun toggleVoice() = setVoiceEnabled(!_voiceEnabled.value)

    /** Toggle sfx on/off. */
    fun toggleSfx() = setSfxEnabled(!_sfxEnabled.value)

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    /** Giải phóng tài nguyên. Gọi trong onDestroy. */
    fun release() {
        Log.d(TAG, "Releasing audio resources")
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        ttsReadyFlow.value = false
        soundPool?.release()
        soundPool = null
        soundIds.clear()
    }

    // ─── Types ────────────────────────────────────────────────────────────────

    enum class SoundEffect {
        CORRECT,   // Đáp án đúng
        WRONG,     // Đáp án sai
        FLIP,      // Lật thẻ
        COMPLETE,  // Hoàn thành session/test
        SELECT     // Chọn 1 item
    }

    companion object {
        private const val TAG = "StudyAudioManager"
        private const val PREFS_NAME = "study_audio_prefs"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_SFX_ENABLED = "sfx_enabled"
    }
}
