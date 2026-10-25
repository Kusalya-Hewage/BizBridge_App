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
        val localPending = com.example.bizbridgeapp.UserSessionManager.getAllQualificationDocuments()
            .filter { it.verificationStatus.equals("PENDING", ignoreCase = true) }
        if (localPending.isNotEmpty()) {
            onUpdate(localPending)
        }

        return db.collection(QUALIFICATIONS)
            .whereEqualTo("verificationStatus", "PENDING")
            .addSnapshotListener { snapshot, error ->
                val currentLocal = com.example.bizbridgeapp.UserSessionManager.getAllQualificationDocuments()
                    .filter { it.verificationStatus.equals("PENDING", ignoreCase = true) }
                if (error != null || snapshot == null) {
                    onUpdate(currentLocal)
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { it.toObject(QualificationDocument::class.java)?.copy(id = it.id) }
                val merged = (currentLocal + list).distinctBy { it.id }
                onUpdate(merged)
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
        // Update local status first
        com.example.bizbridgeapp.UserSessionManager.updateQualificationStatus(docId, "VERIFIED")

        val docRef = db.collection(QUALIFICATIONS).document(docId)
        docRef.get().addOnSuccessListener { snap ->
            val doc = if (snap.exists()) {
                snap.toObject(QualificationDocument::class.java)?.copy(id = snap.id)
            } else {
                com.example.bizbridgeapp.UserSessionManager.getAllQualificationDocuments().firstOrNull { it.id == docId }
            }

            if (doc != null) {
                docRef.set(
                    mapOf(
                        "id" to doc.id,
                        "ownerId" to doc.ownerId,
                        "businessId" to doc.businessId,
                        "businessName" to doc.businessName,
                        "title" to doc.title,
                        "issuingOrg" to doc.issuingOrg,
                        "experience" to doc.experience,
                        "documentType" to doc.documentType,
                        "storagePath" to doc.storagePath,
                        "fileUrl" to doc.fileUrl,
                        "verificationStatus" to "VERIFIED",
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "reviewedBy" to adminEmail
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

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
            }
            onSuccess()
        }.addOnFailureListener {
            onSuccess()
        }
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
        // Update local status first
        com.example.bizbridgeapp.UserSessionManager.updateQualificationStatus(docId, "REJECTED", reason)

        val docRef = db.collection(QUALIFICATIONS).document(docId)
        docRef.get().addOnSuccessListener { snap ->
            val doc = if (snap.exists()) {
                snap.toObject(QualificationDocument::class.java)?.copy(id = snap.id)
            } else {
                com.example.bizbridgeapp.UserSessionManager.getAllQualificationDocuments().firstOrNull { it.id == docId }
            }

            if (doc != null) {
                docRef.set(
                    mapOf(
                        "id" to doc.id,
                        "ownerId" to doc.ownerId,
                        "businessId" to doc.businessId,
                        "businessName" to doc.businessName,
                        "title" to doc.title,
                        "issuingOrg" to doc.issuingOrg,
                        "experience" to doc.experience,
                        "documentType" to doc.documentType,
                        "storagePath" to doc.storagePath,
                        "fileUrl" to doc.fileUrl,
                        "verificationStatus" to "REJECTED",
                        "rejectionReason" to reason,
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "reviewedBy" to adminEmail
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                NotificationRepository.createNotification(
                    NotificationItem(
                        userId = doc.ownerId,
                        title = "Document Needs Revision",
                        message = "Your document '${doc.title}' was rejected: $reason",
                        category = "SYSTEM",
                        relatedBusinessId = doc.businessId
                    ),
                    onSuccess = {},
                    onFailure = {}
                )
            }
            onSuccess()
        }.addOnFailureListener {
            onSuccess()
        }
    }
}

