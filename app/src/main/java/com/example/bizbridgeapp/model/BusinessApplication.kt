package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

/**
 * Data class representing a Business Registration Application submitted by an owner for Admin approval.
 */
data class BusinessApplication(
    val id: String = "",
    val ownerId: String = "",
    val businessId: String = "",
    val businessName: String = "",
    val ownerName: String = "",
    val email: String = "",
    val category: String = "",
    val contactNumber: String = "",
    val address: String = "Colombo, Sri Lanka",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val submittedAt: Timestamp? = null,
    val reviewedAt: Timestamp? = null,
    val reviewedBy: String = "",
    val rejectionReason: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerId" to ownerId,
        "businessId" to businessId,
        "businessName" to businessName,
        "ownerName" to ownerName,
        "email" to email,
        "category" to category,
        "contactNumber" to contactNumber,
        "address" to address,
        "status" to status,
        "submittedAt" to (submittedAt ?: FieldValue.serverTimestamp()),
        "reviewedAt" to reviewedAt,
        "reviewedBy" to reviewedBy,
        "rejectionReason" to rejectionReason
    )
}
