package com.optuze.recordings.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.optuze.recordings.data.models.User

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val PREF_NAME = "RecordingsAppPrefs"
        const val JWT_TOKEN = "JWT_TOKEN"
        const val USER_DATA = "USER_DATA"
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(JWT_TOKEN, token)
        editor.apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(JWT_TOKEN, null)
    }

    fun saveUser(user: User) {
        val editor = prefs.edit()
        editor.putString(USER_DATA, gson.toJson(user))
        editor.apply()
    }

    fun getUser(): User? {
        val userJson = prefs.getString(USER_DATA, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else null
    }

    fun isLoggedIn(): Boolean {
        return getAuthToken() != null
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
} 