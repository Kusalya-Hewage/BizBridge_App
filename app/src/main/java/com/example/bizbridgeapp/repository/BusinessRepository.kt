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

    val defaultDemoBusinesses = listOf(
        Business(
            id = "biz_juniper_studio",
            name = "Juniper Studio",
            category = "Hair salon",
            description = "Premium haircare, modern styling, and specialized organic treatments by award-winning stylists.",
            address = "42 Flower Road, Colombo 07",
            rating = 4.9,
            reviewCount = 124,
            price = "From $35",
            approved = true,
            active = true,
            latitude = 6.9034,
            longitude = 79.8601
        ),
        Business(
            id = "biz_oliver_beauty",
            name = "Oliver Beauty Bar",
            category = "Beauty & Spa",
            description = "Full service salon offering luxury manicures, facials, skincare, and bridal treatments.",
            address = "18 Havelock Road, Colombo 05",
            rating = 4.7,
            reviewCount = 86,
            price = "From $28",
            approved = true,
            active = true,
            latitude = 6.8850,
            longitude = 79.8650
        ),
        Business(
            id = "biz_daily_cut",
            name = "The Daily Cut Barber",
            category = "Hair salon",
            description = "Classic and contemporary gentleman's grooming, beard styling, and hot towel treatments.",
            address = "75 Galle Road, Colombo 03",
            rating = 4.8,
            reviewCount = 95,
            price = "From $25",
            approved = true,
            active = true,
            latitude = 6.9120,
            longitude = 79.8510
        ),
        Business(
            id = "biz_lotus_wellness",
            name = "Lotus Wellness & Spa",
            category = "Wellness",
            description = "Holistic herbal treatments, deep tissue massage, aromatherapy, and mindfulness sessions.",
            address = "12 Rosmead Place, Colombo 07",
            rating = 4.9,
            reviewCount = 68,
            price = "From $45",
            approved = true,
            active = true,
            latitude = 6.9150,
            longitude = 79.8700
        )
    )

    /** Seed initial demo businesses - disabled so real data is used */
    fun seedDefaultBusinessesIfEmpty(onComplete: (List<Business>) -> Unit = {}) {
        onComplete(emptyList())
    }

    /** Fetch all approved and active businesses from Firestore */
    fun getApprovedActiveBusinesses(
        onSuccess: (List<Business>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION)
            .whereEqualTo("approved", true)
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Business::class.java)?.copy(id = doc.id)
                }
                onSuccess(list)
            }
            .addOnFailureListener { e ->
                onSuccess(emptyList())
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
        db.collection(COLLECTION).document(businessId).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val bus = snap.toObject(Business::class.java)?.copy(id = snap.id)
                    onSuccess(bus)
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
                                description = "Welcome to $savedName!",
                                approvalStatus = (sessionProfile["approvalStatus"] as? String) ?: "PENDING",
                                verificationStatus = (sessionProfile["verificationStatus"] as? String) ?: "PENDING",
                                approved = (sessionProfile["approved"] as? Boolean) ?: false,
                                active = (sessionProfile["active"] as? Boolean) ?: false,
                                trustScore = 80
                            )
                        )
                    } else {
                        val demo = defaultDemoBusinesses.firstOrNull { it.id == businessId }
                        onSuccess(demo)
                    }
                }
            }
            .addOnFailureListener { e ->
                val sessionProfile = com.example.bizbridgeapp.UserSessionManager.getProfile()
                val savedName = sessionProfile?.get("businessName") as? String
                if (!savedName.isNullOrBlank()) {
                    onSuccess(
                        Business(
                            id = businessId,
                            ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                            name = savedName,
                            category = (sessionProfile["category"] as? String) ?: "Hair salon",
                            contactNumber = (sessionProfile["phone"] as? String) ?: ""
                        )
                    )
                } else {
                    onSuccess(defaultDemoBusinesses.firstOrNull { it.id == businessId })
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
            .addOnFailureListener { e -> onSuccess(defaultDemoBusinesses) }
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
                    ?: defaultDemoBusinesses.firstOrNull()
                onSuccess(biz)
            }
            .addOnFailureListener {
                onSuccess(defaultDemoBusinesses.firstOrNull())
            }
    }

    /** Determine the business ID for the current owner session */
    fun getOwnerBusinessId(): String {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val saved = com.example.bizbridgeapp.UserSessionManager.getProfile()?.get("businessId") as? String
        if (!saved.isNullOrBlank() && saved != "biz_juniper_studio") return saved
        if (!currentUid.isNullOrBlank()) return "biz_${currentUid.take(12)}"
        if (!saved.isNullOrBlank()) return saved
        return "biz_juniper_studio"
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
            "biz_${currentUid.take(12)}"
        } else {
            (sessionProfile?.get("businessId") as? String) ?: "biz_juniper_studio"
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
                                    trustScore = 80
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
                    onUpdate(defaultDemoBusinesses.firstOrNull { it.id == effectiveBizId } ?: defaultDemoBusinesses.first())
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
                "biz_juniper_studio"
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
            trustScore = existingBiz?.trustScore ?: (sessionProfile["trustScore"] as? Int) ?: 80,
            rating = existingBiz?.rating ?: 0.0,
            reviewCount = existingBiz?.reviewCount ?: 0
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

        val docRef = db.collection(COLLECTION).document(targetId)
        val updates = mutableMapOf<String, Any>(
            "id" to targetId,
            "name" to name,
            "category" to category,
            "description" to description,
            "address" to address,
            "contactNumber" to contactNumber,
            "openingHours" to openingHours,
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
        return db.collection(COLLECTION)
            .whereEqualTo("approved", true)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { it.toObject(Business::class.java)?.copy(id = it.id) }
                onUpdate(list)
            }
    }
}
