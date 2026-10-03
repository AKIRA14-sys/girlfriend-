package com.mika.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import com.mika.app.MainActivity
import com.mika.app.MikaApplication
import com.mika.app.R
import java.util.Locale

class WakeWordService : Service() {

    private var porcupineManager: PorcupineManager? = null
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        initPorcupine()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "mika_wakeword_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hey Mika Wake Word",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, WakeWordService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Mika Voice Activation")
            .setContentText("Listening for 'Hey Mika'...")
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(2001, notification)
    }

    private fun initPorcupine() {
        val prefs = MikaApplication.instance.preferences
        val accessKey = prefs.picovoiceAccessKey
        if (accessKey.isBlank()) {
            Log.e("WakeWordService", "Picovoice AccessKey missing.")
            stopSelf()
            return
        }

        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeyword(Porcupine.BuiltInKeyword.PORCUPINE)
                .build(applicationContext) { keywordIndex ->
                    Log.d("WakeWordService", "Wake word detected!")
                    startOneShotSpeechRecognition()
                }
            porcupineManager?.start()
        } catch (e: PorcupineException) {
            Log.e("WakeWordService", "Failed to start Porcupine: ${e.localizedMessage}")
            stopSelf()
        }
    }

    private fun startOneShotSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        porcupineManager?.stop()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    resumeWakeWord()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()
                    if (!spokenText.isNullOrBlank()) {
                        Log.d("WakeWordService", "Recognized: $spokenText")
                    }
                    resumeWakeWord()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer?.startListening(intent)
    }

    private fun resumeWakeWord() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            porcupineManager?.start()
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
    }
}
