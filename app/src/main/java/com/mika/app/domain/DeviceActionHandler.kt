package com.mika.app.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mika.app.MikaApplication
import com.mika.app.services.ReminderWorker
import java.util.concurrent.TimeUnit

object DeviceActionHandler {

    fun executeAction(context: Context, action: ActionTag): String {
        return when (action.type.lowercase()) {
            "open_app" -> openApp(context, action.rawArgs)
            "call" -> makeCall(context, action.rawArgs)
            "email_draft" -> draftEmail(context, action.rawArgs)
            "remind" -> scheduleReminder(context, action.rawArgs)
            "read_email" -> "Reading email snippet requested."
            "look" -> "Look action confirmed."
            else -> "Unknown action type: ${action.type}"
        }
    }

    private fun openApp(context: Context, appName: String): String {
        if (appName.isBlank()) return "App name empty."
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        val target = resolveInfos.firstOrNull {
            val label = it.loadLabel(pm).toString()
            label.contains(appName, ignoreCase = true) || appName.contains(label, ignoreCase = true)
        }

        return if (target != null) {
            val launchIntent = pm.getLaunchIntentForPackage(target.activityInfo.packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                "Opened ${target.loadLabel(pm)}"
            } else {
                "Could not launch ${target.loadLabel(pm)}"
            }
        } else {
            "App '$appName' not found."
        }
    }

    private fun makeCall(context: Context, contactOrNumber: String): String {
        if (contactOrNumber.isBlank()) return "No contact or number provided."
        val prefs = MikaApplication.instance.preferences
        var phoneNumber = contactOrNumber.filter { it.isDigit() || it == '+' }

        if (phoneNumber.isBlank()) {
            phoneNumber = lookupContactNumber(context, contactOrNumber) ?: ""
        }

        val dialUri = if (phoneNumber.isNotBlank()) "tel:$phoneNumber" else "tel:${Uri.encode(contactOrNumber)}"
        val intent = if (prefs.directCallingAllowed && phoneNumber.isNotBlank()) {
            Intent(Intent.ACTION_CALL, Uri.parse(dialUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse(dialUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            "Initiated call for $contactOrNumber"
        } catch (e: Exception) {
            "Failed to place call: ${e.localizedMessage}"
        }
    }

    fun lookupContactNumber(context: Context, nameQuery: String): String? {
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$nameQuery%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIndex != -1) {
                        return it.getString(numIndex)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun draftEmail(context: Context, args: String): String {
        val parts = args.split(",").map { it.trim() }
        val to = parts.getOrNull(0) ?: ""
        val subject = parts.getOrNull(1) ?: ""
        val body = parts.getOrNull(2) ?: ""

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$to")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            "Opened email draft"
        } catch (e: Exception) {
            "Failed to open email app: ${e.localizedMessage}"
        }
    }

    private fun scheduleReminder(context: Context, args: String): String {
        val parts = args.split(",").map { it.trim() }
        val minutes = parts.getOrNull(0)?.toLongOrNull() ?: 5L
        val text = parts.getOrNull(1) ?: "Reminder from Mika"

        val data = Data.Builder()
            .putString("reminder_text", text)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        return "Reminder scheduled in $minutes minutes!"
    }
}
