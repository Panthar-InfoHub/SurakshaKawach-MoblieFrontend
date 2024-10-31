package com.nextlevelprogrammers.surakshakawach

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.nextlevelprogrammers.surakshakawach.ui.IntroductionScreen
import com.nextlevelprogrammers.surakshakawach.ui.theme.SurakshaKawachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("Firebase", "FirebaseApp initialized: ${FirebaseApp.getInstance().name}")

        // Enable Edge-to-Edge UI
        enableEdgeToEdge()

        // Set the content using Jetpack Compose
        setContent {
            SurakshaKawachTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IntroductionScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}