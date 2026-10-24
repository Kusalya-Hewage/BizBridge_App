package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp

data class Review(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val businessId: String = "",
    val bookingId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val moderationStatus: String = "APPROVED",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "customerId" to customerId,
        "customerName" to customerName,
        "businessId" to businessId,
        "bookingId" to bookingId,
        "rating" to rating,
        "comment" to comment,
        "moderationStatus" to moderationStatus,
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}
