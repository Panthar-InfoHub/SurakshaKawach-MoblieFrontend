package com.nextlevelprogrammers.surakshakawach

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class WakeWordReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WakeWordReceiver", "Wake word detected! Checking if HomeActivity is running.")

        if (!isHomeActivityRunning(context)) {
            val launchIntent = Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            try {
                context.startActivity(launchIntent)
                Log.d("WakeWordReceiver", "HomeActivity launched as it was not open.")
            } catch (e: Exception) {
                Log.e("WakeWordReceiver", "Failed to launch HomeActivity: ${e.message}")
            }
        } else {
            Log.d("WakeWordReceiver", "HomeActivity is already open; no action needed.")
        }
    }

    private fun isHomeActivityRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val runningAppProcesses = activityManager.runningAppProcesses
            runningAppProcesses.any { process ->
                process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                        process.pkgList.contains(context.packageName)
            }
        } else {
            @Suppress("DEPRECATION")
            val runningTasks = activityManager.getRunningTasks(Int.MAX_VALUE)
            runningTasks.any { task ->
                task.topActivity?.className == HomeActivity::class.java.name
            }
        }
    }
}