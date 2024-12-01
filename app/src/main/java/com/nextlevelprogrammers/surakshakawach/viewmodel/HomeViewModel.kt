package com.nextlevelprogrammers.surakshakawach.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel : ViewModel() {
    private val _triggerSOS = MutableStateFlow(false)
    val triggerSOS: StateFlow<Boolean> = _triggerSOS
    private var lastSOSActivationTime = System.currentTimeMillis()

    fun activateSOS() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSOSActivationTime > 1000) {
            _triggerSOS.tryEmit(true)
            lastSOSActivationTime = currentTime
        }
    }

    fun resetSOS() {
        _triggerSOS.tryEmit(false)
    }
    suspend fun fetchSessionData(firebaseUID: String): Map<String, String> {
        // Simulate fetching data (replace with actual implementation)
        return mapOf("key" to "value") // Example session data
    }

    suspend fun syncSessionData(firebaseUID: String, sessionData: Map<String, String>) {
        // Simulate syncing data (replace with actual implementation)
        Log.d("HomeViewModel", "Data synced for UID: $firebaseUID with session: $sessionData")
    }

}