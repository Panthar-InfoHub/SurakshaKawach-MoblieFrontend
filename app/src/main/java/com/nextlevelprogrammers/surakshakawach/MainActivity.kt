package com.nextlevelprogrammers.surakshakawach

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.nextlevelprogrammers.surakshakawach.ui.LoginScreen
import com.nextlevelprogrammers.surakshakawach.api.UserPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Authentication
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val isUserCreated = UserPreferences.isUserCreated(this)

        // Determine the target activity
        if (currentUser != null) {
            // Revalidate Firebase authentication token
            currentUser.getIdToken(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val idToken = task.result?.token

                    if (idToken != null && isUserCreated) {
                        // User is authenticated and session exists
                        navigateToActivity(HomeActivity::class.java)
                    } else {
                        // User authenticated but session not created
                        navigateToActivity(IntroductionActivity::class.java)
                    }
                } else {
                    // Token validation failed, force login
                    auth.signOut()
                    navigateToActivity(LoginScreen::class.java)
                }
            }
        } else {
            // User is not authenticated, navigate to login
            navigateToActivity(LoginScreen::class.java)
        }
    }

    // Function to navigate to the specified activity
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}