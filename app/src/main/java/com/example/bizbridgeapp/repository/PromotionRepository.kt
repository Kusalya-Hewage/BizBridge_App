package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.UserSessionManager
import com.example.bizbridgeapp.model.Promotion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object PromotionRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val PROMOTIONS = "promotions"

    val defaultDemoPromotions = emptyList<Promotion>()

    fun seedDefaultPromotionsIfEmpty(businessId: String, onComplete: (List<Promotion>) -> Unit = {}) {
        onComplete(emptyList())
    }

    /** Get active promotions for customer home */
    fun getActivePromotions(onSuccess: (List<Promotion>) -> Unit, onFailure: (Exception) -> Unit = {}) {
        val localActive = UserSessionManager.getAllPromotions().filter { it.status == "ACTIVE" }

        db.collection(PROMOTIONS)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnSuccessListener { snapshot ->
                val cloudList = snapshot.documents.mapNotNull { it.toObject(Promotion::class.java)?.copy(id = it.id) }
                val merged = (localActive + cloudList).distinctBy { it.id }
                onSuccess(merged)
            }
            .addOnFailureListener {
                onSuccess(localActive)
            }
    }

    /** Real-time listener for promotions belonging to a business */
    fun listenToPromotionsForBusiness(businessId: String, onUpdate: (List<Promotion>) -> Unit): ListenerRegistration {
        val targetBizId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val initialLocal = UserSessionManager.getPromotionsForBusiness(targetBizId)
        if (initialLocal.isNotEmpty()) {
            onUpdate(initialLocal)
        }

        return db.collection(PROMOTIONS)
            .whereEqualTo("businessId", targetBizId)
            .addSnapshotListener { snapshot, error ->
                val localList = UserSessionManager.getPromotionsForBusiness(targetBizId)
                if (error != null || snapshot == null) {
                    onUpdate(localList)
                    return@addSnapshotListener
                }
                if (snapshot.isEmpty) {
                    onUpdate(localList)
                } else {
                    val cloudList = snapshot.documents.mapNotNull { it.toObject(Promotion::class.java)?.copy(id = it.id) }
                    val merged = (localList + cloudList).distinctBy { it.id }
                    onUpdate(merged)
                }
            }
    }

    /** Create or update a promotion */
    fun createOrUpdatePromotion(promotion: Promotion, onSuccess: () -> Unit, onFailure: (Exception) -> Unit = {}) {
        val targetBizId = if (promotion.businessId.isNotBlank()) promotion.businessId else BusinessRepository.getOwnerBusinessId()
        val docId = if (promotion.id.isNotEmpty()) promotion.id else "promo_${System.currentTimeMillis()}"
        val toSave = promotion.copy(id = docId, businessId = targetBizId)

        // Always save locally immediately
        UserSessionManager.savePromotion(toSave)

        val docRef = db.collection(PROMOTIONS).document(docId)
        docRef.set(toSave.toMap())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                // Succeed locally if cloud returns PERMISSION_DENIED or offline
                onSuccess()
            }
    }

    /** Toggle or change promotion status (ACTIVE, PAUSED, EXPIRED) */
    fun togglePromotionStatus(promotionId: String, status: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit = {}) {
        val local = UserSessionManager.getAllPromotions().firstOrNull { it.id == promotionId }
        if (local != null) {
            UserSessionManager.savePromotion(local.copy(status = status))
        }

        db.collection(PROMOTIONS).document(promotionId)
            .update("status", status)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }
}
