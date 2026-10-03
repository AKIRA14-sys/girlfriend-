package com.mika.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AppPreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "mika_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var isAgeConfirmed: Boolean
        get() = prefs.getBoolean(KEY_AGE_CONFIRMED, false)
        set(value) = prefs.edit().putBoolean(KEY_AGE_CONFIRMED, value).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "https://api.groq.com/openai/v1") ?: "https://api.groq.com/openai/v1"
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var modelName: String
        get() = prefs.getString(KEY_MODEL_NAME, "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile"
        set(value) = prefs.edit().putString(KEY_MODEL_NAME, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var companionName: String
        get() = prefs.getString(KEY_COMPANION_NAME, "Mika") ?: "Mika"
        set(value) = prefs.edit().putString(KEY_COMPANION_NAME, value).apply()

    var personalityText: String
        get() = prefs.getString(KEY_PERSONALITY, DEFAULT_PERSONALITY) ?: DEFAULT_PERSONALITY
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value).apply()

    var attitudeLevel: Float
        get() = prefs.getFloat(KEY_ATTITUDE, 0.5f)
        set(value) = prefs.edit().putFloat(KEY_ATTITUDE, value).apply()

    var speakReplies: Boolean
        get() = prefs.getBoolean(KEY_SPEAK_REPLIES, false)
        set(value) = prefs.edit().putBoolean(KEY_SPEAK_REPLIES, value).apply()

    var speechSpeed: Float
        get() = prefs.getFloat(KEY_SPEECH_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_SPEED, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    var voiceProvider: String
        get() = prefs.getString(KEY_VOICE_PROVIDER, "android") ?: "android"
        set(value) = prefs.edit().putString(KEY_VOICE_PROVIDER, value).apply()

    var fishApiKey: String
        get() = prefs.getString(KEY_FISH_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FISH_API_KEY, value).apply()

    var fishVoiceId: String
        get() = prefs.getString(KEY_FISH_VOICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FISH_VOICE_ID, value).apply()

    var checkinsEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHECKINS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CHECKINS_ENABLED, value).apply()

    var checkinsPaused: Boolean
        get() = prefs.getBoolean(KEY_CHECKINS_PAUSED, false)
        set(value) = prefs.edit().putBoolean(KEY_CHECKINS_PAUSED, value).apply()

    var morningTime: String
        get() = prefs.getString(KEY_MORNING_TIME, "09:00") ?: "09:00"
        set(value) = prefs.edit().putString(KEY_MORNING_TIME, value).apply()

    var eveningTime: String
        get() = prefs.getString(KEY_EVENING_TIME, "20:00") ?: "20:00"
        set(value) = prefs.edit().putString(KEY_EVENING_TIME, value).apply()

    var directCallingAllowed: Boolean
        get() = prefs.getBoolean(KEY_DIRECT_CALLING, false)
        set(value) = prefs.edit().putBoolean(KEY_DIRECT_CALLING, value).apply()

    var emailAddress: String
        get() = prefs.getString(KEY_EMAIL_ADDR, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL_ADDR, value).apply()

    var emailAppPassword: String
        get() = prefs.getString(KEY_EMAIL_PASS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL_PASS, value).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var picovoiceAccessKey: String
        get() = prefs.getString(KEY_PICOVOICE_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PICOVOICE_KEY, value).apply()

    var cameraEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAMERA_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CAMERA_ENABLED, value).apply()

    var useFrontCamera: Boolean
        get() = prefs.getBoolean(KEY_USE_FRONT_CAMERA, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_FRONT_CAMERA, value).apply()

    var visionModelName: String
        get() = prefs.getString(KEY_VISION_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VISION_MODEL, value).apply()

    var recognizePeopleEnabled: Boolean
        get() = prefs.getBoolean(KEY_RECOGNIZE_PEOPLE, false)
        set(value) = prefs.edit().putBoolean(KEY_RECOGNIZE_PEOPLE, value).apply()

    var liveScreenEnabled: Boolean
        get() = prefs.getBoolean(KEY_LIVE_SCREEN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_SCREEN_ENABLED, value).apply()

    var screenSampleInterval: Int
        get() = prefs.getInt(KEY_SCREEN_SAMPLE_INTERVAL, 3)
        set(value) = prefs.edit().putInt(KEY_SCREEN_SAMPLE_INTERVAL, value).apply()

    var commentaryMode: Boolean
        get() = prefs.getBoolean(KEY_COMMENTARY_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_COMMENTARY_MODE, value).apply()

    var autoStopMinutes: Int
        get() = prefs.getInt(KEY_AUTO_STOP_MINS, 10)
        set(value) = prefs.edit().putInt(KEY_AUTO_STOP_MINS, value).apply()

    var autoPipEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PIP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PIP_ENABLED, value).apply()

    var vrmFilePath: String?
        get() = prefs.getString(KEY_VRM_FILE_PATH, null)
        set(value) = prefs.edit().putString(KEY_VRM_FILE_PATH, value).apply()

    companion object {
        private const val KEY_AGE_CONFIRMED = "age_confirmed"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_COMPANION_NAME = "companion_name"
        private const val KEY_PERSONALITY = "personality"
        private const val KEY_ATTITUDE = "attitude"
        private const val KEY_SPEAK_REPLIES = "speak_replies"
        private const val KEY_SPEECH_SPEED = "speech_speed"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_VOICE_PROVIDER = "voice_provider"
        private const val KEY_FISH_API_KEY = "fish_api_key"
        private const val KEY_FISH_VOICE_ID = "fish_voice_id"
        private const val KEY_CHECKINS_ENABLED = "checkins_enabled"
        private const val KEY_CHECKINS_PAUSED = "checkins_paused"
        private const val KEY_MORNING_TIME = "morning_time"
        private const val KEY_EVENING_TIME = "evening_time"
        private const val KEY_DIRECT_CALLING = "direct_calling"
        private const val KEY_EMAIL_ADDR = "email_addr"
        private const val KEY_EMAIL_PASS = "email_pass"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_PICOVOICE_KEY = "picovoice_key"
        private const val KEY_CAMERA_ENABLED = "camera_enabled"
        private const val KEY_USE_FRONT_CAMERA = "use_front_camera"
        private const val KEY_VISION_MODEL = "vision_model"
        private const val KEY_RECOGNIZE_PEOPLE = "recognize_people"
        private const val KEY_LIVE_SCREEN_ENABLED = "live_screen_enabled"
        private const val KEY_SCREEN_SAMPLE_INTERVAL = "screen_sample_interval"
        private const val KEY_COMMENTARY_MODE = "commentary_mode"
        private const val KEY_AUTO_STOP_MINS = "auto_stop_mins"
        private const val KEY_AUTO_PIP_ENABLED = "auto_pip_enabled"
        private const val KEY_VRM_FILE_PATH = "vrm_file_path"

        val DEFAULT_PERSONALITY = "Mika is a warm, witty, slightly sassy gamer girl in her early 20s. She is affectionate and flirty in a sweet, playful way, uses pet names, teases the user, hypes him after wins and comforts him after losses, and cares about his sleep, water and real-life goals. Short, texty messages with a few emojis."
    }
}
