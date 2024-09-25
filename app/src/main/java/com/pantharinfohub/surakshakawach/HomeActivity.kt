package com.pantharinfohub.surakshakawach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
            // Pass the FusedLocationProviderClient to the HomeScreen composable
            HomeScreen(fusedLocationClient)
        }
    }
}