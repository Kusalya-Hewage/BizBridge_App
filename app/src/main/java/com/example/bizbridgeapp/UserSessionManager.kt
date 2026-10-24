package com.example.bizbridgeapp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Simple manager that stores the signed‑in customer's profile in SharedPreferences.
 * Uses org.json (available in Android SDK) – no extra Gson dependency required.
 */
object UserSessionManager {
    private const val PREF_NAME = "bizbridge_user_session"
    private const val KEY_PROFILE = "user_profile"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /** Save a map (profile) as a JSON string. */
    fun saveProfile(profile: Map<String, Any>) {
        val json = JSONObject(profile).toString()
        prefs.edit().putString(KEY_PROFILE, json).apply()
    }

    /** Retrieve the saved profile map, or null if none exists. */
    fun getProfile(): Map<String, Any>? {
        val json = prefs.getString(KEY_PROFILE, null) ?: return null
        val obj = JSONObject(json)
        val map = mutableMapOf<String, Any>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.get(key)
        }
        return map
    }

    /** Clear stored data – typically on logout. */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
