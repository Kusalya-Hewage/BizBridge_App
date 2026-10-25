package com.example.bizbridgeapp

import android.content.Context
import android.content.SharedPreferences
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.BusinessApplication
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple manager that stores the signed‑in user's profile and local registered accounts/applications.
 * Uses org.json (available in Android SDK) – no extra Gson dependency required.
 */
object UserSessionManager {
    private const val PREF_NAME = "bizbridge_user_session"
    private const val KEY_PROFILE = "user_profile"
    private const val KEY_REGISTERED_ACCOUNTS = "registered_accounts"
    private const val KEY_APPLICATIONS = "local_business_applications"
    private const val KEY_BUSINESSES = "local_businesses"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        seedDefaultAccountsIfEmpty()
    }

    private fun seedDefaultAccountsIfEmpty() {
        if (getRegisteredAccount("admin@bizbridge.com") == null) {
            saveRegisteredAccount(
                "admin@bizbridge.com",
                "admin123",
                mapOf(
                    "uid" to "admin_bizbridge",
                    "fullName" to "Platform Administrator",
                    "email" to "admin@bizbridge.com",
                    "role" to "ADMIN",
                    "accountStatus" to "ACTIVE"
                )
            )
        }
        if (getRegisteredAccount("alex@bizbridge.example") == null) {
            saveRegisteredAccount(
                "alex@bizbridge.example",
                "admin123",
                mapOf(
                    "uid" to "admin_alex",
                    "fullName" to "Alex Morgan",
                    "email" to "alex@bizbridge.example",
                    "role" to "ADMIN",
                    "accountStatus" to "ACTIVE"
                )
            )
        }
    }

    /** Save a map (profile) as a JSON string. */
    fun saveProfile(profile: Map<String, Any>) {
        if (!::prefs.isInitialized) return
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

        // If this is a business owner, automatically ensure application & business exist
        val role = data["role"] as? String ?: ""
        if (role.equals("BUSINESS_OWNER", ignoreCase = true)) {
            val uid = (data["uid"] as? String)?.ifBlank { null } ?: "owner_${normalizedEmail.hashCode()}"
            val bizId = (data["businessId"] as? String)?.ifBlank { null } ?: "biz_${uid.take(12)}"
            val appId = "app_${bizId.removePrefix("biz_")}"
            val bizName = (data["businessName"] as? String)?.ifBlank { null } ?: "My Business"
            val ownerName = (data["fullName"] as? String)?.ifBlank { null } ?: "Business Owner"
            val category = (data["category"] as? String)?.ifBlank { null } ?: "Hair salon"
            val phone = (data["phone"] as? String) ?: ""

            saveApplication(
                BusinessApplication(
                    id = appId,
                    ownerId = uid,
                    businessId = bizId,
                    businessName = bizName,
                    ownerName = ownerName,
                    email = normalizedEmail,
                    contactNumber = phone,
                    category = category,
                    address = "Colombo, Sri Lanka",
                    status = (data["approvalStatus"] as? String) ?: "PENDING"
                )
            )

            saveBusiness(
                Business(
                    id = bizId,
                    ownerId = uid,
                    name = bizName,
                    category = category,
                    contactNumber = phone,
                    address = "Colombo, Sri Lanka",
                    approved = (data["approved"] as? Boolean) ?: false,
                    active = (data["active"] as? Boolean) ?: false,
                    approvalStatus = (data["approvalStatus"] as? String) ?: "PENDING",
                    verificationStatus = (data["verificationStatus"] as? String) ?: "PENDING",
                    trustScore = 80,
                    rating = 0.0,
                    reviewCount = 0
                )
            )
        }
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

    /** Return all registered accounts (for Admin user management) */
    fun getAllRegisteredAccounts(): List<Map<String, Any>> {
        if (!::prefs.isInitialized) return emptyList()
        val accountsJson = prefs.getString(KEY_REGISTERED_ACCOUNTS, "{}") ?: "{}"
        val accountsObj = JSONObject(accountsJson)
        val result = mutableListOf<Map<String, Any>>()
        val keys = accountsObj.keys()
        while (keys.hasNext()) {
            val emailKey = keys.next()
            val obj = accountsObj.getJSONObject(emailKey)
            val map = mutableMapOf<String, Any>()
            val innerKeys = obj.keys()
            while (innerKeys.hasNext()) {
                val k = innerKeys.next()
                map[k] = obj.get(k)
            }
            result.add(map)
        }
        return result
    }

    /** Save or update a business application */
    fun saveApplication(app: BusinessApplication) {
        if (!::prefs.isInitialized) return
        val current = getAllApplications().toMutableList()
        val index = current.indexOfFirst { it.id == app.id || it.businessId == app.businessId }
        if (index >= 0) {
            current[index] = app
        } else {
            current.add(0, app)
        }
        val array = JSONArray()
        for (item in current) {
            val o = JSONObject()
            o.put("id", item.id)
            o.put("ownerId", item.ownerId)
            o.put("businessId", item.businessId)
            o.put("businessName", item.businessName)
            o.put("ownerName", item.ownerName)
            o.put("email", item.email)
            o.put("contactNumber", item.contactNumber)
            o.put("category", item.category)
            o.put("address", item.address)
            o.put("status", item.status)
            o.put("rejectionReason", item.rejectionReason)
            array.put(o)
        }
        prefs.edit().putString(KEY_APPLICATIONS, array.toString()).apply()
    }

    /** Retrieve all saved business applications */
    fun getAllApplications(): List<BusinessApplication> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(KEY_APPLICATIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = mutableListOf<BusinessApplication>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                BusinessApplication(
                    id = o.optString("id"),
                    ownerId = o.optString("ownerId"),
                    businessId = o.optString("businessId"),
                    businessName = o.optString("businessName"),
                    ownerName = o.optString("ownerName"),
                    email = o.optString("email"),
                    contactNumber = o.optString("contactNumber"),
                    category = o.optString("category"),
                    address = o.optString("address", "Colombo, Sri Lanka"),
                    status = o.optString("status", "PENDING"),
                    rejectionReason = o.optString("rejectionReason", "")
                )
            )
        }
        return list
    }

    /** Update application status */
    fun updateApplicationStatus(appIdOrBizId: String, status: String, reason: String = "") {
        if (!::prefs.isInitialized) return
        val apps = getAllApplications().map {
            if (it.id == appIdOrBizId || it.businessId == appIdOrBizId) {
                it.copy(status = status, rejectionReason = reason)
            } else it
        }
        val array = JSONArray()
        for (item in apps) {
            val o = JSONObject()
            o.put("id", item.id)
            o.put("ownerId", item.ownerId)
            o.put("businessId", item.businessId)
            o.put("businessName", item.businessName)
            o.put("ownerName", item.ownerName)
            o.put("email", item.email)
            o.put("contactNumber", item.contactNumber)
            o.put("category", item.category)
            o.put("address", item.address)
            o.put("status", item.status)
            o.put("rejectionReason", item.rejectionReason)
            array.put(o)
        }
        prefs.edit().putString(KEY_APPLICATIONS, array.toString()).apply()
    }

    /** Save or update business */
    fun saveBusiness(biz: Business) {
        if (!::prefs.isInitialized) return
        val current = getAllBusinesses().toMutableList()
        val index = current.indexOfFirst { it.id == biz.id }
        if (index >= 0) {
            current[index] = biz
        } else {
            current.add(0, biz)
        }
        val array = JSONArray()
        for (b in current) {
            val o = JSONObject()
            o.put("id", b.id)
            o.put("ownerId", b.ownerId)
            o.put("name", b.name)
            o.put("category", b.category)
            o.put("description", b.description)
            o.put("address", b.address)
            o.put("contactNumber", b.contactNumber)
            o.put("approved", b.approved)
            o.put("active", b.active)
            o.put("approvalStatus", b.approvalStatus)
            o.put("verificationStatus", b.verificationStatus)
            o.put("trustScore", b.trustScore)
            o.put("rating", b.rating ?: 0.0)
            o.put("reviewCount", b.reviewCount)
            array.put(o)
        }
        prefs.edit().putString(KEY_BUSINESSES, array.toString()).apply()
    }

    /** Retrieve all businesses */
    fun getAllBusinesses(): List<Business> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(KEY_BUSINESSES, null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = mutableListOf<Business>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                Business(
                    id = o.optString("id"),
                    ownerId = o.optString("ownerId"),
                    name = o.optString("name"),
                    category = o.optString("category"),
                    description = o.optString("description"),
                    address = o.optString("address"),
                    contactNumber = o.optString("contactNumber"),
                    approved = o.optBoolean("approved", false),
                    active = o.optBoolean("active", false),
                    approvalStatus = o.optString("approvalStatus", "PENDING"),
                    verificationStatus = o.optString("verificationStatus", "PENDING"),
                    trustScore = o.optInt("trustScore", 80),
                    rating = o.optDouble("rating", 0.0),
                    reviewCount = o.optInt("reviewCount", 0)
                )
            )
        }
        return list
    }

    /** Update business approval status across session & stored businesses */
    fun updateBusinessApprovalStatus(
        businessId: String,
        approved: Boolean,
        active: Boolean,
        approvalStatus: String,
        verificationStatus: String
    ) {
        if (!::prefs.isInitialized) return
        val businesses = getAllBusinesses().map {
            if (it.id == businessId) {
                it.copy(
                    approved = approved,
                    active = active,
                    approvalStatus = approvalStatus,
                    verificationStatus = verificationStatus
                )
            } else it
        }
        val array = JSONArray()
        for (b in businesses) {
            val o = JSONObject()
            o.put("id", b.id)
            o.put("ownerId", b.ownerId)
            o.put("name", b.name)
            o.put("category", b.category)
            o.put("description", b.description)
            o.put("address", b.address)
            o.put("contactNumber", b.contactNumber)
            o.put("approved", b.approved)
            o.put("active", b.active)
            o.put("approvalStatus", b.approvalStatus)
            o.put("verificationStatus", b.verificationStatus)
            o.put("trustScore", b.trustScore)
            o.put("rating", b.rating ?: 0.0)
            o.put("reviewCount", b.reviewCount)
            array.put(o)
        }
        prefs.edit().putString(KEY_BUSINESSES, array.toString()).apply()

        // Also update current active session profile if it belongs to this business
        val activeProfile = getProfile()
        if (activeProfile != null) {
            val currentBizId = activeProfile["businessId"] as? String
            if (currentBizId == businessId) {
                val updated = activeProfile.toMutableMap()
                updated["approved"] = approved
                updated["active"] = active
                updated["approvalStatus"] = approvalStatus
                updated["verificationStatus"] = verificationStatus
                saveProfile(updated)
            }
        }
    }

    /** Clear stored session data – on logout, preserves registered accounts. */
    fun clear() {
        if (!::prefs.isInitialized) return
        prefs.edit().remove(KEY_PROFILE).apply()
    }
}
