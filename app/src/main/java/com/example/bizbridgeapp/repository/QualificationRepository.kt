package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.AdminAuditLog
import com.example.bizbridgeapp.model.NotificationItem
import com.example.bizbridgeapp.model.QualificationDocument
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object QualificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val QUALIFICATIONS = "qualificationVerifications"
    private const val AUDIT_LOGS = "adminAuditLogs"

    /** Submit a qualification document for verification */
    fun submitDocument(
        doc: QualificationDocument,
        onSuccess: (id: String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = if (doc.id.isNotEmpty()) db.collection(QUALIFICATIONS).document(doc.id) else db.collection(QUALIFICATIONS).document()
        val toSave = doc.copy(id = docRef.id)
        docRef.set(toSave.toMap())
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { onFailure(it) }
    }

    /** Listen to qualifications for a specific business */
    fun listenToQualificationsForBusiness(
        businessId: String,
        onUpdate: (List<QualificationDocument>) -> Unit
    ): ListenerRegistration {
        return db.collection(QUALIFICATIONS)
            .whereEqualTo("businessId", businessId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { it.toObject(QualificationDocument::class.java)?.copy(id = it.id) }
                onUpdate(list)
            }
    }

    /** Listen to pending qualifications for Admin */
    fun listenToPendingQualifications(onUpdate: (List<QualificationDocument>) -> Unit): ListenerRegistration {
        return db.collection(QUALIFICATIONS)
            .whereEqualTo("verificationStatus", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { it.toObject(QualificationDocument::class.java)?.copy(id = it.id) }
                onUpdate(list)
            }
    }

    /** Verify document by Admin */
    fun verifyDocument(
        docId: String,
        adminUid: String,
        adminEmail: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = db.collection(QUALIFICATIONS).document(docId)
        docRef.get().addOnSuccessListener { snap ->
            val doc = snap.toObject(QualificationDocument::class.java)?.copy(id = snap.id) ?: return@addOnSuccessListener
            val batch = db.batch()
            batch.update(docRef, mapOf(
                "verificationStatus" to "VERIFIED",
                "reviewedAt" to FieldValue.serverTimestamp(),
                "reviewedBy" to adminEmail
            ))

            val logRef = db.collection(AUDIT_LOGS).document()
            val log = AdminAuditLog(
                id = logRef.id,
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "VERIFY_QUALIFICATION",
                targetType = "DOCUMENT",
                targetId = docId,
                previousStatus = "PENDING",
                newStatus = "VERIFIED",
                reason = "Document verified by administrator"
            )
            batch.set(logRef, log.toMap())

            batch.commit().addOnSuccessListener {
                // Recalculate trust score
                TrustScoreRepository.recalculateTrustScore(doc.businessId)

                NotificationRepository.createNotification(
                    NotificationItem(
                        userId = doc.ownerId,
                        title = "Document Verified",
                        message = "Your document '${doc.title}' has been successfully verified.",
                        category = "SYSTEM",
                        relatedBusinessId = doc.businessId
                    ),
                    onSuccess = {},
                    onFailure = {}
                )
                onSuccess()
            }.addOnFailureListener { onFailure(it) }
        }.addOnFailureListener { onFailure(it) }
    }

    /** Reject document by Admin */
    fun rejectDocument(
        docId: String,
        adminUid: String,
        adminEmail: String,
        reason: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = db.collection(QUALIFICATIONS).document(docId)
        docRef.get().addOnSuccessListener { snap ->
            val doc = snap.toObject(QualificationDocument::class.java)?.copy(id = snap.id) ?: return@addOnSuccessListener
            val batch = db.batch()
            batch.update(docRef, mapOf(
                "verificationStatus" to "REJECTED",
                "rejectionReason" to reason,
                "reviewedAt" to FieldValue.serverTimestamp(),
                "reviewedBy" to adminEmail
            ))

            val logRef = db.collection(AUDIT_LOGS).document()
            val log = AdminAuditLog(
                id = logRef.id,
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "REJECT_QUALIFICATION",
                targetType = "DOCUMENT",
                targetId = docId,
                previousStatus = "PENDING",
                newStatus = "REJECTED",
                reason = reason
            )
            batch.set(logRef, log.toMap())

            batch.commit().addOnSuccessListener {
                NotificationRepository.createNotification(
                    NotificationItem(
                        userId = doc.ownerId,
                        title = "Document Rejected",
                        message = "Your document '${doc.title}' could not be verified: $reason",
                        category = "SYSTEM",
                        relatedBusinessId = doc.businessId
                    ),
                    onSuccess = {},
                    onFailure = {}
                )
                onSuccess()
            }.addOnFailureListener { onFailure(it) }
        }.addOnFailureListener { onFailure(it) }
    }
}
