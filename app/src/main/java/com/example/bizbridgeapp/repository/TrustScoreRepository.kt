package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.UserSessionManager
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.QualificationDocument
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class TrustScoreBreakdown(
    val qualificationScore: Int,
    val maxQualificationScore: Int = 30,
    val experienceScore: Int,
    val maxExperienceScore: Int = 25,
    val profileScore: Int,
    val maxProfileScore: Int = 15,
    val docScore: Int,
    val maxDocScore: Int = 15,
    val reviewScore: Int,
    val maxReviewScore: Int = 15,
    val totalScore: Int,
    val trustBadge: String,
    val trustDescription: String
)

object TrustScoreRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Dynamic Trust Score Algorithm:
     * - Professional qualifications: max 30 (15 pts per verified professional cert; 10 pts for submitted doc pending review)
     * - Industry experience / Approval base: max 25 (25 pts once approved, 10 pts baseline if pending)
     * - Profile completeness: max 15 (description +4, address +4, contactNumber +4, openingHours +3)
     * - Verified documents: max 15 (business reg / identity verified = 15 pts; 5 pts if pending)
     * - Eligible customer reviews: max 15 (0 pts if no reviews yet; up to 15 pts based on rating)
     * Total: 100
     */
    fun computeBreakdown(biz: Business?, allDocs: List<QualificationDocument>): TrustScoreBreakdown {
        val b = biz ?: Business()

        // 1. Professional qualifications (max 30)
        val certDocs = allDocs.filter {
            it.documentType.contains("CERT", ignoreCase = true) ||
            it.title.contains("cert", ignoreCase = true) ||
            it.title.contains("diploma", ignoreCase = true) ||
            it.title.contains("degree", ignoreCase = true) ||
            it.title.contains("license", ignoreCase = true) ||
            it.documentType.isNotBlank()
        }
        val verifiedCertCount = certDocs.count { it.verificationStatus.equals("VERIFIED", ignoreCase = true) }
        val pendingCertCount = certDocs.count { it.verificationStatus.equals("PENDING", ignoreCase = true) }

        val qualScore = when {
            verifiedCertCount >= 2 -> 30
            verifiedCertCount == 1 -> 15
            pendingCertCount >= 1 -> 10 // 1 doc submitted, pending admin review
            else -> 0
        }

        // 2. Industry experience / Approval base (max 25)
        val isApproved = b.approved || b.approvalStatus.equals("APPROVED", ignoreCase = true)
        val expScore = if (isApproved) 25 else 10

        // 3. Profile completeness (max 15)
        var profScore = 0
        if (b.description.isNotBlank()) profScore += 4
        if (b.address.isNotBlank()) profScore += 4
        if (b.contactNumber.isNotBlank()) profScore += 4
        if (b.openingHours.isNotBlank()) profScore += 3

        // 4. Verified documents (max 15)
        val hasVerifiedDoc = allDocs.any { it.verificationStatus.equals("VERIFIED", ignoreCase = true) }
        val docScore = if (hasVerifiedDoc || b.verificationStatus.equals("VERIFIED", ignoreCase = true) || isApproved) {
            15
        } else if (allDocs.isNotEmpty()) {
            5 // Document submitted pending verification
        } else {
            0
        }

        // 5. Eligible customer reviews (max 15)
        val rating = b.rating ?: 0.0
        val count = b.reviewCount
        val revScore = if (count >= 1 && rating > 0.0) {
            ((rating / 5.0) * 15.0).toInt().coerceIn(1, 15)
        } else {
            0 // Exactly 0 points if there are no reviews yet
        }

        val totalScore = (qualScore + expScore + profScore + docScore + revScore).coerceIn(0, 100)

        val (badge, desc) = when {
            totalScore >= 85 -> "Top Tier Verified Partner" to "Excellent trust standing based on verified credentials, high ratings, and verified business documentation."
            totalScore >= 70 -> "Verified Trusted Business" to "Strong trust standing. Platform credentials and active business profile verified."
            totalScore >= 50 -> "Standard Verified Partner" to "Good trust standing. Core business details and documents on file."
            else -> "Building Trust Profile" to "Newly registered business. Upload credentials and complete details to boost score."
        }

        return TrustScoreBreakdown(
            qualificationScore = qualScore,
            maxQualificationScore = 30,
            experienceScore = expScore,
            maxExperienceScore = 25,
            profileScore = profScore,
            maxProfileScore = 15,
            docScore = docScore,
            maxDocScore = 15,
            reviewScore = revScore,
            maxReviewScore = 15,
            totalScore = totalScore,
            trustBadge = badge,
            trustDescription = desc
        )
    }

    private fun getRelevantLocalDocs(targetId: String): List<QualificationDocument> {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: (UserSessionManager.getProfile()?.get("uid") as? String)
        val sessionBizName = (UserSessionManager.getProfile()?.get("businessName") as? String)?.trim()?.lowercase()
        return UserSessionManager.getAllQualificationDocuments().filter {
            it.businessId == targetId ||
            (!currentUid.isNullOrBlank() && it.ownerId == currentUid) ||
            (!sessionBizName.isNullOrBlank() && it.businessName.trim().lowercase() == sessionBizName)
        }
    }

    fun getLocalTrustScoreBreakdown(businessId: String): TrustScoreBreakdown {
        val targetId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val localBiz = UserSessionManager.getAllBusinesses().firstOrNull { it.id == targetId }
            ?: UserSessionManager.getAllBusinesses().firstOrNull {
                val sessionName = (UserSessionManager.getProfile()?.get("businessName") as? String)?.trim()?.lowercase()
                !sessionName.isNullOrBlank() && it.name.trim().lowercase() == sessionName
            }
        val localDocs = getRelevantLocalDocs(targetId)
        return computeBreakdown(localBiz, localDocs)
    }

    fun getTrustScoreBreakdown(businessId: String, onComplete: (TrustScoreBreakdown) -> Unit) {
        val targetId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val localBreakdown = getLocalTrustScoreBreakdown(targetId)
        onComplete(localBreakdown)

        val localBiz = UserSessionManager.getAllBusinesses().firstOrNull { it.id == targetId }
            ?: UserSessionManager.getAllBusinesses().firstOrNull {
                val sessionName = (UserSessionManager.getProfile()?.get("businessName") as? String)?.trim()?.lowercase()
                !sessionName.isNullOrBlank() && it.name.trim().lowercase() == sessionName
            }
        val localDocs = getRelevantLocalDocs(targetId)
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: (UserSessionManager.getProfile()?.get("uid") as? String)

        fun persistAndNotify(biz: Business, docs: List<QualificationDocument>) {
            val breakdown = computeBreakdown(biz, docs)
            UserSessionManager.updateBusinessTrustScore(targetId, breakdown.totalScore)

            val bizRef = db.collection("businesses").document(targetId)
            bizRef.set(mapOf("trustScore" to breakdown.totalScore), SetOptions.merge())
                .addOnCompleteListener {
                    onComplete(breakdown)
                }
        }

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

            // Fetch qualifications from Firestore
            db.collection("qualificationVerifications")
                .whereEqualTo("businessId", targetId)
                .get()
                .addOnSuccessListener { docSnap ->
                    val remoteDocs = docSnap.documents.mapNotNull { it.toObject(QualificationDocument::class.java)?.copy(id = it.id) }
                    val combinedDocs = (localDocs + remoteDocs).distinctBy { it.id }

                    if (combinedDocs.isEmpty() && !currentUid.isNullOrBlank()) {
                        db.collection("qualificationVerifications")
                            .whereEqualTo("ownerId", currentUid)
                            .get()
                            .addOnSuccessListener { ownerDocSnap ->
                                val ownerDocs = ownerDocSnap.documents.mapNotNull { it.toObject(QualificationDocument::class.java)?.copy(id = it.id) }
                                val finalDocs = (localDocs + ownerDocs).distinctBy { it.id }
                                persistAndNotify(effectiveBiz, finalDocs)
                            }
                            .addOnFailureListener {
                                persistAndNotify(effectiveBiz, combinedDocs)
                            }
                    } else {
                        persistAndNotify(effectiveBiz, combinedDocs)
                    }
                }
                .addOnFailureListener {
                    persistAndNotify(effectiveBiz, localDocs)
                }
        }.addOnFailureListener {
            val effectiveBiz = localBiz ?: Business(
                id = targetId,
                name = (UserSessionManager.getProfile()?.get("businessName") as? String) ?: "Business",
                approved = false,
                approvalStatus = "PENDING",
                verificationStatus = "PENDING"
            )
            persistAndNotify(effectiveBiz, localDocs)
        }
    }

    fun recalculateTrustScore(businessId: String, onComplete: (Int) -> Unit = {}) {
        getTrustScoreBreakdown(businessId) { breakdown ->
            onComplete(breakdown.totalScore)
        }
    }
}
