package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

/**
 * Data class representing a Moderation Report filed against a business, review, or user.
 */
data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val targetType: String = "BUSINESS", // BUSINESS, REVIEW, USER
    val targetId: String = "",
    val targetName: String = "",
    val reason: String = "",
    val description: String = "",
    val status: String = "OPEN", // OPEN, INVESTIGATING, RESOLVED, DISMISSED
    val createdAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null,
    val resolutionNotes: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "reporterId" to reporterId,
        "reporterName" to reporterName,
        "targetType" to targetType,
        "targetId" to targetId,
        "targetName" to targetName,
        "reason" to reason,
        "description" to description,
        "status" to status,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "resolvedAt" to resolvedAt,
        "resolutionNotes" to resolutionNotes
    )
}
