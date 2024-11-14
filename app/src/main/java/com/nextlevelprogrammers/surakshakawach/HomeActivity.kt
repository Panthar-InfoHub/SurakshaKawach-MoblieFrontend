package com.nextlevelprogrammers.surakshakawach

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.nextlevelprogrammers.surakshakawach.viewmodel.HomeViewModel

class HomeActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val homeViewModel: HomeViewModel by viewModels()

    // Define the BroadcastReceiver
    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            homeViewModel.activateSOS()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Register receiver with condition to support different API levels
        val intentFilter = IntentFilter("com.nextlevelprogrammers.surakshakawach.WAKE_WORD_DETECTED")
        registerReceiver(wakeWordReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)

        // Start voice recognition service
        startService(Intent(this, VoiceRecognitionService::class.java))

        setContent {
            val navController = rememberNavController()
            AppNavigation(navController, fusedLocationClient, homeViewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wakeWordReceiver)
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    fusedLocationClient: FusedLocationProviderClient,
    homeViewModel: HomeViewModel
) {
    val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid

    // Define the navigation routes
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                navController = navController,
                fusedLocationClient = fusedLocationClient,
                homeViewModel = homeViewModel // Pass ViewModel to HomeScreen
            )
        }
        composable("emergency_contacts") {
            EmergencyContactsScreen()
        }
        composable("dashboard") {
            if (firebaseUID != null) {
                DashboardScreen(firebaseUID = firebaseUID)
            }
        }
    }
}