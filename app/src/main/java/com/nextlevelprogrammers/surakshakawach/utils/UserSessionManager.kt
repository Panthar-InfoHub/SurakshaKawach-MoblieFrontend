package com.nextlevelprogrammers.surakshakawach.utils

import android.content.Context

object UserSessionManager {
    private const val PREF_NAME = "user_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_GENDER = "user_gender"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_SESSION_TIMESTAMP = "session_timestamp"
    private const val SESSION_TIMEOUT = 24 * 60 * 60 * 1000 // 24 hours

    fun saveSession(context: Context, userId: String, userName: String, userEmail: String, userGender: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_EMAIL, userEmail)
            .putString(KEY_USER_GENDER, userGender)
            .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun isSessionValid(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastTimestamp = prefs.getLong(KEY_SESSION_TIMESTAMP, 0)
        return System.currentTimeMillis() - lastTimestamp <= SESSION_TIMEOUT
    }

    fun getSession(context: Context): Map<String, String?> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return mapOf(
            "userId" to prefs.getString(KEY_USER_ID, null),
            "userName" to prefs.getString(KEY_USER_NAME, null),
            "userEmail" to prefs.getString(KEY_USER_EMAIL, null),
            "userGender" to prefs.getString(KEY_USER_GENDER, null)
        )
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}