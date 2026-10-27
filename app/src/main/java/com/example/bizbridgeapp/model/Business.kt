package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

/**
 * Data class representing a business listing stored in Firestore.
 * Supports Customer, Business Owner, and Admin views with real-time sync.
 */
data class Business(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val address: String = "",
    val contactNumber: String = "",
    val openingHours: String = "9:00 AM - 7:00 PM",
    val imageUrl: String? = null,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val price: String? = null,
    val approved: Boolean = false,
    val active: Boolean = false,
    val approvalStatus: String = "PENDING", // PENDING, APPROVED, REJECTED
    val verificationStatus: String = "PENDING", // PENDING, VERIFIED, REJECTED
    val trustScore: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerId" to ownerId,
        "name" to name,
        "category" to category,
        "description" to description,
        "address" to address,
        "contactNumber" to contactNumber,
        "openingHours" to openingHours,
        "imageUrl" to imageUrl,
        "rating" to rating,
        "reviewCount" to reviewCount,
        "price" to price,
        "approved" to approved,
        "active" to active,
        "approvalStatus" to approvalStatus,
        "verificationStatus" to verificationStatus,
        "trustScore" to trustScore,
        "latitude" to latitude,
        "longitude" to longitude,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
