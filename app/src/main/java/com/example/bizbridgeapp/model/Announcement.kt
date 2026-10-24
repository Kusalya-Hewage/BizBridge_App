package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

/**
 * Data class representing a Platform Announcement published by Administrators.
 */
data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val audience: String = "ALL", // ALL, CUSTOMERS, OWNERS
    val status: String = "PUBLISHED", // DRAFT, PUBLISHED
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val publishedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "message" to message,
        "audience" to audience,
        "status" to status,
        "createdBy" to createdBy,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "publishedAt" to (publishedAt ?: FieldValue.serverTimestamp())
    )
}
