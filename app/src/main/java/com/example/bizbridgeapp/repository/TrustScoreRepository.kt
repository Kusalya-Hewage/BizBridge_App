package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.UserSessionManager
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.QualificationDocument
import com.example.bizbridgeapp.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object TrustScoreRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Phase 10 Dynamic Trust Score Algorithm:
     * - Professional qualifications: max 30 (15 pts per verified professional cert, up to 30)
     * - Industry experience / Approval base: max 25 (25 pts once approved, 10 pts baseline if pending)
     * - Profile completeness: max 15 (description +4, address +4, contactNumber +4, openingHours +3)
     * - Verified documents: max 15 (business reg / identity verified = 15 pts)
     * - Eligible customer reviews: max 15 (based on average rating: 5.0 -> 15 pts, 4.0 -> 12 pts, min baseline 10)
     * Total: 100
     */
    fun recalculateTrustScore(businessId: String, onComplete: (Int) -> Unit = {}) {
        val targetId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val localBiz = UserSessionManager.getAllBusinesses().firstOrNull { it.id == targetId }
        val localVerifiedDocs = UserSessionManager.getAllQualificationDocuments()
            .filter { it.businessId == targetId && it.verificationStatus.equals("VERIFIED", ignoreCase = true) }

        fun computeAndPersist(biz: Business, verifiedDocs: List<QualificationDocument>) {
            // 1. Professional qualifications (max 30)
            val certCount = verifiedDocs.count {
                it.documentType.contains("CERT", ignoreCase = true) ||
                it.title.contains("cert", ignoreCase = true) ||
                it.title.contains("diploma", ignoreCase = true) ||
                it.title.contains("degree", ignoreCase = true) ||
                it.title.contains("license", ignoreCase = true)
            }
            val qualificationScore = (certCount * 15).coerceAtMost(30)

            // 2. Industry experience / Approval base (max 25)
            val isApproved = biz.approved || biz.approvalStatus.equals("APPROVED", ignoreCase = true)
            val experienceScore = if (isApproved) 25 else 10

            // 3. Profile completeness (max 15)
            var profileScore = 0
            if (biz.description.isNotBlank()) profileScore += 4
            if (biz.address.isNotBlank()) profileScore += 4
            if (biz.contactNumber.isNotBlank()) profileScore += 4
            if (biz.openingHours.isNotBlank()) profileScore += 3

            // 4. Verified documents (max 15)
            val hasRegDoc = verifiedDocs.any { !it.documentType.contains("CERT", ignoreCase = true) }
            val docScore = if (hasRegDoc || biz.verificationStatus.equals("VERIFIED", ignoreCase = true) || isApproved) 15 else 0

            // 5. Eligible customer reviews (max 15)
            val rating = biz.rating ?: 0.0
            val count = biz.reviewCount
            val reviewScore = if (count >= 1) {
                ((rating / 5.0) * 15.0).toInt().coerceIn(0, 15)
            } else {
                10 // Baseline neutral for new businesses
            }

            val totalScore = (qualificationScore + experienceScore + profileScore + docScore + reviewScore).coerceIn(0, 100)

            // 1. Always update local UserSessionManager immediately
            UserSessionManager.updateBusinessTrustScore(targetId, totalScore)

            // 2. Update Firestore
            val bizRef = db.collection("businesses").document(targetId)
            bizRef.set(mapOf("trustScore" to totalScore), SetOptions.merge())
                .addOnSuccessListener {
                    onComplete(totalScore)
                }
                .addOnFailureListener {
                    onComplete(totalScore)
                }
        }

        // Fetch remote document & remote verifications, merging with local state
        val bizRef = db.collection("businesses").document(targetId)
        bizRef.get().addOnSuccessListener { bizSnap ->
            val remoteBiz = if (bizSnap.exists()) bizSnap.toObject(Business::class.java)?.copy(id = bizSnap.id) else null
            val effectiveBiz = remoteBiz ?: localBiz ?: Business(
                id = targetId,
                name = (UserSessionManager.getProfile()?.get("businessName") as? String) ?: "Business",
                approved = false,
                approvalStatus = "PENDING",
                verificationStatus = "PENDING"
            )

            db.collection("qualificationVerifications")
                .whereEqualTo("businessId", targetId)
                .whereEqualTo("verificationStatus", "VERIFIED")
                .get()
                .addOnSuccessListener { docSnap ->
                    val remoteDocs = docSnap.documents.mapNotNull { it.toObject(QualificationDocument::class.java)?.copy(id = it.id) }
                    val mergedDocs = (localVerifiedDocs + remoteDocs).distinctBy { it.id }
                    computeAndPersist(effectiveBiz, mergedDocs)
                }
                .addOnFailureListener {
                    computeAndPersist(effectiveBiz, localVerifiedDocs)
                }
        }.addOnFailureListener {
            val effectiveBiz = localBiz ?: Business(
                id = targetId,
                name = (UserSessionManager.getProfile()?.get("businessName") as? String) ?: "Business",
                approved = false,
                approvalStatus = "PENDING",
                verificationStatus = "PENDING"
            )
            computeAndPersist(effectiveBiz, localVerifiedDocs)
        }
    }
}
