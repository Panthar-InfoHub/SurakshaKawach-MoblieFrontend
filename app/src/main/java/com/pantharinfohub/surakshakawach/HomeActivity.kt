package com.pantharinfohub.surakshakawach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class HomeActivity : ComponentActivity() {

    // Declare FusedLocationProviderClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            // Initialize the NavController for navigation
            val navController = rememberNavController()
            // Pass the NavController and FusedLocationProviderClient to the AppNavigation composable
            AppNavigation(navController, fusedLocationClient)
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, fusedLocationClient: FusedLocationProviderClient) {
    // Navigation host to define screen routes
    NavHost(navController = navController, startDestination = "home") {
        // Home Screen
        composable("home") {
            HomeScreen(navController, fusedLocationClient)
        }
        // Emergency Contacts Screen
        composable("emergency_contacts") {
            EmergencyContactsScreen()
        }
    }
}