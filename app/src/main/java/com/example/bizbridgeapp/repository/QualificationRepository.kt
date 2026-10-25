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
        val sessionProfile = com.example.bizbridgeapp.UserSessionManager.getProfile()
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: (sessionProfile?.get("uid") as? String)
        val sessionBizId = (sessionProfile?.get("businessId") as? String)
        val sessionBizName = (sessionProfile?.get("businessName") as? String) ?: ""

        val finalOwnerId = if (doc.ownerId.isNotBlank() && doc.ownerId != "owner_elena" && doc.ownerId != "owner_bizbridge") {
            doc.ownerId
        } else {
            currentUid ?: "owner_elena"
        }
        val finalBizId = if (doc.businessId.isNotBlank() && doc.businessId != "biz_juniper_studio") {
            doc.businessId
        } else if (!sessionBizId.isNullOrBlank()) {
            sessionBizId
        } else {
            BusinessRepository.getOwnerBusinessId()
        }

        val docRef = if (doc.id.isNotEmpty()) db.collection(QUALIFICATIONS).document(doc.id) else db.collection(QUALIFICATIONS).document()
        val toSave = doc.copy(
            id = docRef.id,
            ownerId = finalOwnerId,
            businessId = finalBizId,
            businessName = if (doc.businessName.isNotBlank()) doc.businessName else sessionBizName
        )

        // Always save locally immediately so the user never loses their submitted document
        com.example.bizbridgeapp.UserSessionManager.saveQualificationDocument(toSave)
        TrustScoreRepository.recalculateTrustScore(finalBizId)

        docRef.set(toSave.toMap())
            .addOnSuccessListener {
                com.example.bizbridgeapp.UserSessionManager.saveQualificationDocument(toSave)
                onSuccess(docRef.id)
            }
            .addOnFailureListener {
                // Succeeded locally
                com.example.bizbridgeapp.UserSessionManager.saveQualificationDocument(toSave)
                onSuccess(docRef.id)
            }
    }

    /** Retrieve a single qualification document by ID */
    fun getDocumentById(
        docId: String,
        onSuccess: (QualificationDocument?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (docId.isBlank()) {
            onSuccess(null)
            return
        }
        val local = com.example.bizbridgeapp.UserSessionManager.getAllQualificationDocuments().firstOrNull { it.id == docId }
        if (local != null) {
            onSuccess(local)
            return
        }
        db.collection(QUALIFICATIONS).document(docId).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val doc = snap.toObject(QualificationDocument::class.java)?.copy(id = snap.id)
                    onSuccess(doc)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    /** Fetch submitted qualification documents for a business or owner */
    fun getDocumentsForBusiness(
        businessId: String,
        ownerId: String = "",
        onSuccess: (List<QualificationDocument>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val localList = com.example.bizbridgeapp.UserSessionManager.getQualificationsForBusiness(businessId, ownerId)
        val query = if (businessId.isNotBlank() && businessId != "biz_juniper_studio") {
            db.collection(QUALIFICATIONS).whereEqualTo("businessId", businessId)
        } else if (ownerId.isNotBlank()) {
            db.collection(QUALIFICATIONS).whereEqualTo("ownerId", ownerId)
        } else {
            db.collection(QUALIFICATIONS).whereEqualTo("businessId", businessId)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val firestoreList = snapshot.documents.mapNotNull { it.toObject(QualificationDocument::class.java)?.copy(id = it.id) }
                val merged = (localList + firestoreList).distinctBy { it.id }
                onSuccess(merged)
            }
            .addOnFailureListener {
                onSuccess(localList)
            }
    }

    /** Listen to qualifications for a specific business */
    fun listenToQualificationsForBusiness(
        businessId: String,
        onUpdate: (List<QualificationDocument>) -> Unit
    ): ListenerRegistration {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: (com.example.bizbridgeapp.UserSessionManager.getProfile()?.get("uid") as? String)
        val targetBizId = if (businessId.isNotBlank() && businessId != "biz_juniper_studio") businessId else BusinessRepository.getOwnerBusinessId()

        val localList = com.example.bizbridgeapp.UserSessionManager.getQualificationsForBusiness(targetBizId, currentUid ?: "")
        if (localList.isNotEmpty()) {
            onUpdate(localList)
        }

        val query = if (!currentUid.isNullOrBlank()) {
            db.collection(QUALIFICATIONS).whereEqualTo("ownerId", currentUid)
        } else {
            db.collection(QUALIFICATIONS).whereEqualTo("businessId", targetBizId)
        }

        return query.addSnapshotListener { snapshot, error ->
            val localCurrent = com.example.bizbridgeapp.UserSessionManager.getQualificationsForBusiness(targetBizId, currentUid ?: "")
            if (error != null || snapshot == null) {
                onUpdate(localCurrent)
                return@addSnapshotListener
            }
            val firestoreList = snapshot.documents.mapNotNull { it.toObject(QualificationDocument::class.java)?.copy(id = it.id) }
            val merged = (localCurrent + firestoreList).distinctBy { it.id }
            onUpdate(merged)
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
