package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.UserSessionManager
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.QualificationDocument
import com.example.bizbridgeapp.model.Review
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
     * Phase 10 Dynamic Trust Score Algorithm:
     * - Professional qualifications: max 30 (15 pts per verified professional cert, up to 30)
     * - Industry experience / Approval base: max 25 (25 pts once approved, 10 pts baseline if pending)
     * - Profile completeness: max 15 (description +4, address +4, contactNumber +4, openingHours +3)
     * - Verified documents: max 15 (business reg / identity verified = 15 pts)
     * - Eligible customer reviews: max 15 (based on average rating: 5.0 -> 15 pts, 4.0 -> 12 pts, min baseline 10)
     * Total: 100
     */
    fun computeBreakdown(biz: Business?, verifiedDocs: List<QualificationDocument>): TrustScoreBreakdown {
        val b = biz ?: Business()

        // 1. Professional qualifications (max 30)
        val certCount = verifiedDocs.count {
            it.documentType.contains("CERT", ignoreCase = true) ||
            it.title.contains("cert", ignoreCase = true) ||
            it.title.contains("diploma", ignoreCase = true) ||
            it.title.contains("degree", ignoreCase = true) ||
            it.title.contains("license", ignoreCase = true)
        }
        val qualScore = (certCount * 15).coerceAtMost(30)

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
        val hasRegDoc = verifiedDocs.any { !it.documentType.contains("CERT", ignoreCase = true) }
        val docScore = if (hasRegDoc || b.verificationStatus.equals("VERIFIED", ignoreCase = true) || isApproved) 15 else 0

        // 5. Eligible customer reviews (max 15)
        val rating = b.rating ?: 0.0
        val count = b.reviewCount
        val revScore = if (count >= 1) {
            ((rating / 5.0) * 15.0).toInt().coerceIn(0, 15)
        } else {
            10 // Baseline neutral for new businesses
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

    fun getLocalTrustScoreBreakdown(businessId: String): TrustScoreBreakdown {
        val targetId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val localBiz = UserSessionManager.getAllBusinesses().firstOrNull { it.id == targetId }
        val localVerifiedDocs = UserSessionManager.getAllQualificationDocuments()
            .filter { it.businessId == targetId && it.verificationStatus.equals("VERIFIED", ignoreCase = true) }
        return computeBreakdown(localBiz, localVerifiedDocs)
    }

    fun getTrustScoreBreakdown(businessId: String, onComplete: (TrustScoreBreakdown) -> Unit) {
        val targetId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val localBreakdown = getLocalTrustScoreBreakdown(targetId)
        // Immediately notify with local cached calculation so UI never waits or flashes
        onComplete(localBreakdown)

        val localBiz = UserSessionManager.getAllBusinesses().firstOrNull { it.id == targetId }
        val localVerifiedDocs = UserSessionManager.getAllQualificationDocuments()
            .filter { it.businessId == targetId && it.verificationStatus.equals("VERIFIED", ignoreCase = true) }

        fun persistAndNotify(biz: Business, verifiedDocs: List<QualificationDocument>) {
            val breakdown = computeBreakdown(biz, verifiedDocs)
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

            db.collection("qualificationVerifications")
                .whereEqualTo("businessId", targetId)
                .whereEqualTo("verificationStatus", "VERIFIED")
                .get()
                .addOnSuccessListener { docSnap ->
                    val remoteDocs = docSnap.documents.mapNotNull { it.toObject(QualificationDocument::class.java)?.copy(id = it.id) }
                    val mergedDocs = (localVerifiedDocs + remoteDocs).distinctBy { it.id }
                    persistAndNotify(effectiveBiz, mergedDocs)
                }
                .addOnFailureListener {
                    persistAndNotify(effectiveBiz, localVerifiedDocs)
                }
        }.addOnFailureListener {
            val effectiveBiz = localBiz ?: Business(
                id = targetId,
                name = (UserSessionManager.getProfile()?.get("businessName") as? String) ?: "Business",
                approved = false,
                approvalStatus = "PENDING",
                verificationStatus = "PENDING"
            )
            persistAndNotify(effectiveBiz, localVerifiedDocs)
        }
    }

    fun recalculateTrustScore(businessId: String, onComplete: (Int) -> Unit = {}) {
        getTrustScoreBreakdown(businessId) { breakdown ->
            onComplete(breakdown.totalScore)
        }
    }
}
