package com.pantharinfohub.surakshakawach

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.pantharinfohub.surakshakawach.ui.theme.SurakshaKawachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        Log.d("Firebase", "FirebaseApp initialized: ${FirebaseApp.getInstance().name}")


        enableEdgeToEdge()
        setContent {
            SurakshaKawachTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IntroductionScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}