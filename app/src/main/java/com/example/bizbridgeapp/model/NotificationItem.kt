package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp

data class NotificationItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "BOOKING",
    val relatedBookingId: String = "",
    val relatedBusinessId: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "title" to title,
        "message" to message,
        "category" to category,
        "relatedBookingId" to relatedBookingId,
        "relatedBusinessId" to relatedBusinessId,
        "isRead" to isRead,
        "createdAt" to (createdAt ?: com.google.firebase.firestore.FieldValue.serverTimestamp())
    )
}
