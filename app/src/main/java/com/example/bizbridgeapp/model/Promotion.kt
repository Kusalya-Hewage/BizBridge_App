package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

/**
 * Data class representing a Business Promotion / Deal.
 */
data class Promotion(
    val id: String = "",
    val businessId: String = "",
    val businessName: String = "",
    val title: String = "",
    val description: String = "",
    val discountText: String = "",
    val imageUrl: String = "",
    val status: String = "ACTIVE", // ACTIVE, EXPIRED, PAUSED
    val startAt: Timestamp? = null,
    val endAt: Timestamp? = null,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "businessId" to businessId,
        "businessName" to businessName,
        "title" to title,
        "description" to description,
        "discountText" to discountText,
        "imageUrl" to imageUrl,
        "status" to status,
        "startAt" to startAt,
        "endAt" to endAt,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
    )
}
