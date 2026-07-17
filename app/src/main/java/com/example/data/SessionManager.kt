package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("GestiStockSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_USER_ROLE = "user_role"
    }

    fun loginUser(user: User) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULL_NAME, user.fullName)
            putString(KEY_USER_ROLE, user.role)
            apply()
        }
    }

    fun logoutUser() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putInt(KEY_USER_ID, -1)
            putString(KEY_USERNAME, null)
            putString(KEY_FULL_NAME, null)
            putString(KEY_USER_ROLE, null)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getLoggedInUser(): User? {
        if (!isLoggedIn()) return null
        val id = prefs.getInt(KEY_USER_ID, -1)
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val fullName = prefs.getString(KEY_FULL_NAME, "") ?: ""
        val role = prefs.getString(KEY_USER_ROLE, "") ?: ""
        return User(id = id, username = username, password = "", fullName = fullName, role = role)
    }
}
