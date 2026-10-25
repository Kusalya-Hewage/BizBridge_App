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
    private const val KEY_REGISTERED_ACCOUNTS = "registered_accounts"
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
        if (!::prefs.isInitialized) return null
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

    /** Save a registered account for reliable local authentication fallback */
    fun saveRegisteredAccount(email: String, password: String, data: Map<String, Any>) {
        if (!::prefs.isInitialized) return
        val normalizedEmail = email.trim().lowercase()
        val accountsJson = prefs.getString(KEY_REGISTERED_ACCOUNTS, "{}") ?: "{}"
        val accountsObj = JSONObject(accountsJson)
        val accountData = JSONObject(data)
        accountData.put("password", password)
        accountData.put("email", normalizedEmail)
        accountsObj.put(normalizedEmail, accountData)
        prefs.edit().putString(KEY_REGISTERED_ACCOUNTS, accountsObj.toString()).apply()
    }

    /** Retrieve registered account by email */
    fun getRegisteredAccount(email: String): Map<String, Any>? {
        if (!::prefs.isInitialized) return null
        val normalizedEmail = email.trim().lowercase()
        val accountsJson = prefs.getString(KEY_REGISTERED_ACCOUNTS, "{}") ?: "{}"
        val accountsObj = JSONObject(accountsJson)
        if (!accountsObj.has(normalizedEmail)) return null
        val obj = accountsObj.getJSONObject(normalizedEmail)
        val map = mutableMapOf<String, Any>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.get(key)
        }
        return map
    }

    /** Clear stored session data – on logout, preserves registered accounts. */
    fun clear() {
        if (!::prefs.isInitialized) return
        prefs.edit().remove(KEY_PROFILE).apply()
    }
}
