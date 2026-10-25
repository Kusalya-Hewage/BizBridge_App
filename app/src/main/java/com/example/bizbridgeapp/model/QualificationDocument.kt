package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

/**
 * Data class representing a Qualification / Verification document submitted by a Business Owner.
 */
data class QualificationDocument(
    val id: String = "",
    val ownerId: String = "",
    val businessId: String = "",
    val businessName: String = "",
    val documentType: String = "CERTIFICATE", // CERTIFICATE, BUSINESS_REG, IDENTITY, LICENSE
    val title: String = "",
    val issuingOrg: String = "",
    val experience: String = "",
    val storagePath: String = "",
    val fileUrl: String = "",
    val verificationStatus: String = "PENDING", // PENDING, VERIFIED, REJECTED
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
        "documentType" to documentType,
        "title" to title,
        "issuingOrg" to issuingOrg,
        "experience" to experience,
        "storagePath" to storagePath,
        "fileUrl" to fileUrl,
        "verificationStatus" to verificationStatus,
        "submittedAt" to (submittedAt ?: FieldValue.serverTimestamp()),
        "reviewedAt" to reviewedAt,
        "reviewedBy" to reviewedBy,
        "rejectionReason" to rejectionReason
    )
}
