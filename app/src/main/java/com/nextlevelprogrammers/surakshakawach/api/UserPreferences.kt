package com.nextlevelprogrammers.surakshakawach.api

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {

    private const val PREFS_NAME = "user_prefs"
    private const val KEY_USER_CREATED = "user_created"

    fun setUserCreated(context: Context, isCreated: Boolean) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(KEY_USER_CREATED, isCreated).apply()
    }

    fun isUserCreated(context: Context): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_USER_CREATED, false)
    }
}