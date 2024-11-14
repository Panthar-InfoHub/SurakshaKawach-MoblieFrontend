package com.nextlevelprogrammers.surakshakawach

import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioRecord
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat

class VoiceRecognitionService : Service() {
    private var porcupineManager: PorcupineManager? = null
    private var audioRecorder: AudioRecord? = null
    private var isListening = false
    private val sampleRate = 16000

    private val porcupineCallback = PorcupineManagerCallback { keywordIndex ->
        Log.d("VoiceRecognitionService", "Wake word detected! Performing action.")
        openHomeActivityIfNotOpen()
        sendWakeWordDetectedBroadcast()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createForegroundNotification())
        // Initialize wake word detection
        startWakeWordDetection()
        return START_STICKY
    }

    private fun startWakeWordDetection() {
        porcupineManager = PorcupineManager.Builder()
            .setAccessKey("VyTqW8d9vYCOqdxvNnuH7skFy+b6IBy5NGk2oMWCd48f/KMUCMQmJg==")
            .setKeywordPath("help_us.ppn")
            .setSensitivity(1f)
            .build(applicationContext, porcupineCallback)
        porcupineManager?.start()
    }


    private fun openHomeActivityIfNotOpen() {
        if (!isHomeActivityRunning()) {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(launchIntent)
            Log.d("VoiceRecognitionService", "HomeActivity launched as it was not open.")
        } else {
            Log.d("VoiceRecognitionService", "HomeActivity is already open; no action needed.")
        }
    }

    private fun isHomeActivityRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(Int.MAX_VALUE)

        for (task in runningTasks) {
            if (task.topActivity?.className == MainActivity::class.java.name) {
                return true
            }
        }
        return false
    }


    private fun sendWakeWordDetectedBroadcast() {
        val intent = Intent("com.nextlevelprogrammers.surakshakawach.WAKE_WORD_DETECTED")
        sendBroadcast(intent)
    }

    private fun createForegroundNotification(): Notification {
        Log.d("VoiceRecognitionService", "createForegroundNotification: Creating foreground notification for the service.")

        val channelId = "VOICE_RECOGNITION_CHANNEL"
        val channelName = "Voice Recognition Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(notificationChannel)
            Log.d("VoiceRecognitionService", "createForegroundNotification: Notification channel created.")
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Listening for Wake Word")
            .setContentText("This service is running in the background.")
            .setSmallIcon(R.mipmap.icon) // Replace with your app icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("VoiceRecognitionService", "onTaskRemoved: Attempting to reschedule service after task removal.")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            Log.d("VoiceRecognitionService", "onTaskRemoved: Setting exact alarm for service restart.")
            val restartServiceIntent = Intent(applicationContext, this::class.java).also {
                it.setPackage(packageName)
            }
            val restartServicePendingIntent = PendingIntent.getService(
                this, 1, restartServiceIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
            )
        } else {
            Log.e("VoiceRecognitionService", "onTaskRemoved: Cannot schedule exact alarms without user permission.")
            promptUserForExactAlarmPermission()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun promptUserForExactAlarmPermission() {
        Log.d("VoiceRecognitionService", "promptUserForExactAlarmPermission: Prompting user to enable exact alarm permission.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                startActivity(intent)
                Log.d("VoiceRecognitionService", "promptUserForExactAlarmPermission: Prompt displayed successfully.")
            } catch (e: Exception) {
                Log.e("VoiceRecognitionService", "promptUserForExactAlarmPermission: Error displaying prompt - ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        Log.d("VoiceRecognitionService", "onDestroy: Stopping PorcupineManager and releasing AudioRecord.")

        porcupineManager?.stop()
        porcupineManager?.delete()

        audioRecorder?.stop()
        audioRecorder?.release()
        isListening = false
        Log.d("VoiceRecognitionService", "onDestroy: PorcupineManager stopped, AudioRecord released.")

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}