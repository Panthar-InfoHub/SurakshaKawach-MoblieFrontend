package com.nextlevelprogrammers.surakshakawach

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.auth.FirebaseAuth
import com.nextlevelprogrammers.surakshakawach.api.UserPreferences
import com.nextlevelprogrammers.surakshakawach.ui.IntroductionScreen
import com.nextlevelprogrammers.surakshakawach.ui.LoginScreen

class IntroductionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val isUserCreated = UserPreferences.isUserCreated(this)

        if (currentUser != null && isUserCreated) {
            navigateToHome()
        } else if (currentUser == null) {
            navigateToLogin()
        } else {
            setContent {
                IntroductionScreen()
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}