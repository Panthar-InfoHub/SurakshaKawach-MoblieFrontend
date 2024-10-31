package com.nextlevelprogrammers.surakshakawach

import android.app.Application
import com.google.firebase.FirebaseApp

class SurakshaKawach : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase globally
        FirebaseApp.initializeApp(this)
    }
}