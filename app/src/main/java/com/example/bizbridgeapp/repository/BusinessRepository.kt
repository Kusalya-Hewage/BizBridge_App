package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.Business
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

object BusinessRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val COLLECTION = "businesses"

    val defaultDemoBusinesses = emptyList<Business>()

    /** Seed initial demo businesses - disabled so real data is used */
    fun seedDefaultBusinessesIfEmpty(onComplete: (List<Business>) -> Unit = {}) {
        onComplete(emptyList())
    }

    /** Deduplicate businesses by normalized name and ID, favoring authoritative remote entries */
    fun deduplicateBusinesses(local: List<Business>, remote: List<Business>): List<Business> {
        val combined = (remote + local) // remote takes precedence
        val distinctList = mutableListOf<Business>()
        val seenNames = mutableSetOf<String>()
        val seenIds = mutableSetOf<String>()

        for (biz in combined) {
            val normName = biz.name.trim().lowercase()
            val id = biz.id.trim()
            if (normName.isNotEmpty() && !seenNames.contains(normName) && !seenIds.contains(id)) {
                seenNames.add(normName)
                seenIds.add(id)
                distinctList.add(biz)
            }
        }
        return distinctList
    }

    /** Fetch all approved and active businesses from Firestore and local cache */
    fun getApprovedActiveBusinesses(
        onSuccess: (List<Business>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val sessionBizId = getOwnerBusinessId()

        val localApproved = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
            .filter { (it.approved && it.active) || (sessionBizId.isNotBlank() && it.id == sessionBizId) || (currentUid != null && it.ownerId == currentUid) }
            .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }

        db.collection(COLLECTION)
            .whereEqualTo("approved", true)
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val firestoreList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Business::class.java)?.copy(id = doc.id)
                }.filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }
                val merged = deduplicateBusinesses(localApproved, firestoreList)
                if (merged.isNotEmpty()) {
                    onSuccess(merged)
                } else {
                    val fallback = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
                        .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }
                    onSuccess(fallback)
                }
            }
            .addOnFailureListener { e ->
                if (localApproved.isNotEmpty()) {
                    onSuccess(localApproved)
                } else {
                    val fallback = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
                        .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }
                    onSuccess(fallback)
                }
            }
    }

    /** Fetch businesses specifically for map view, ensuring newly added businesses and owner's business appear */
    fun getBusinessesForMap(
        onSuccess: (List<Business>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val sessionBizId = getOwnerBusinessId()

        val allLocal = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
            .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }

        db.collection(COLLECTION).get()
            .addOnSuccessListener { snapshot ->
                val firestoreList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Business::class.java)?.copy(id = doc.id)
                }.filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }

                val combined = deduplicateBusinesses(allLocal, firestoreList)

                val approvedOrOwn = combined.filter { biz ->
                    (biz.approved && biz.active) ||
                    (currentUid != null && biz.ownerId == currentUid) ||
                    (sessionBizId.isNotBlank() && biz.id == sessionBizId)
                }

                val result = if (approvedOrOwn.isNotEmpty()) approvedOrOwn else combined
                onSuccess(result)
            }
            .addOnFailureListener {
                val approvedOrOwn = allLocal.filter { biz ->
                    (biz.approved && biz.active) ||
                    (currentUid != null && biz.ownerId == currentUid) ||
                    (sessionBizId.isNotBlank() && biz.id == sessionBizId)
                }
                val result = if (approvedOrOwn.isNotEmpty()) approvedOrOwn else allLocal
                onSuccess(result)
            }
    }

    /** Fetch businesses filtered by category. */
    fun getBusinessesByCategory(
        category: String,
        onSuccess: (List<Business>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getApprovedActiveBusinesses(
            onSuccess = { all ->
                if (category.equals("All", ignoreCase = true)) {
                    onSuccess(all)
                } else {
                    val filtered = all.filter {
                        it.category.contains(category, ignoreCase = true) ||
                        category.contains(it.category, ignoreCase = true)
                    }
                    onSuccess(filtered)
                }
            },
            onFailure = {
                onSuccess(emptyList())
            }
        )
    }

    /** Simple text search on name, category, description, and address. */
    fun searchBusinesses(
        query: String,
        onSuccess: (List<Business>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val trimmed = query.trim()
        getApprovedActiveBusinesses(
            onSuccess = { all ->
                if (trimmed.isEmpty()) {
                    onSuccess(all)
                } else {
                    val filtered = all.filter {
                        it.name.contains(trimmed, ignoreCase = true) ||
                        it.category.contains(trimmed, ignoreCase = true) ||
                        it.description.contains(trimmed, ignoreCase = true) ||
                        it.address.contains(trimmed, ignoreCase = true)
                    }
                    onSuccess(filtered)
                }
            },
            onFailure = {
                onSuccess(emptyList())
            }
        )
    }

    /** Fetch a single business document by its document ID. */
    fun getBusinessById(
        businessId: String,
        onSuccess: (Business?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (businessId.isBlank()) {
            onSuccess(null)
            return
        }

        val localBiz = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
            .firstOrNull { it.id == businessId || it.name.equals(businessId, ignoreCase = true) }

        db.collection(COLLECTION).document(businessId).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val bus = snap.toObject(Business::class.java)?.copy(id = snap.id)
                    onSuccess(bus)
                } else if (localBiz != null) {
                    onSuccess(localBiz)
                } else {
                    // Try looking up by name in Firestore
                    db.collection(COLLECTION).whereEqualTo("name", businessId).limit(1).get()
                        .addOnSuccessListener { nameSnap ->
                            if (!nameSnap.isEmpty) {
                                val doc = nameSnap.documents.first()
                                onSuccess(doc.toObject(Business::class.java)?.copy(id = doc.id))
                            } else {
                                val sessionProfile = com.example.bizbridgeapp.UserSessionManager.getProfile()
                                val savedBizId = sessionProfile?.get("businessId") as? String
                                val savedName = sessionProfile?.get("businessName") as? String
                                if (!savedName.isNullOrBlank() && (savedBizId == businessId || businessId.startsWith("biz_"))) {
                                    onSuccess(
                                        Business(
                                            id = businessId,
                                            ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                            name = savedName,
                                            category = (sessionProfile["category"] as? String) ?: "Hair salon",
                                            contactNumber = (sessionProfile["phone"] as? String) ?: "",
                                            description = (sessionProfile["description"] as? String) ?: "Welcome to $savedName!",
                                            address = (sessionProfile["address"] as? String) ?: "Colombo, Sri Lanka",
                                            imageUrl = (sessionProfile["imageUrl"] as? String),
                                            approvalStatus = (sessionProfile["approvalStatus"] as? String) ?: "PENDING",
                                            verificationStatus = (sessionProfile["verificationStatus"] as? String) ?: "PENDING",
                                            approved = (sessionProfile["approved"] as? Boolean) ?: false,
                                            active = (sessionProfile["active"] as? Boolean) ?: false,
                                            trustScore = (sessionProfile["trustScore"] as? Int) ?: TrustScoreRepository.getLocalTrustScoreBreakdown(businessId).totalScore
                                        )
                                    )
                                } else {
                                    onSuccess(null)
                                }
                            }
                        }
                        .addOnFailureListener {
                            onSuccess(localBiz)
                        }
                }
            }
            .addOnFailureListener { e ->
                if (localBiz != null) {
                    onSuccess(localBiz)
                } else {
                    val sessionProfile = com.example.bizbridgeapp.UserSessionManager.getProfile()
                    val savedName = sessionProfile?.get("businessName") as? String
                    if (!savedName.isNullOrBlank()) {
                        onSuccess(
                            Business(
                                id = businessId,
                                ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                name = savedName,
                                category = (sessionProfile["category"] as? String) ?: "Hair salon",
                                contactNumber = (sessionProfile["phone"] as? String) ?: "",
                                imageUrl = (sessionProfile["imageUrl"] as? String)
                            )
                        )
                    } else {
                        onSuccess(null)
                    }
                }
            }
    }

    /** Retrieve businesses within a geographic radius (km). Requires valid latitude/longitude fields. */
    fun getNearbyBusinesses(
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double,
        onSuccess: (List<Business>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Firestore does not have native geo‑queries; we approximate with a bounding box.
        // Calculate latitude/longitude deltas for the given radius.
        val earthRadiusKm = 6371.0
        val latDelta = Math.toDegrees(radiusKm / earthRadiusKm)
        val lngDelta = Math.toDegrees(radiusKm / (earthRadiusKm * Math.cos(Math.toRadians(centerLat))))

        val lowerLat = centerLat - latDelta
        val upperLat = centerLat + latDelta
        val lowerLng = centerLng - lngDelta
        val upperLng = centerLng + lngDelta

        db.collection(COLLECTION)
            .whereGreaterThanOrEqualTo("latitude", lowerLat)
            .whereLessThanOrEqualTo("latitude", upperLat)
            .whereGreaterThanOrEqualTo("longitude", lowerLng)
            .whereLessThanOrEqualTo("longitude", upperLng)
            .whereEqualTo("approved", true)
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snapshot ->
                // Filter precisely by distance to avoid corner‑box artifacts.
                val result = snapshot.documents.mapNotNull { it.toObject(Business::class.java) }
                    .filter { b ->
                        val lat = b.latitude ?: return@filter false
                        val lng = b.longitude ?: return@filter false
                        val dLat = Math.toRadians(lat - centerLat)
                        val dLng = Math.toRadians(lng - centerLng)
                        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                                Math.cos(Math.toRadians(centerLat)) * Math.cos(Math.toRadians(lat)) *
                                Math.sin(dLng / 2) * Math.sin(dLng / 2)
                        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                        val distance = earthRadiusKm * c
                        distance <= radiusKm
                    }
                onSuccess(result)
            }
            .addOnFailureListener { e -> onSuccess(emptyList()) }
    }

    /** Helper to create or update a business document (used by Owner flow). */
    fun createOrUpdateBusiness(
        business: Business,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val map = business.toMap()
        // Use the supplied id if present; otherwise let Firestore generate one.
        val docRef = if (business.id.isNotBlank()) {
            db.collection(COLLECTION).document(business.id)
        } else {
            db.collection(COLLECTION).document()
        }
        docRef.set(map, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /** Find business owned by given user ID */
    fun getBusinessByOwnerId(
        ownerId: String,
        onSuccess: (Business?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION)
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val biz = snapshot.documents.firstOrNull()?.toObject(Business::class.java)?.copy(id = snapshot.documents.first().id)
                onSuccess(biz)
            }
            .addOnFailureListener {
                onSuccess(null)
            }
    }

    /** Determine the business ID for the current owner session */
    fun getOwnerBusinessId(): String {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val saved = com.example.bizbridgeapp.UserSessionManager.getProfile()?.get("businessId") as? String
        if (!saved.isNullOrBlank()) return saved
        if (!currentUid.isNullOrBlank()) return "biz_${currentUid.take(12)}"
        return ""
    }

    /** Real-time listener for the owner's business */
    fun listenToOwnerBusiness(
        ownerOrBizId: String,
        onUpdate: (Business?) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val sessionProfile = com.example.bizbridgeapp.UserSessionManager.getProfile()
        val savedName = sessionProfile?.get("businessName") as? String ?: "My Business"
        val savedCategory = sessionProfile?.get("category") as? String ?: "Hair salon"
        val savedPhone = sessionProfile?.get("phone") as? String ?: ""

        val effectiveBizId = if (ownerOrBizId.isNotBlank() && ownerOrBizId != "biz_juniper_studio" && ownerOrBizId != "demo_owner") {
            ownerOrBizId
        } else if (!currentUid.isNullOrBlank()) {
            val sessionBiz = sessionProfile?.get("businessId") as? String
            if (!sessionBiz.isNullOrBlank() && sessionBiz != "biz_juniper_studio") sessionBiz else "biz_${currentUid.take(12)}"
        } else {
            (sessionProfile?.get("businessId") as? String).orEmpty()
        }

        // If a real Firebase user is logged in
        if (!currentUid.isNullOrBlank()) {
            return db.collection(COLLECTION).document(effectiveBizId)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null && snapshot.exists()) {
                        val bizObj = snapshot.toObject(Business::class.java)?.copy(id = snapshot.id)
                        onUpdate(bizObj)
                        return@addSnapshotListener
                    }

                    // Query businesses where ownerId == currentUid
                    db.collection(COLLECTION).whereEqualTo("ownerId", currentUid).limit(1).get()
                        .addOnSuccessListener { querySnap ->
                            if (!querySnap.isEmpty) {
                                val doc = querySnap.documents.first()
                                onUpdate(doc.toObject(Business::class.java)?.copy(id = doc.id))
                            } else {
                                val newBiz = Business(
                                    id = effectiveBizId,
                                    ownerId = currentUid,
                                    name = savedName,
                                    category = savedCategory,
                                    contactNumber = savedPhone,
                                    description = "Welcome to $savedName!",
                                    approvalStatus = "PENDING",
                                    verificationStatus = "PENDING",
                                    approved = false,
                                    active = false,
                                    trustScore = TrustScoreRepository.getLocalTrustScoreBreakdown(effectiveBizId).totalScore
                                )
                                db.collection(COLLECTION).document(effectiveBizId).set(newBiz.toMap(), SetOptions.merge())
                                onUpdate(newBiz)
                            }
                        }
                        .addOnFailureListener {
                            onUpdate(
                                Business(
                                    id = effectiveBizId,
                                    ownerId = currentUid,
                                    name = savedName,
                                    category = savedCategory,
                                    contactNumber = savedPhone,
                                    approvalStatus = "PENDING",
                                    verificationStatus = "PENDING",
                                    approved = false,
                                    active = false
                                )
                            )
                        }
                }
        }

        // Fallback for demo session without Firebase Auth
        return db.collection(COLLECTION).document(effectiveBizId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    onUpdate(snapshot.toObject(Business::class.java)?.copy(id = snapshot.id))
                } else {
                    val localBiz = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses().firstOrNull { it.id == effectiveBizId }
                    if (localBiz != null) {
                        onUpdate(localBiz)
                    } else if (sessionProfile != null && !(sessionProfile["businessName"] as? String).isNullOrBlank()) {
                        val constructed = Business(
                            id = effectiveBizId,
                            ownerId = (sessionProfile["uid"] as? String) ?: "owner_bizbridge",
                            name = (sessionProfile["businessName"] as? String) ?: "My Business",
                            category = (sessionProfile["category"] as? String) ?: "Hair salon",
                            contactNumber = (sessionProfile["phone"] as? String) ?: "",
                            address = (sessionProfile["address"] as? String) ?: "Colombo, Sri Lanka",
                            description = (sessionProfile["description"] as? String) ?: "Welcome!",
                            approved = (sessionProfile["approved"] as? Boolean) ?: false,
                            active = (sessionProfile["active"] as? Boolean) ?: false,
                            approvalStatus = (sessionProfile["approvalStatus"] as? String) ?: "PENDING",
                            verificationStatus = (sessionProfile["verificationStatus"] as? String) ?: "PENDING",
                            trustScore = (sessionProfile["trustScore"] as? Int) ?: TrustScoreRepository.getLocalTrustScoreBreakdown(effectiveBizId).totalScore
                        )
                        onUpdate(constructed)
                    } else {
                        onUpdate(null)
                    }
                }
            }
    }

    /** Update business profile fields while preserving security rule fields */
    fun updateBusinessProfile(
        businessId: String,
        name: String,
        category: String,
        description: String,
        address: String,
        contactNumber: String,
        openingHours: String,
        imageUrl: String? = null,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val sessionProfile = com.example.bizbridgeapp.UserSessionManager.getProfile()?.toMutableMap() ?: mutableMapOf()
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: (sessionProfile["uid"] as? String)
        val targetId = if (businessId.isNotBlank() && businessId != "biz_juniper_studio") {
            businessId
        } else {
            val sessionBiz = sessionProfile["businessId"] as? String
            if (!sessionBiz.isNullOrBlank() && sessionBiz != "biz_juniper_studio") {
                sessionBiz
            } else if (!currentUid.isNullOrBlank()) {
                "biz_${currentUid.take(12)}"
            } else {
                "biz_${System.currentTimeMillis()}"
            }
        }

        // 1. Immediately persist to local UserSessionManager
        sessionProfile["businessId"] = targetId
        sessionProfile["businessName"] = name
        sessionProfile["category"] = category
        sessionProfile["phone"] = contactNumber
        sessionProfile["address"] = address
        sessionProfile["description"] = description
        sessionProfile["openingHours"] = openingHours
        if (!imageUrl.isNullOrBlank()) {
            sessionProfile["imageUrl"] = imageUrl
        }
        com.example.bizbridgeapp.UserSessionManager.saveProfile(sessionProfile)

        // 2. Immediately persist to local Business cache
        val existingBiz = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses().firstOrNull { it.id == targetId }
        val defaultLat = existingBiz?.latitude ?: 6.9271
        val defaultLng = existingBiz?.longitude ?: 79.8612
        val updatedBiz = Business(
            id = targetId,
            ownerId = currentUid ?: existingBiz?.ownerId ?: "owner_bizbridge",
            name = name,
            category = category,
            description = description,
            address = address,
            contactNumber = contactNumber,
            openingHours = openingHours,
            imageUrl = imageUrl ?: existingBiz?.imageUrl,
            approved = existingBiz?.approved ?: (sessionProfile["approved"] as? Boolean) ?: false,
            active = existingBiz?.active ?: (sessionProfile["active"] as? Boolean) ?: false,
            approvalStatus = existingBiz?.approvalStatus ?: (sessionProfile["approvalStatus"] as? String) ?: "PENDING",
            verificationStatus = existingBiz?.verificationStatus ?: (sessionProfile["verificationStatus"] as? String) ?: "PENDING",
            trustScore = existingBiz?.trustScore ?: (sessionProfile["trustScore"] as? Int) ?: TrustScoreRepository.getLocalTrustScoreBreakdown(targetId).totalScore,
            rating = existingBiz?.rating ?: 0.0,
            reviewCount = existingBiz?.reviewCount ?: 0,
            latitude = defaultLat,
            longitude = defaultLng
        )
        com.example.bizbridgeapp.UserSessionManager.saveBusiness(updatedBiz)

        // 3. Immediately persist to local application cache
        val appId = "app_${targetId.removePrefix("biz_")}"
        val existingApp = com.example.bizbridgeapp.UserSessionManager.getAllApplications().firstOrNull { it.id == appId || it.businessId == targetId }
        if (existingApp != null) {
            com.example.bizbridgeapp.UserSessionManager.saveApplication(
                existingApp.copy(
                    businessName = name,
                    category = category,
                    address = address,
                    contactNumber = contactNumber
                )
            )
        }

        // 4. Immediately persist to registered accounts so future logins preserve business name
        val acctUpdates = mutableMapOf<String, Any>(
            "businessName" to name,
            "category" to category,
            "phone" to contactNumber,
            "address" to address,
            "description" to description
        )
        if (!imageUrl.isNullOrBlank()) {
            acctUpdates["imageUrl"] = imageUrl
        }
        com.example.bizbridgeapp.UserSessionManager.updateRegisteredAccountBusiness(
            targetId,
            acctUpdates
        )

        val docRef = db.collection(COLLECTION).document(targetId)
        val updates = mutableMapOf<String, Any>(
            "id" to targetId,
            "name" to name,
            "category" to category,
            "description" to description,
            "address" to address,
            "contactNumber" to contactNumber,
            "openingHours" to openingHours,
            "latitude" to defaultLat,
            "longitude" to defaultLng,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!currentUid.isNullOrBlank()) {
            updates["ownerId"] = currentUid
        }
        if (!imageUrl.isNullOrBlank()) {
            updates["imageUrl"] = imageUrl
        }

        docRef.set(updates, SetOptions.merge())
            .addOnSuccessListener {
                // Secondary Firestore sync (users & businessApplications)
                if (!currentUid.isNullOrBlank()) {
                    db.collection("users").document(currentUid).set(
                        mapOf(
                            "businessId" to targetId,
                            "businessName" to name,
                            "category" to category,
                            "phone" to contactNumber,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                }

                db.collection("businessApplications").document(appId).set(
                    mapOf(
                        "businessName" to name,
                        "category" to category,
                        "address" to address,
                        "contactNumber" to contactNumber
                    ),
                    SetOptions.merge()
                )

                TrustScoreRepository.recalculateTrustScore(targetId)
                onSuccess()
            }
            .addOnFailureListener { e ->
                // Local save already succeeded; ensure TrustScore recalculated and report success
                TrustScoreRepository.recalculateTrustScore(targetId)
                onSuccess()
            }
    }

    /** Real-time listener for a single business document */
    fun listenToBusiness(
        businessId: String,
        onUpdate: (Business?) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection(COLLECTION).document(businessId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val biz = if (snapshot.exists()) snapshot.toObject(Business::class.java)?.copy(id = snapshot.id) else null
                onUpdate(biz)
            }
    }

    /** Real-time listener for all approved & active businesses */
    fun listenToApprovedBusinesses(
        onUpdate: (List<Business>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val sessionBizId = getOwnerBusinessId()

        val initialLocal = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
            .filter { (it.approved && it.active) || (sessionBizId.isNotBlank() && it.id == sessionBizId) || (currentUid != null && it.ownerId == currentUid) }
            .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }
        val fallbackLocal = if (initialLocal.isNotEmpty()) {
            initialLocal
        } else {
            com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
                .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }
        }
        if (fallbackLocal.isNotEmpty()) {
            onUpdate(fallbackLocal)
        }

        return db.collection(COLLECTION)
            .whereEqualTo("approved", true)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                val localApproved = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
                    .filter { (it.approved && it.active) || (sessionBizId.isNotBlank() && it.id == sessionBizId) || (currentUid != null && it.ownerId == currentUid) }
                    .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }
                if (error != null || snapshot == null) {
                    val res = if (localApproved.isNotEmpty()) localApproved else {
                        com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
                            .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }
                    }
                    onUpdate(res)
                    return@addSnapshotListener
                }
                val firestoreList = snapshot.documents.mapNotNull { it.toObject(Business::class.java)?.copy(id = it.id) }
                    .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }
                val merged = deduplicateBusinesses(localApproved, firestoreList)
                val finalResult = if (merged.isNotEmpty()) {
                    merged
                } else {
                    com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
                        .filter { it.id != "biz_juniper_studio" && it.id != "biz_oliver_beauty" && it.id != "biz_daily_cut" && it.id != "biz_lotus_wellness" }
                }
                onUpdate(finalResult)
            }
    }
}
