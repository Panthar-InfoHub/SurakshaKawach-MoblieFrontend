package com.nextlevelprogrammers.surakshakawach

import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class VoiceRecognitionService : Service() {

    private var porcupineManager: PorcupineManager? = null
    private var audioRecorder: AudioRecord? = null
    private var isListening = false
    private val sampleRate = 16000

    private val porcupineCallback = PorcupineManagerCallback { keywordIndex ->
        Log.d("VoiceRecognitionService", "Wake word detected! Performing action.")
        sendWakeWordDetectedBroadcast()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VoiceRecognitionService", "onStartCommand: Starting foreground service for wake word detection.")

        // Start as a foreground service
        startForeground(1, createForegroundNotification())

        if (checkRecordAudioPermission()) {
            startWakeWordDetection()
        } else {
            Log.e("VoiceRecognitionService", "onStartCommand: Microphone permission is not granted.")
        }
        return START_STICKY
    }

    private fun startWakeWordDetection() {
        Log.d("VoiceRecognitionService", "startWakeWordDetection: Initializing wake word detection...")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                Log.d("VoiceRecognitionService", "startWakeWordDetection: Setting up PorcupineManager...")

                porcupineManager = PorcupineManager.Builder()
                    .setAccessKey("VyTqW8d9vYCOqdxvNnuH7skFy+b6IBy5NGk2oMWCd48f/KMUCMQmJg==")
                    .setKeywordPath("help_us.ppn") // Ensure help_us.ppn is in assets
                    .setSensitivity(0.5f) // Adjust sensitivity between 0 to 1
                    .build(applicationContext, porcupineCallback)

                val bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioRecorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                if (audioRecorder?.state == AudioRecord.STATE_INITIALIZED) {
                    Log.d("VoiceRecognitionService", "startWakeWordDetection: AudioRecord successfully initialized with buffer size: $bufferSize")
                    audioRecorder?.startRecording()
                    isListening = true
                    porcupineManager?.start()
                    Log.d("VoiceRecognitionService", "startWakeWordDetection: PorcupineManager started successfully.")
                } else {
                    Log.e("VoiceRecognitionService", "startWakeWordDetection: Failed to initialize AudioRecord.")
                }
            } catch (e: Exception) {
                Log.e("VoiceRecognitionService", "startWakeWordDetection: Error initializing Porcupine - ${e.message}", e)
            }
        } else {
            Log.e("VoiceRecognitionService", "startWakeWordDetection: RECORD_AUDIO permission is not granted.")
        }
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

    private fun checkRecordAudioPermission(): Boolean {
        val permissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        Log.d("VoiceRecognitionService", "checkRecordAudioPermission: RECORD_AUDIO permission granted: $permissionGranted")
        return permissionGranted
    }

    private fun sendWakeWordDetectedBroadcast() {
        Log.d("VoiceRecognitionService", "sendWakeWordDetectedBroadcast: Broadcasting wake word detection intent.")
        val intent = Intent("com.nextlevelprogrammers.surakshakawach.WAKE_WORD_DETECTED")
        sendBroadcast(intent)
        Log.d("VoiceRecognitionService", "sendWakeWordDetectedBroadcast: Broadcast sent successfully.")
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