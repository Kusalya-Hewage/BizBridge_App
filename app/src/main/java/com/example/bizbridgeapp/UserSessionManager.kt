package com.example.bizbridgeapp

import android.content.Context
import android.content.SharedPreferences
import com.example.bizbridgeapp.model.AdminAuditLog
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.BusinessApplication
import com.example.bizbridgeapp.model.NotificationItem
import com.example.bizbridgeapp.model.Promotion
import com.example.bizbridgeapp.model.QualificationDocument
import com.example.bizbridgeapp.model.Service
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
    private const val KEY_QUALIFICATIONS = "local_qualifications"
    private const val KEY_AUDIT_LOGS = "local_admin_audit_logs"
    private const val KEY_SERVICES = "local_services"
    private const val KEY_PROMOTIONS = "local_promotions"
    private const val KEY_NOTIFICATIONS = "local_notifications"
    private const val KEY_BOOKINGS = "local_bookings"
    private const val KEY_LAST_KNOWN_LOCATION_NAME = "last_known_location_name"
    private const val KEY_LAST_KNOWN_LAT = "last_known_lat"
    private const val KEY_LAST_KNOWN_LNG = "last_known_lng"
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

        if (getAllAuditLogs().isEmpty()) {
            saveAuditLog(
                AdminAuditLog(
                    id = "log_init_console",
                    adminUid = "admin_bizbridge",
                    adminEmail = "admin@bizbridge.com",
                    actionType = "SYSTEM_INITIALIZED",
                    targetType = "CONSOLE",
                    targetId = "BizBridge Operations",
                    newStatus = "ACTIVE",
                    reason = "Platform Administration Console initialized",
                    createdAt = com.google.firebase.Timestamp.now()
                )
            )
        }

        // Clean out legacy demo qualification documents if any exist
        val demoDocIds = setOf("doc_hair_design", "doc_color_systems", "doc_experience_letter", "doc_massage_cert")
        val currentDocs = getAllQualificationDocuments()
        if (currentDocs.any { it.id in demoDocIds }) {
            val cleanedDocs = currentDocs.filterNot { it.id in demoDocIds }
            saveAllQualificationDocuments(cleanedDocs)
        }

        // Clean out legacy demo businesses from local storage if any exist
        val demoBizIds = setOf("biz_juniper_studio", "biz_oliver_beauty", "biz_daily_cut", "biz_lotus_wellness")
        val currentBiz = getAllBusinesses()
        if (currentBiz.any { it.id in demoBizIds }) {
            val cleanedBiz = currentBiz.filterNot { it.id in demoBizIds }
            val array = JSONArray()
            for (b in cleanedBiz) {
                array.put(businessToJson(b))
            }
            prefs.edit().putString(KEY_BUSINESSES, array.toString()).apply()
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

    /** Save last known location name and coordinates */
    fun saveLastKnownLocation(name: String, lat: Double, lng: Double) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putString(KEY_LAST_KNOWN_LOCATION_NAME, name)
            .putFloat(KEY_LAST_KNOWN_LAT, lat.toFloat())
            .putFloat(KEY_LAST_KNOWN_LNG, lng.toFloat())
            .apply()
    }

    /** Get the last resolved real location name */
    fun getLastKnownLocationName(): String? {
        if (!::prefs.isInitialized) return null
        return prefs.getString(KEY_LAST_KNOWN_LOCATION_NAME, null)
    }

    /** Get the last resolved real coordinates */
    fun getLastKnownCoordinates(): Pair<Double, Double>? {
        if (!::prefs.isInitialized || !prefs.contains(KEY_LAST_KNOWN_LAT)) return null
        return Pair(prefs.getFloat(KEY_LAST_KNOWN_LAT, 0f).toDouble(), prefs.getFloat(KEY_LAST_KNOWN_LNG, 0f).toDouble())
    }

    /** Update only the profile image URL in active session and registered account cache */
    fun updateProfileImage(imageUrl: String) {
        if (!::prefs.isInitialized || imageUrl.isBlank()) return
        val profile = getProfile()?.toMutableMap() ?: mutableMapOf()
        profile["profileImageUrl"] = imageUrl
        saveProfile(profile)

        val email = profile["email"] as? String
        if (!email.isNullOrBlank()) {
            val normalized = email.trim().lowercase()
            val accountsJson = prefs.getString(KEY_REGISTERED_ACCOUNTS, "{}") ?: "{}"
            try {
                val obj = JSONObject(accountsJson)
                if (obj.has(normalized)) {
                    val acc = obj.getJSONObject(normalized)
                    acc.put("profileImageUrl", imageUrl)
                    obj.put(normalized, acc)
                    prefs.edit().putString(KEY_REGISTERED_ACCOUNTS, obj.toString()).apply()
                }
            } catch (_: Exception) {}
        }
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

        // If this is a business owner, automatically ensure application & business exist without overriding customized data
        val role = data["role"] as? String ?: ""
        if (role.equals("BUSINESS_OWNER", ignoreCase = true)) {
            val uid = (data["uid"] as? String)?.ifBlank { null } ?: "owner_${normalizedEmail.hashCode()}"
            val bizId = (data["businessId"] as? String)?.ifBlank { null } ?: "biz_${uid.take(12)}"
            val appId = "app_${bizId.removePrefix("biz_")}"
            val existingBiz = getAllBusinesses().firstOrNull { it.id == bizId }
            val existingApp = getAllApplications().firstOrNull { it.id == appId || it.businessId == bizId }

            val bizName = (data["businessName"] as? String)?.ifBlank { null } ?: existingBiz?.name ?: "Business"
            val ownerName = (data["fullName"] as? String)?.ifBlank { null } ?: existingApp?.ownerName ?: "Business Owner"
            val category = (data["category"] as? String)?.ifBlank { null } ?: existingBiz?.category ?: "Hair salon"
            val phone = (data["phone"] as? String) ?: existingBiz?.contactNumber ?: ""
            val isApproved = (data["approved"] as? Boolean) ?: existingBiz?.approved ?: false
            val isActive = (data["active"] as? Boolean) ?: existingBiz?.active ?: false
            val approvalStatus = (data["approvalStatus"] as? String) ?: existingBiz?.approvalStatus ?: "PENDING"
            val verificationStatus = (data["verificationStatus"] as? String) ?: existingBiz?.verificationStatus ?: "PENDING"
            val trustScore = (data["trustScore"] as? Int) ?: existingBiz?.trustScore ?: 0
            val imageUrl = (data["imageUrl"] as? String)?.ifBlank { null } ?: existingBiz?.imageUrl

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
                    address = existingBiz?.address?.ifBlank { "Colombo, Sri Lanka" } ?: "Colombo, Sri Lanka",
                    status = approvalStatus
                )
            )

            val hash = Math.abs(bizId.hashCode())
            val defaultLat = existingBiz?.latitude ?: (6.9271 + ((hash % 100) - 50) * 0.0003)
            val defaultLng = existingBiz?.longitude ?: (79.8612 + (((hash / 100) % 100) - 50) * 0.0003)

            saveBusiness(
                Business(
                    id = bizId,
                    ownerId = uid,
                    name = bizName,
                    category = category,
                    contactNumber = phone,
                    address = existingBiz?.address?.ifBlank { "Colombo, Sri Lanka" } ?: "Colombo, Sri Lanka",
                    imageUrl = imageUrl,
                    approved = isApproved,
                    active = isActive,
                    approvalStatus = approvalStatus,
                    verificationStatus = verificationStatus,
                    trustScore = trustScore,
                    rating = existingBiz?.rating ?: 5.0,
                    reviewCount = existingBiz?.reviewCount ?: 0,
                    latitude = defaultLat,
                    longitude = defaultLng
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

    fun businessToJson(b: Business): JSONObject {
        val o = JSONObject()
        o.put("id", b.id)
        o.put("ownerId", b.ownerId)
        o.put("name", b.name)
        o.put("category", b.category)
        o.put("description", b.description)
        o.put("address", b.address)
        o.put("contactNumber", b.contactNumber)
        o.put("openingHours", b.openingHours)
        if (!b.imageUrl.isNullOrBlank()) {
            o.put("imageUrl", b.imageUrl)
        }
        o.put("approved", b.approved)
        o.put("active", b.active)
        o.put("approvalStatus", b.approvalStatus)
        o.put("verificationStatus", b.verificationStatus)
        o.put("trustScore", b.trustScore)
        o.put("rating", b.rating ?: 0.0)
        o.put("reviewCount", b.reviewCount)
        if (!b.price.isNullOrBlank()) o.put("price", b.price)
        if (b.latitude != null) o.put("latitude", b.latitude)
        if (b.longitude != null) o.put("longitude", b.longitude)
        return o
    }

    fun jsonToBusiness(o: JSONObject): Business {
        val img = if (o.has("imageUrl") && !o.isNull("imageUrl")) o.optString("imageUrl").ifBlank { null } else null
        return Business(
            id = o.optString("id"),
            ownerId = o.optString("ownerId"),
            name = o.optString("name"),
            category = o.optString("category"),
            description = o.optString("description"),
            address = o.optString("address"),
            contactNumber = o.optString("contactNumber"),
            openingHours = o.optString("openingHours", "9:00 AM - 7:00 PM"),
            imageUrl = img,
            approved = o.optBoolean("approved", false),
            active = o.optBoolean("active", false),
            approvalStatus = o.optString("approvalStatus", "PENDING"),
            verificationStatus = o.optString("verificationStatus", "PENDING"),
            trustScore = o.optInt("trustScore", 0),
            rating = o.optDouble("rating", 0.0),
            reviewCount = o.optInt("reviewCount", 0),
            price = if (o.has("price") && !o.isNull("price")) o.optString("price").ifBlank { null } else null,
            latitude = if (o.has("latitude") && !o.isNull("latitude")) o.optDouble("latitude") else null,
            longitude = if (o.has("longitude") && !o.isNull("longitude")) o.optDouble("longitude") else null
        )
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
            array.put(businessToJson(b))
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
            list.add(jsonToBusiness(o))
        }
        return list
    }

    /** Update business approval status across session & stored businesses */
    fun updateBusinessApprovalStatus(
        businessId: String,
        approved: Boolean,
        active: Boolean,
        approvalStatus: String,
        verificationStatus: String,
        name: String = "",
        category: String = "",
        address: String = "",
        ownerId: String = "",
        contactNumber: String = ""
    ) {
        if (!::prefs.isInitialized) return
        val currentList = getAllBusinesses().toMutableList()
        val index = currentList.indexOfFirst { it.id == businessId }
        if (index >= 0) {
            val hash = Math.abs(businessId.hashCode())
            val defaultLat = currentList[index].latitude ?: (6.9271 + ((hash % 100) - 50) * 0.0003)
            val defaultLng = currentList[index].longitude ?: (79.8612 + (((hash / 100) % 100) - 50) * 0.0003)
            currentList[index] = currentList[index].copy(
                approved = approved,
                active = active,
                approvalStatus = approvalStatus,
                verificationStatus = verificationStatus,
                name = if (name.isNotBlank()) name else currentList[index].name,
                category = if (category.isNotBlank()) category else currentList[index].category,
                address = if (address.isNotBlank()) address else currentList[index].address,
                ownerId = if (ownerId.isNotBlank()) ownerId else currentList[index].ownerId,
                contactNumber = if (contactNumber.isNotBlank()) contactNumber else currentList[index].contactNumber,
                latitude = defaultLat,
                longitude = defaultLng
            )
        } else {
            val hash = Math.abs(businessId.hashCode())
            val defaultLat = 6.9271 + ((hash % 100) - 50) * 0.0003
            val defaultLng = 79.8612 + (((hash / 100) % 100) - 50) * 0.0003
            val existing = currentList.firstOrNull { it.id == businessId || (name.isNotBlank() && it.name.trim().equals(name.trim(), ignoreCase = true)) }
            currentList.add(
                0,
                Business(
                    id = businessId,
                    ownerId = ownerId,
                    name = name.ifBlank { "Approved Business" },
                    category = category.ifBlank { "Services" },
                    address = address.ifBlank { "Colombo, Sri Lanka" },
                    contactNumber = contactNumber,
                    approved = approved,
                    active = active,
                    approvalStatus = approvalStatus,
                    verificationStatus = verificationStatus,
                    trustScore = existing?.trustScore ?: 0,
                    rating = existing?.rating ?: 0.0,
                    reviewCount = existing?.reviewCount ?: 0,
                    latitude = defaultLat,
                    longitude = defaultLng
                )
            )
        }
        val array = JSONArray()
        for (b in currentList) {
            array.put(businessToJson(b))
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

        // Also update registered account credentials
        updateRegisteredAccountBusiness(
            businessId,
            mapOf(
                "approved" to approved,
                "active" to active,
                "approvalStatus" to approvalStatus,
                "verificationStatus" to verificationStatus
            )
        )
    }

    /** Update business trust score across session, stored businesses, and registered accounts */
    fun updateBusinessTrustScore(businessId: String, trustScore: Int) {
        if (!::prefs.isInitialized) return
        val currentBusinesses = getAllBusinesses()
        val targetName = currentBusinesses.firstOrNull { it.id == businessId }?.name?.trim()?.lowercase()
            ?: (getProfile()?.get("businessName") as? String)?.trim()?.lowercase()

        val businesses = currentBusinesses.map {
            val matchesId = it.id == businessId
            val matchesName = !targetName.isNullOrBlank() && it.name.trim().lowercase() == targetName
            if (matchesId || matchesName) it.copy(trustScore = trustScore) else it
        }
        val array = JSONArray()
        for (b in businesses) {
            array.put(businessToJson(b))
        }
        prefs.edit().putString(KEY_BUSINESSES, array.toString()).apply()

        val activeProfile = getProfile()
        if (activeProfile != null) {
            val currentBizId = activeProfile["businessId"] as? String
            val profName = (activeProfile["businessName"] as? String)?.trim()?.lowercase()
            if (currentBizId == businessId || (!targetName.isNullOrBlank() && profName == targetName)) {
                val updated = activeProfile.toMutableMap()
                updated["trustScore"] = trustScore
                saveProfile(updated)
            }
        }

        updateRegisteredAccountBusiness(businessId, mapOf("trustScore" to trustScore))
    }

    /** Update fields in registered accounts for a given businessId or matching email */
    fun updateRegisteredAccountBusiness(businessId: String, updates: Map<String, Any>) {
        if (!::prefs.isInitialized) return
        val accountsJson = prefs.getString(KEY_REGISTERED_ACCOUNTS, "{}") ?: "{}"
        val accountsObj = JSONObject(accountsJson)
        val keys = accountsObj.keys()
        var changed = false
        while (keys.hasNext()) {
            val emailKey = keys.next()
            val acc = accountsObj.getJSONObject(emailKey)
            val bizId = acc.optString("businessId")
            if (bizId == businessId || (businessId == "biz_hairloft" && emailKey.startsWith("hairloft"))) {
                for ((k, v) in updates) {
                    acc.put(k, v)
                }
                accountsObj.put(emailKey, acc)
                changed = true
            }
        }
        if (changed) {
            prefs.edit().putString(KEY_REGISTERED_ACCOUNTS, accountsObj.toString()).apply()
        }
    }

    /** Save or update a qualification document locally */
    fun saveQualificationDocument(doc: QualificationDocument) {
        if (!::prefs.isInitialized) return
        val current = getAllQualificationDocuments().toMutableList()
        val index = current.indexOfFirst { it.id == doc.id }
        if (index >= 0) {
            current[index] = doc
        } else {
            current.add(0, doc)
        }
        val array = JSONArray()
        for (d in current) {
            val o = JSONObject()
            o.put("id", d.id)
            o.put("ownerId", d.ownerId)
            o.put("businessId", d.businessId)
            o.put("businessName", d.businessName)
            o.put("title", d.title)
            o.put("issuingOrg", d.issuingOrg)
            o.put("experience", d.experience)
            o.put("documentType", d.documentType)
            o.put("storagePath", d.storagePath)
            o.put("fileUrl", d.fileUrl)
            o.put("verificationStatus", d.verificationStatus)
            o.put("rejectionReason", d.rejectionReason)
            array.put(o)
        }
        prefs.edit().putString(KEY_QUALIFICATIONS, array.toString()).apply()
    }

    /** Retrieve all locally stored qualification documents */
    fun getAllQualificationDocuments(): List<QualificationDocument> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(KEY_QUALIFICATIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = mutableListOf<QualificationDocument>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                QualificationDocument(
                    id = o.optString("id"),
                    ownerId = o.optString("ownerId"),
                    businessId = o.optString("businessId"),
                    businessName = o.optString("businessName"),
                    title = o.optString("title"),
                    issuingOrg = o.optString("issuingOrg"),
                    experience = o.optString("experience"),
                    documentType = o.optString("documentType", "CERTIFICATE"),
                    storagePath = o.optString("storagePath"),
                    fileUrl = o.optString("fileUrl"),
                    verificationStatus = o.optString("verificationStatus", "PENDING"),
                    rejectionReason = o.optString("rejectionReason")
                )
            )
        }
        return list
    }

    /** Save all qualification documents to SharedPreferences */
    fun saveAllQualificationDocuments(docs: List<QualificationDocument>) {
        if (!::prefs.isInitialized) return
        val array = JSONArray()
        for (d in docs) {
            val o = JSONObject()
            o.put("id", d.id)
            o.put("ownerId", d.ownerId)
            o.put("businessId", d.businessId)
            o.put("businessName", d.businessName)
            o.put("title", d.title)
            o.put("issuingOrg", d.issuingOrg)
            o.put("experience", d.experience)
            o.put("documentType", d.documentType)
            o.put("storagePath", d.storagePath)
            o.put("fileUrl", d.fileUrl)
            o.put("verificationStatus", d.verificationStatus)
            o.put("rejectionReason", d.rejectionReason)
            array.put(o)
        }
        prefs.edit().putString(KEY_QUALIFICATIONS, array.toString()).apply()
    }

    /** Update status of a qualification document locally */
    fun updateQualificationStatus(docId: String, status: String, reason: String = "") {
        if (!::prefs.isInitialized) return
        val docs = getAllQualificationDocuments().map {
            if (it.id == docId) {
                it.copy(verificationStatus = status, rejectionReason = reason)
            } else it
        }
        saveAllQualificationDocuments(docs)
    }

    /** Retrieve qualification documents for a specific business or owner */
    fun getQualificationsForBusiness(businessId: String, ownerId: String = ""): List<QualificationDocument> {
        val all = getAllQualificationDocuments()
        return all.filter {
            (businessId.isNotBlank() && it.businessId == businessId) ||
            (ownerId.isNotBlank() && it.ownerId == ownerId)
        }
    }

    /** Save an administrative audit log locally */
    fun saveAuditLog(log: AdminAuditLog) {
        if (!::prefs.isInitialized) return
        val current = getAllAuditLogs().toMutableList()
        current.removeAll { it.id == log.id }
        current.add(0, log)
        val array = JSONArray()
        for (l in current.take(50)) {
            val o = JSONObject()
            o.put("id", l.id)
            o.put("adminUid", l.adminUid)
            o.put("adminEmail", l.adminEmail)
            o.put("actionType", l.actionType)
            o.put("targetType", l.targetType)
            o.put("targetId", l.targetId)
            o.put("previousStatus", l.previousStatus)
            o.put("newStatus", l.newStatus)
            o.put("reason", l.reason)
            o.put("timestamp", l.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            array.put(o)
        }
        prefs.edit().putString(KEY_AUDIT_LOGS, array.toString()).apply()
    }

    /** Retrieve all saved administrative audit logs */
    fun getAllAuditLogs(): List<AdminAuditLog> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(KEY_AUDIT_LOGS, null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = mutableListOf<AdminAuditLog>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val timeMs = o.optLong("timestamp", System.currentTimeMillis())
            list.add(
                AdminAuditLog(
                    id = o.optString("id"),
                    adminUid = o.optString("adminUid"),
                    adminEmail = o.optString("adminEmail"),
                    actionType = o.optString("actionType"),
                    targetType = o.optString("targetType"),
                    targetId = o.optString("targetId"),
                    previousStatus = o.optString("previousStatus"),
                    newStatus = o.optString("newStatus"),
                    reason = o.optString("reason"),
                    createdAt = com.google.firebase.Timestamp(java.util.Date(timeMs))
                )
            )
        }
        return list
    }

    /** Save or update a service locally */
    fun saveService(service: Service) {
        if (!::prefs.isInitialized) return
        val current = getAllServices().toMutableList()
        val index = current.indexOfFirst { it.id == service.id }
        if (index >= 0) {
            current[index] = service
        } else {
            current.add(0, service)
        }
        val array = JSONArray()
        for (s in current) {
            val o = JSONObject()
            o.put("id", s.id)
            o.put("businessId", s.businessId)
            o.put("name", s.name)
            o.put("description", s.description)
            o.put("price", s.price)
            o.put("durationMinutes", s.durationMinutes)
            o.put("isActive", s.isActive)
            array.put(o)
        }
        prefs.edit().putString(KEY_SERVICES, array.toString()).apply()
    }

    /** Retrieve all locally stored services */
    fun getAllServices(): List<Service> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(KEY_SERVICES, null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = mutableListOf<Service>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                Service(
                    id = o.optString("id"),
                    businessId = o.optString("businessId"),
                    name = o.optString("name"),
                    description = o.optString("description"),
                    price = o.optDouble("price", 0.0),
                    durationMinutes = o.optInt("durationMinutes", 30),
                    isActive = o.optBoolean("isActive", true)
                )
            )
        }
        return list
    }

    /** Retrieve services for a specific business */
    fun getServicesForBusiness(businessId: String): List<Service> {
        val all = getAllServices()
        return if (businessId.isBlank()) all else all.filter { it.businessId == businessId }
    }

    /** Delete a service locally */
    fun deleteService(serviceId: String) {
        if (!::prefs.isInitialized) return
        val current = getAllServices().filter { it.id != serviceId }
        val array = JSONArray()
        for (s in current) {
            val o = JSONObject()
            o.put("id", s.id)
            o.put("businessId", s.businessId)
            o.put("name", s.name)
            o.put("description", s.description)
            o.put("price", s.price)
            o.put("durationMinutes", s.durationMinutes)
            o.put("isActive", s.isActive)
            array.put(o)
        }
        prefs.edit().putString(KEY_SERVICES, array.toString()).apply()
    }

    /** Save or update a promotion locally */
    fun savePromotion(promo: Promotion) {
        if (!::prefs.isInitialized) return
        val current = getAllPromotions().toMutableList()
        val index = current.indexOfFirst { it.id == promo.id }
        if (index >= 0) {
            current[index] = promo
        } else {
            current.add(0, promo)
        }
        val array = JSONArray()
        for (p in current) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("businessId", p.businessId)
            o.put("businessName", p.businessName)
            o.put("title", p.title)
            o.put("description", p.description)
            o.put("discountText", p.discountText)
            o.put("imageUrl", p.imageUrl)
            o.put("status", p.status)
            array.put(o)
        }
        prefs.edit().putString(KEY_PROMOTIONS, array.toString()).apply()
    }

    /** Retrieve all locally stored promotions */
    fun getAllPromotions(): List<Promotion> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(KEY_PROMOTIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = mutableListOf<Promotion>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                Promotion(
                    id = o.optString("id"),
                    businessId = o.optString("businessId"),
                    businessName = o.optString("businessName"),
                    title = o.optString("title"),
                    description = o.optString("description"),
                    discountText = o.optString("discountText"),
                    imageUrl = o.optString("imageUrl"),
                    status = o.optString("status", "ACTIVE")
                )
            )
        }
        return list
    }

    /** Retrieve promotions for a specific business */
    fun getPromotionsForBusiness(businessId: String): List<Promotion> {
        val all = getAllPromotions()
        return if (businessId.isBlank()) all else all.filter { it.businessId == businessId }
    }

    /** Save or update a notification locally */
    fun saveNotification(item: NotificationItem) {
        if (!::prefs.isInitialized) return
        val current = getAllNotifications().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        val array = JSONArray()
        for (n in current) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("userId", n.userId)
            o.put("title", n.title)
            o.put("message", n.message)
            o.put("category", n.category)
            o.put("relatedBookingId", n.relatedBookingId)
            o.put("relatedBusinessId", n.relatedBusinessId)
            o.put("isRead", n.isRead)
            array.put(o)
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, array.toString()).apply()
    }

    /** Retrieve all locally stored notifications */
    fun getAllNotifications(): List<NotificationItem> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = mutableListOf<NotificationItem>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                NotificationItem(
                    id = o.optString("id"),
                    userId = o.optString("userId"),
                    title = o.optString("title"),
                    message = o.optString("message"),
                    category = o.optString("category", "BOOKING"),
                    relatedBookingId = o.optString("relatedBookingId"),
                    relatedBusinessId = o.optString("relatedBusinessId"),
                    isRead = o.optBoolean("isRead", false)
                )
            )
        }
        return list
    }

    /** Mark a single notification as read locally */
    fun markNotificationRead(id: String) {
        if (!::prefs.isInitialized) return
        val current = getAllNotifications().map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        val array = JSONArray()
        for (n in current) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("userId", n.userId)
            o.put("title", n.title)
            o.put("message", n.message)
            o.put("category", n.category)
            o.put("relatedBookingId", n.relatedBookingId)
            o.put("relatedBusinessId", n.relatedBusinessId)
            o.put("isRead", n.isRead)
            array.put(o)
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, array.toString()).apply()
    }

    /** Mark all notifications for a user as read locally */
    fun markAllNotificationsRead(userId: String) {
        if (!::prefs.isInitialized) return
        val current = getAllNotifications().map {
            if (it.userId == userId) it.copy(isRead = true) else it
        }
        val array = JSONArray()
        for (n in current) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("userId", n.userId)
            o.put("title", n.title)
            o.put("message", n.message)
            o.put("category", n.category)
            o.put("relatedBookingId", n.relatedBookingId)
            o.put("relatedBusinessId", n.relatedBusinessId)
            o.put("isRead", n.isRead)
            array.put(o)
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, array.toString()).apply()
    }

    /** Save or update a booking in local cache */
    fun saveBooking(booking: Booking) {
        if (!::prefs.isInitialized) return
        val current = getAllBookings().toMutableList()
        val index = current.indexOfFirst { it.id == booking.id || (it.qrReference.isNotBlank() && it.qrReference == booking.qrReference) }
        if (index >= 0) {
            current[index] = booking
        } else {
            current.add(0, booking)
        }
        val array = JSONArray()
        for (b in current) {
            val o = JSONObject()
            o.put("id", b.id)
            o.put("customerId", b.customerId)
            o.put("businessId", b.businessId)
            o.put("businessNameSnapshot", b.businessNameSnapshot)
            o.put("businessAddressSnapshot", b.businessAddressSnapshot)
            o.put("serviceId", b.serviceId)
            o.put("serviceNameSnapshot", b.serviceNameSnapshot)
            o.put("appointmentDate", b.appointmentDate)
            o.put("appointmentTime", b.appointmentTime)
            o.put("priceAtBooking", b.priceAtBooking)
            o.put("durationMinutes", b.durationMinutes)
            o.put("status", b.status)
            o.put("customerNotes", b.customerNotes)
            o.put("qrReference", b.qrReference)
            o.put("cancellationReason", b.cancellationReason)
            array.put(o)
        }
        prefs.edit().putString(KEY_BOOKINGS, array.toString()).apply()
    }

    /** Retrieve all stored bookings */
    fun getAllBookings(): List<Booking> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(KEY_BOOKINGS, null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = mutableListOf<Booking>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                Booking(
                    id = o.optString("id"),
                    customerId = o.optString("customerId"),
                    businessId = o.optString("businessId"),
                    businessNameSnapshot = o.optString("businessNameSnapshot"),
                    businessAddressSnapshot = o.optString("businessAddressSnapshot"),
                    serviceId = o.optString("serviceId"),
                    serviceNameSnapshot = o.optString("serviceNameSnapshot"),
                    appointmentDate = o.optString("appointmentDate"),
                    appointmentTime = o.optString("appointmentTime"),
                    priceAtBooking = o.optDouble("priceAtBooking", 0.0),
                    durationMinutes = o.optInt("durationMinutes", 30),
                    status = o.optString("status", "PENDING"),
                    customerNotes = o.optString("customerNotes"),
                    qrReference = o.optString("qrReference"),
                    cancellationReason = o.optString("cancellationReason")
                )
            )
        }
        return list
    }

    /** Update booking status locally */
    fun updateBookingStatus(bookingIdOrRef: String, newStatus: String, reason: String = "") {
        if (!::prefs.isInitialized) return
        val current = getAllBookings().toMutableList()
        val index = current.indexOfFirst { it.id == bookingIdOrRef || it.qrReference.equals(bookingIdOrRef, ignoreCase = true) }
        if (index >= 0) {
            current[index] = current[index].copy(status = newStatus, cancellationReason = reason)
            val array = JSONArray()
            for (b in current) {
                val o = JSONObject()
                o.put("id", b.id)
                o.put("customerId", b.customerId)
                o.put("businessId", b.businessId)
                o.put("businessNameSnapshot", b.businessNameSnapshot)
                o.put("businessAddressSnapshot", b.businessAddressSnapshot)
                o.put("serviceId", b.serviceId)
                o.put("serviceNameSnapshot", b.serviceNameSnapshot)
                o.put("appointmentDate", b.appointmentDate)
                o.put("appointmentTime", b.appointmentTime)
                o.put("priceAtBooking", b.priceAtBooking)
                o.put("durationMinutes", b.durationMinutes)
                o.put("status", b.status)
                o.put("customerNotes", b.customerNotes)
                o.put("qrReference", b.qrReference)
                o.put("cancellationReason", b.cancellationReason)
                array.put(o)
            }
            prefs.edit().putString(KEY_BOOKINGS, array.toString()).apply()
        }
    }

    /** Clear stored session data – on logout, preserves registered accounts. */
    fun clear() {
        if (!::prefs.isInitialized) return
        prefs.edit().remove(KEY_PROFILE).apply()
    }
}
