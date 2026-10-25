package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.Promotion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object PromotionRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val PROMOTIONS = "promotions"

    val defaultDemoPromotions = listOf(
        Promotion(
            id = "promo_season_finish",
            businessId = "biz_juniper_studio",
            businessName = "Juniper Studio",
            title = "A fresh season finish",
            description = "Complimentary gloss treatment with every full color service this month.",
            discountText = "Complimentary treatment",
            status = "ACTIVE"
        )
    )

    fun seedDefaultPromotionsIfEmpty(businessId: String, onComplete: (List<Promotion>) -> Unit = {}) {
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        val colRef = db.collection(PROMOTIONS)
        colRef.whereEqualTo("businessId", targetBizId).get().addOnSuccessListener { snap ->
            if (snap.isEmpty) {
                val batch = db.batch()
                val seeded = defaultDemoPromotions.map { p ->
                    val docRef = colRef.document(p.id)
                    val toSave = p.copy(businessId = targetBizId)
                    batch.set(docRef, toSave.toMap())
                    toSave
                }
                batch.commit()
                    .addOnSuccessListener { onComplete(seeded) }
                    .addOnFailureListener { onComplete(seeded) }
            } else {
                val list = snap.documents.mapNotNull { it.toObject(Promotion::class.java)?.copy(id = it.id) }
                onComplete(list)
            }
        }.addOnFailureListener {
            onComplete(defaultDemoPromotions.map { it.copy(businessId = targetBizId) })
        }
    }

    /** Get active promotions for customer home */
    fun getActivePromotions(onSuccess: (List<Promotion>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection(PROMOTIONS)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toObject(Promotion::class.java)?.copy(id = it.id) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    /** Real-time listener for promotions belonging to a business */
    fun listenToPromotionsForBusiness(businessId: String, onUpdate: (List<Promotion>) -> Unit): ListenerRegistration {
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        return db.collection(PROMOTIONS)
            .whereEqualTo("businessId", targetBizId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onUpdate(defaultDemoPromotions.filter { it.businessId == targetBizId })
                    return@addSnapshotListener
                }
                if (snapshot.isEmpty) {
                    seedDefaultPromotionsIfEmpty(targetBizId) { seeded ->
                        onUpdate(seeded)
                    }
                } else {
                    val list = snapshot.documents.mapNotNull { it.toObject(Promotion::class.java)?.copy(id = it.id) }
                    onUpdate(list)
                }
            }
    }

    /** Create or update a promotion */
    fun createOrUpdatePromotion(promotion: Promotion, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = if (promotion.id.isNotEmpty()) db.collection(PROMOTIONS).document(promotion.id) else db.collection(PROMOTIONS).document()
        val toSave = promotion.copy(id = docRef.id)
        docRef.set(toSave.toMap())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /** Toggle or change promotion status (ACTIVE, PAUSED, EXPIRED) */
    fun togglePromotionStatus(promotionId: String, status: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection(PROMOTIONS).document(promotionId)
            .update("status", status)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
