package com.nextlevelprogrammers.surakshakawach

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
import com.google.firebase.auth.FirebaseAuth
import com.nextlevelprogrammers.surakshakawach.ui.DashboardScreen
import com.nextlevelprogrammers.surakshakawach.ui.EmergencyContactsScreen
import com.nextlevelprogrammers.surakshakawach.ui.HomeScreen

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

    // Retrieve the Firebase UID from the session using FirebaseAuth
    val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid

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
        // Dashboard Screen, pass the firebaseUID retrieved from the session
        composable("dashboard") {
            if (firebaseUID != null) {
                DashboardScreen(firebaseUID = firebaseUID)
            } else {
                // Handle case where user is not logged in
                // You could navigate back to a login screen or show an error
            }
        }
    }
}