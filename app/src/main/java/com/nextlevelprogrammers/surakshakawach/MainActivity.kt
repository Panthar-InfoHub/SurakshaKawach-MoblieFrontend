package com.nextlevelprogrammers.surakshakawach

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.nextlevelprogrammers.surakshakawach.api.UserPreferences
import com.nextlevelprogrammers.surakshakawach.ui.LoginScreen
import com.nextlevelprogrammers.surakshakawach.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var networkMonitor: NetworkMonitor
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Authentication
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val isUserCreated = UserPreferences.isUserCreated(this)

        // Initialize and start network monitoring
        networkMonitor = NetworkMonitor(this, object : NetworkMonitor.NetworkCallback {
            override fun onNetworkChanged(isAvailable: Boolean) {
                coroutineScope.launch {
                    handleNetworkChange(isAvailable)
                }
            }
        })

        // Handle Firebase authentication logic
        if (currentUser != null) {
            currentUser.getIdToken(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val idToken = task.result?.token
                    if (idToken != null && isUserCreated) {
                        navigateToActivity(HomeActivity::class.java)
                    } else {
                        navigateToActivity(IntroductionActivity::class.java)
                    }
                } else {
                    // Authentication failed, navigate to login
                    auth.signOut()
                    Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                    navigateToActivity(LoginScreen::class.java)
                }
            }
        } else {
            // User not authenticated, navigate to login
            navigateToActivity(LoginScreen::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        networkMonitor.startMonitoring() // Start monitoring network changes
    }

    override fun onPause() {
        super.onPause()
        networkMonitor.stopMonitoring() // Stop monitoring network changes
    }

    private suspend fun handleNetworkChange(isConnected: Boolean) {
        if (isConnected) {
            Toast.makeText(this, "Network is available", Toast.LENGTH_SHORT).show()
            syncData()
        } else {
            Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun syncData() {
        withContext(Dispatchers.IO) {
            // Add background data sync logic here (e.g., fetching updates from a server)
            // This could involve calling a server API or updating local caches
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}