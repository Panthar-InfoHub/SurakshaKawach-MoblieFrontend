package com.pantharinfohub.surakshakawach

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.camera.core.ImageCapture
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SOSBackgroundService : Service() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var sosTicketId: String? = null

    companion object {
        const val CHANNEL_ID = "sos_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize the executor and location client
        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create notification channel for Android 8.0+ devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retrieve the SOS ticket ID from the intent
        sosTicketId = intent?.getStringExtra("sosTicketId")

        // Start the foreground service with a notification
        startForeground(NOTIFICATION_ID, createNotification())

        // Start background tasks
        startImageCapture()
        startLocationUpdates()

        return START_STICKY
    }

    // Function to create the notification for the foreground service
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SOS Active")
            .setContentText("SOS operations running in the background.")
            .setSmallIcon(R.drawable.ic_sos) // Ensure you have this drawable resource
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    // Function to create the notification channel for Android 8.0+ (API level 26)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SOS Background Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for SOS background service"
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Function to handle image capture in the background
    private fun startImageCapture() {
        // Logic for background image capture
    }

    // Function to handle location updates in the background
    private fun startLocationUpdates() {
        // Logic for background location updates
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown the executor and remove any pending callbacks
        cameraExecutor.shutdown()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
