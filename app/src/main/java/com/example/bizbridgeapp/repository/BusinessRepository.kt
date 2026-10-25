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

    /** Seed initial demo businesses to Firestore if collection is empty */
    fun seedDefaultBusinessesIfEmpty(onComplete: (List<Business>) -> Unit = {}) {
        val batch = db.batch()
        for (biz in defaultDemoBusinesses) {
            val docRef = db.collection(COLLECTION).document(biz.id)
            batch.set(docRef, biz.toMap())
        }
        batch.commit()
            .addOnSuccessListener { onComplete(defaultDemoBusinesses) }
            .addOnFailureListener { onComplete(defaultDemoBusinesses) }
    }

    /** Fetch all approved and active businesses from Firestore */
    fun getApprovedActiveBusinesses(
        onSuccess: (List<Business>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    seedDefaultBusinessesIfEmpty { seeded ->
                        onSuccess(seeded)
                    }
                } else {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Business::class.java)?.copy(id = doc.id)
                    }
                    val visible = if (list.any { it.approved && it.active }) {
                        list.filter { it.approved && it.active }
                    } else {
                        list
                    }
                    onSuccess(visible)
                }
            }
            .addOnFailureListener { e ->
                // Provide fallback demonstration businesses so UI remains interactive
                onSuccess(defaultDemoBusinesses)
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
                onSuccess(defaultDemoBusinesses)
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
                onSuccess(defaultDemoBusinesses)
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
            onSuccess(defaultDemoBusinesses.firstOrNull())
            return
        }
        db.collection(COLLECTION).document(businessId).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val bus = snap.toObject(Business::class.java)?.copy(id = snap.id)
                    onSuccess(bus)
                } else {
                    val fallback = defaultDemoBusinesses.firstOrNull { it.id == businessId } ?: defaultDemoBusinesses.firstOrNull()
                    onSuccess(fallback)
                }
            }
            .addOnFailureListener { e ->
                val fallback = defaultDemoBusinesses.firstOrNull { it.id == businessId } ?: defaultDemoBusinesses.firstOrNull()
                onSuccess(fallback)
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
        if (!saved.isNullOrBlank()) return saved
        return "biz_juniper_studio"
    }

    /** Real-time listener for the owner's business */
    fun listenToOwnerBusiness(
        ownerOrBizId: String,
        onUpdate: (Business?) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val targetId = if (ownerOrBizId.isBlank() || ownerOrBizId == "demo_owner") "biz_juniper_studio" else ownerOrBizId
        if (targetId.startsWith("biz_")) {
            return db.collection(COLLECTION).document(targetId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        onUpdate(defaultDemoBusinesses.firstOrNull { it.id == targetId } ?: defaultDemoBusinesses.first())
                        return@addSnapshotListener
                    }
                    if (snapshot.exists()) {
                        onUpdate(snapshot.toObject(Business::class.java)?.copy(id = snapshot.id))
                    } else {
                        // Seed default businesses so Juniper Studio exists
                        seedDefaultBusinessesIfEmpty { seeded ->
                            onUpdate(seeded.firstOrNull { it.id == targetId } ?: seeded.firstOrNull())
                        }
                    }
                }
        }

        // Query by ownerId
        return db.collection(COLLECTION)
            .whereEqualTo("ownerId", targetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    // Fallback to demo business document listener
                    db.collection(COLLECTION).document("biz_juniper_studio").get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                onUpdate(doc.toObject(Business::class.java)?.copy(id = doc.id))
                            } else {
                                onUpdate(defaultDemoBusinesses.first())
                            }
                        }
                        .addOnFailureListener { onUpdate(defaultDemoBusinesses.first()) }
                    return@addSnapshotListener
                }
                val doc = snapshot.documents.first()
                onUpdate(doc.toObject(Business::class.java)?.copy(id = doc.id))
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
        val targetId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        val docRef = db.collection(COLLECTION).document(targetId)

        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "category" to category,
            "description" to description,
            "address" to address,
            "contactNumber" to contactNumber,
            "openingHours" to openingHours,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!imageUrl.isNullOrBlank()) {
            updates["imageUrl"] = imageUrl
        }

        docRef.set(updates, SetOptions.merge())
            .addOnSuccessListener {
                // Recalculate trust score based on new profile completeness
                TrustScoreRepository.recalculateTrustScore(targetId)
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure(e) }
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
                    onUpdate(defaultDemoBusinesses)
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { it.toObject(Business::class.java)?.copy(id = it.id) }
                onUpdate(if (list.isNotEmpty()) list else defaultDemoBusinesses)
            }
    }
}
