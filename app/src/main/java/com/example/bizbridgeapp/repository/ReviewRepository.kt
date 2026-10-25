package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object ReviewRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val REVIEWS = "reviews"
    private const val BUSINESSES = "businesses"

    /**
     * Submit a review and atomically update the business's average rating and review count.
     * Uses bookingId as the review document ID to prevent duplicate reviews per booking.
     */
    fun submitReview(
        review: Review,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (review.bookingId.isBlank()) {
            onFailure(IllegalArgumentException("Booking ID is required to submit a review."))
            return
        }

        val reviewDocRef = db.collection(REVIEWS).document(review.bookingId)
        val businessDocRef = db.collection(BUSINESSES).document(review.businessId)

        db.runTransaction { transaction ->
            val existingReviewSnap = transaction.get(reviewDocRef)
            if (existingReviewSnap.exists()) {
                throw IllegalStateException("You have already submitted a review for this appointment.")
            }

            val businessSnap = transaction.get(businessDocRef)
            if (!businessSnap.exists()) {
                throw IllegalStateException("Business not found.")
            }

            val currentRating = businessSnap.getDouble("rating") ?: 0.0
            val currentCount = (businessSnap.getLong("reviewCount") ?: 0L).toInt()

            val newCount = currentCount + 1
            val newRating = ((currentRating * currentCount) + review.rating) / newCount
            val roundedRating = Math.round(newRating * 10.0) / 10.0

            // Update business rating summary
            transaction.update(businessDocRef, mapOf(
                "rating" to roundedRating,
                "reviewCount" to newCount,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Save review
            val toSave = review.copy(id = review.bookingId)
            transaction.set(reviewDocRef, toSave.toMap())
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    /**
     * Check if a review has already been submitted for a booking.
     */
    fun hasReviewedBooking(
        bookingId: String,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(REVIEWS).document(bookingId).get()
            .addOnSuccessListener { doc ->
                onSuccess(doc.exists())
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    val defaultDemoReviews = listOf(
        Review(
            id = "rev_maya",
            customerId = "cust_maya",
            customerName = "Maya Chen",
            businessId = "biz_juniper_studio",
            bookingId = "BB-24108",
            rating = 5,
            comment = "Elena listened to exactly what I wanted. Such a lovely neighbourhood studio.",
            moderationStatus = "APPROVED"
        ),
        Review(
            id = "rev_jordan",
            customerId = "cust_jordan",
            customerName = "Jordan Lee",
            businessId = "biz_juniper_studio",
            bookingId = "BB-23910",
            rating = 5,
            comment = "Consistently great cuts and calm atmosphere. Highly recommend Juniper Studio.",
            moderationStatus = "APPROVED"
        )
    )

    fun seedDefaultReviewsIfEmpty(businessId: String, onComplete: (List<Review>) -> Unit = {}) {
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        if (targetBizId != "biz_juniper_studio") {
            onComplete(emptyList())
            return
        }
        db.collection(REVIEWS).whereEqualTo("businessId", targetBizId).get().addOnSuccessListener { snap ->
            if (snap.isEmpty) {
                val batch = db.batch()
                val seeded = defaultDemoReviews.map { r ->
                    val docRef = db.collection(REVIEWS).document(r.id)
                    val toSave = r.copy(businessId = targetBizId)
                    batch.set(docRef, toSave.toMap())
                    toSave
                }
                batch.commit()
                    .addOnSuccessListener { onComplete(seeded) }
                    .addOnFailureListener { onComplete(seeded) }
            } else {
                val list = snap.documents.mapNotNull { it.toObject(Review::class.java)?.copy(id = it.id) }
                onComplete(list)
            }
        }.addOnFailureListener {
            onComplete(defaultDemoReviews.map { it.copy(businessId = targetBizId) })
        }
    }

    /**
     * Real-time listener for reviews belonging to a business.
     */
    fun listenToReviewsForBusiness(
        businessId: String,
        onUpdate: (List<Review>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        return db.collection(REVIEWS)
            .whereEqualTo("businessId", targetBizId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onUpdate(defaultDemoReviews.filter { it.businessId == targetBizId })
                    return@addSnapshotListener
                }
                if (snapshot.isEmpty) {
                    if (targetBizId == "biz_juniper_studio") {
                        seedDefaultReviewsIfEmpty(targetBizId) { seeded ->
                            onUpdate(seeded)
                        }
                    } else {
                        onUpdate(emptyList())
                    }
                } else {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Review::class.java)?.copy(id = doc.id)
                    }
                    onUpdate(list)
                }
            }
    }

    /**
     * Submit moderation report for an inappropriate review (Phase 7 - Reviews & Reports).
     */
    fun reportReview(
        reviewId: String,
        reason: String,
        reporterId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val reportDoc = db.collection("reports").document()
        val data = hashMapOf(
            "id" to reportDoc.id,
            "targetType" to "REVIEW",
            "targetId" to reviewId,
            "reason" to reason,
            "reporterId" to reporterId,
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp()
        )
        reportDoc.set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Retrieve all reviews for a business.
     */
    fun getReviewsForBusiness(
        businessId: String,
        onSuccess: (List<Review>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        db.collection(REVIEWS)
            .whereEqualTo("businessId", targetBizId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    seedDefaultReviewsIfEmpty(targetBizId) { seeded ->
                        onSuccess(seeded)
                    }
                } else {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Review::class.java)?.copy(id = doc.id)
                    }
                    onSuccess(list)
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
