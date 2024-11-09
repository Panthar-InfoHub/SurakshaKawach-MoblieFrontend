package com.nextlevelprogrammers.surakshakawach

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.nextlevelprogrammers.surakshakawach.ui.DashboardScreen
import com.nextlevelprogrammers.surakshakawach.ui.EmergencyContactsScreen
import com.nextlevelprogrammers.surakshakawach.ui.HomeScreen

class HomeActivity : ComponentActivity() {

    // Declare FusedLocationProviderClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Callback to trigger SOS action in HomeScreen
    private var triggerSOSAction: (() -> Unit)? = null

    // Broadcast receiver for wake word detection
    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.nextlevelprogrammers.surakshakawach.WAKE_WORD_DETECTED") {
                Log.d("HomeActivity", "Wake word detected! Triggering SOS action.")
                Toast.makeText(context, "Wake word detected!", Toast.LENGTH_SHORT).show()
                triggerSOSAction?.invoke() // Trigger SOS action if available
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Register the broadcast receiver
        val filter = IntentFilter("com.nextlevelprogrammers.surakshakawach.WAKE_WORD_DETECTED")
        registerReceiver(wakeWordReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // Start the VoiceRecognitionService
        startService(Intent(this, VoiceRecognitionService::class.java))

        // Set up the Composable content
        setContent {
            val navController = rememberNavController()
            AppNavigation(
                navController = navController,
                fusedLocationClient = fusedLocationClient,
                triggerSOSAction = { triggerSOSAction = it } // Pass the SOS action to AppNavigation
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wakeWordReceiver)  // Unregister receiver to prevent memory leaks
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    fusedLocationClient: FusedLocationProviderClient,
    triggerSOSAction: ((() -> Unit) -> Unit)
) {
    val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid

    // Navigation host defining screen routes
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                navController = navController,
                fusedLocationClient = fusedLocationClient,
                triggerSOSAction = triggerSOSAction as () -> Unit // Provide the SOS action callback
            )
        }
        composable("emergency_contacts") {
            EmergencyContactsScreen()
        }
        composable("dashboard") {
            if (firebaseUID != null) {
                DashboardScreen(firebaseUID = firebaseUID)
            } else {
                // Handle case where user is not logged in
            }
        }
    }
}