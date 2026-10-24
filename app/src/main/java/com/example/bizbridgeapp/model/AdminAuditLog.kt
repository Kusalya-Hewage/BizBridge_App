package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

/**
 * Data class representing an immutable audit log of administrative actions.
 */
data class AdminAuditLog(
    val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val actionType: String = "", // APPROVE_BUSINESS, REJECT_BUSINESS, VERIFY_QUALIFICATION, SUSPEND_USER, ACTIVATE_USER, MODERATE_REVIEW, RESOLVE_REPORT
    val targetType: String = "", // BUSINESS, APPLICATION, USER, REVIEW, REPORT
    val targetId: String = "",
    val previousStatus: String = "",
    val newStatus: String = "",
    val reason: String = "",
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "adminUid" to adminUid,
        "adminEmail" to adminEmail,
        "actionType" to actionType,
        "targetType" to targetType,
        "targetId" to targetId,
        "previousStatus" to previousStatus,
        "newStatus" to newStatus,
        "reason" to reason,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
    )
}
