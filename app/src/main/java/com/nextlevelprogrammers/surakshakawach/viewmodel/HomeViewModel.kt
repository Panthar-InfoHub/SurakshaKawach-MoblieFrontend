package com.nextlevelprogrammers.surakshakawach.viewmodel

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
}