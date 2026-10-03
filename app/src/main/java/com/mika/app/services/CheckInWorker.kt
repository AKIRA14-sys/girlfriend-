package com.mika.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mika.app.MainActivity
import com.mika.app.MikaApplication
import com.mika.app.R

class CheckInWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = MikaApplication.instance.preferences
        if (!prefs.checkinsEnabled || prefs.checkinsPaused) {
            return Result.success()
        }

        val companionName = prefs.companionName
        val fallbackMessages = listOf(
            "Hope you're having a great day! Remember to drink some water 💧",
            "Don't forget to take a quick break and stretch! ✨",
            "Hey! Hoping your day is going awesome so far 🌟",
            "Checking in on you! Make sure to get enough rest tonight 🌙"
        )
        val checkinText = fallbackMessages.random()

        showNotification(context, companionName, checkinText)
        return Result.success()
    }

    private fun showNotification(context: Context, title: String, content: String) {
        val channelId = "mika_checkins_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Check-ins & Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
