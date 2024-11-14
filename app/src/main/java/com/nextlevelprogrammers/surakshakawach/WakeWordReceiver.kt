package com.nextlevelprogrammers.surakshakawach

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WakeWordReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WakeWordReceiver", "Wake word detected! Checking if HomeActivity is running.")

        if (!isHomeActivityRunning(context)) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(launchIntent)
            Log.d("WakeWordReceiver", "HomeActivity launched as it was not open.")
        } else {
            Log.d("WakeWordReceiver", "HomeActivity is already open; no action needed.")
        }
    }

    private fun isHomeActivityRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(Int.MAX_VALUE)
        for (task in runningTasks) {
            if (task.topActivity?.className == HomeActivity::class.java.name) {
                return true
            }
        }
        return false
    }
}