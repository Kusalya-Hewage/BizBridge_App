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

    /**
     * Retrieve all reviews for a business.
     */
    fun getReviewsForBusiness(
        businessId: String,
        onSuccess: (List<Review>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(REVIEWS)
            .whereEqualTo("businessId", businessId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Review::class.java)?.copy(id = doc.id)
                }
                onSuccess(list)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
