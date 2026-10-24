package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.QualificationDocument
import com.example.bizbridgeapp.model.Review
import com.google.firebase.firestore.FirebaseFirestore

object TrustScoreRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Phase 10 Trust Score Algorithm:
     * - Professional qualifications: max 30 (15 pts per verified professional cert, up to 30)
     * - Industry experience: max 25 (default base 20 pts once approved)
     * - Profile completeness: max 15 (address, contact, openingHours, description present)
     * - Verified documents: max 15 (business reg / identity verified = 15 pts)
     * - Eligible customer reviews: max 15 (based on average rating: 5.0 -> 15 pts, 4.0 -> 12 pts, min 3 reviews)
     * Total: 100
     */
    fun recalculateTrustScore(businessId: String, onComplete: (Int) -> Unit = {}) {
        val bizRef = db.collection("businesses").document(businessId)
        bizRef.get().addOnSuccessListener { bizSnap ->
            val biz = bizSnap.toObject(Business::class.java) ?: return@addOnSuccessListener

            db.collection("qualificationVerifications")
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("verificationStatus", "VERIFIED")
                .get()
                .addOnSuccessListener { docSnap ->
                    val verifiedDocs = docSnap.documents.mapNotNull { it.toObject(QualificationDocument::class.java) }

                    // 1. Professional qualifications (max 30)
                    val certCount = verifiedDocs.count { it.documentType.contains("CERT", ignoreCase = true) }
                    val qualificationScore = (certCount * 15).coerceAtMost(30)

                    // 2. Industry experience / Approval base (max 25)
                    val experienceScore = if (biz.approved) 25 else 10

                    // 3. Profile completeness (max 15)
                    var profileScore = 0
                    if (biz.description.isNotBlank()) profileScore += 4
                    if (biz.address.isNotBlank()) profileScore += 4
                    if (biz.contactNumber.isNotBlank()) profileScore += 4
                    if (biz.openingHours.isNotBlank()) profileScore += 3

                    // 4. Verified documents (max 15)
                    val hasRegDoc = verifiedDocs.any { !it.documentType.contains("CERT", ignoreCase = true) }
                    val docScore = if (hasRegDoc || biz.verificationStatus == "VERIFIED") 15 else 0

                    // 5. Eligible customer reviews (max 15)
                    val rating = biz.rating ?: 0.0
                    val count = biz.reviewCount
                    val reviewScore = if (count >= 1) {
                        ((rating / 5.0) * 15.0).toInt().coerceIn(0, 15)
                    } else {
                        10 // Baseline neutral for new businesses without penalizing
                    }

                    val totalScore = (qualificationScore + experienceScore + profileScore + docScore + reviewScore).coerceIn(0, 100)

                    bizRef.update("trustScore", totalScore).addOnSuccessListener {
                        onComplete(totalScore)
                    }
                }
        }
    }
}
