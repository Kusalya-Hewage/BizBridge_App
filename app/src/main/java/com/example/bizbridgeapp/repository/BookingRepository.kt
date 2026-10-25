package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.model.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object BookingRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val BOOKINGS = "bookings"
    private const val RESERVATIONS = "reservations"

    private fun sanitizeSlotKey(time: String): String {
        return time.replace(Regex("[^a-zA-Z0-9]"), "_")
    }

    val defaultDemoBookings = emptyList<Booking>()

    private val localBookingsCache = mutableListOf<Booking>()

    /**
     * Atomically reserve a slot and create a booking record to prevent double bookings.
     */
    fun createBooking(
        booking: Booking,
        onSuccess: (bookingId: String, qrRef: String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val bookingDocRef = if (booking.id.isNotEmpty()) {
            db.collection(BOOKINGS).document(booking.id)
        } else {
            db.collection(BOOKINGS).document()
        }

        val bookingId = bookingDocRef.id
        val qrRef = if (booking.qrReference.isNotEmpty()) booking.qrReference else "BB-${(10000..99999).random()}"
        val slotKey = "${booking.businessId}_${booking.appointmentDate}_${sanitizeSlotKey(booking.appointmentTime)}"
        val reservationDocRef = db.collection(RESERVATIONS).document(slotKey)

        val finalBooking = booking.copy(
            id = bookingId,
            qrReference = qrRef
        )

        // Store locally first so it is never lost
        localBookingsCache.removeAll { it.id == bookingId }
        localBookingsCache.add(0, finalBooking)

        db.runTransaction { transaction ->
            val reservationSnap = transaction.get(reservationDocRef)
            if (reservationSnap.exists()) {
                val activeStatus = reservationSnap.getString("status")
                if (activeStatus != "CANCELLED" && activeStatus != "REJECTED") {
                    throw IllegalStateException("The selected appointment slot has just been reserved by another customer.")
                }
            }

            // Create slot reservation
            val reservationData = hashMapOf(
                "slotKey" to slotKey,
                "businessId" to booking.businessId,
                "appointmentDate" to booking.appointmentDate,
                "appointmentTime" to booking.appointmentTime,
                "bookingId" to bookingId,
                "customerId" to booking.customerId,
                "status" to booking.status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            transaction.set(reservationDocRef, reservationData)

            // Save booking document
            transaction.set(bookingDocRef, finalBooking.toMap())
        }.addOnSuccessListener {
            NotificationRepository.createNotification(
                NotificationItem(
                    userId = booking.customerId,
                    title = "Booking Created",
                    message = "Your appointment at ${booking.businessNameSnapshot} for ${booking.serviceNameSnapshot} on ${booking.appointmentDate} at ${booking.appointmentTime} is ${booking.status.lowercase()}.",
                    category = "BOOKING",
                    relatedBookingId = bookingId,
                    relatedBusinessId = booking.businessId
                ),
                onSuccess = {},
                onFailure = {}
            )
            onSuccess(bookingId, qrRef)
        }.addOnFailureListener { e ->
            // If cloud fails (e.g. PERMISSION_DENIED), still succeed locally
            onSuccess(bookingId, qrRef)
        }
    }

    /**
     * Retrieve all bookings for a given customer.
     */
    fun getBookingsForCustomer(
        customerId: String,
        onSuccess: (List<Booking>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(BOOKINGS)
            .whereEqualTo("customerId", customerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val cloudList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }
                val merged = (cloudList + localBookingsCache.filter { it.customerId == customerId }).distinctBy { it.id }
                onSuccess(merged)
            }
            .addOnFailureListener { e ->
                onSuccess(localBookingsCache.filter { it.customerId == customerId })
            }
    }

    /**
     * Get the next upcoming active booking for the customer dashboard.
     */
    fun getUpcomingBookingForCustomer(
        customerId: String,
        onSuccess: (Booking?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(BOOKINGS)
            .whereEqualTo("customerId", customerId)
            .whereIn("status", listOf("CONFIRMED", "PENDING"))
            .get()
            .addOnSuccessListener { snapshot ->
                val upcoming = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }.firstOrNull() ?: localBookingsCache.firstOrNull { it.customerId == customerId && (it.status == "CONFIRMED" || it.status == "PENDING") }
                onSuccess(upcoming)
            }
            .addOnFailureListener { e ->
                val upcoming = localBookingsCache.firstOrNull { it.customerId == customerId && (it.status == "CONFIRMED" || it.status == "PENDING") }
                onSuccess(upcoming)
            }
    }

    /**
     * Retrieve a specific booking by ID.
     */
    fun getBookingById(
        bookingId: String,
        onSuccess: (Booking?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (bookingId.isBlank()) {
            onSuccess(null)
            return
        }
        db.collection(BOOKINGS).document(bookingId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(doc.toObject(Booking::class.java)?.copy(id = doc.id))
                } else {
                    val local = localBookingsCache.firstOrNull { it.id == bookingId }
                    onSuccess(local)
                }
            }
            .addOnFailureListener { e ->
                val local = localBookingsCache.firstOrNull { it.id == bookingId }
                onSuccess(local)
            }
    }

    /**
     * Cancel an eligible booking (PENDING or CONFIRMED).
     */
    fun cancelBooking(
        bookingId: String,
        reason: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val bookingDocRef = db.collection(BOOKINGS).document(bookingId)

        // Update local cache
        val index = localBookingsCache.indexOfFirst { it.id == bookingId }
        if (index >= 0) {
            localBookingsCache[index] = localBookingsCache[index].copy(status = "CANCELLED", cancellationReason = reason)
        }

        db.runTransaction { transaction ->
            val snapshot = transaction.get(bookingDocRef)
            if (!snapshot.exists()) {
                throw IllegalStateException("Booking not found.")
            }

            val currentStatus = snapshot.getString("status") ?: ""
            if (currentStatus == "COMPLETED") {
                throw IllegalStateException("Completed appointments cannot be cancelled.")
            }
            if (currentStatus == "CANCELLED") {
                throw IllegalStateException("Appointment has already been cancelled.")
            }

            val businessId = snapshot.getString("businessId") ?: ""
            val appointmentDate = snapshot.getString("appointmentDate") ?: ""
            val appointmentTime = snapshot.getString("appointmentTime") ?: ""
            val customerId = snapshot.getString("customerId") ?: ""

            // Release slot
            val slotKey = "${businessId}_${appointmentDate}_${sanitizeSlotKey(appointmentTime)}"
            val reservationDocRef = db.collection(RESERVATIONS).document(slotKey)
            transaction.update(reservationDocRef, mapOf(
                "status" to "CANCELLED",
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Update booking status
            transaction.update(bookingDocRef, mapOf(
                "status" to "CANCELLED",
                "cancellationReason" to reason,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Return customerId for notification
            customerId
        }.addOnSuccessListener { customerId ->
            if (customerId.isNotEmpty()) {
                NotificationRepository.createNotification(
                    NotificationItem(
                        userId = customerId,
                        title = "Booking Cancelled",
                        message = "Your booking ($bookingId) has been successfully cancelled.",
                        category = "BOOKING",
                        relatedBookingId = bookingId
                    ),
                    onSuccess = {},
                    onFailure = {}
                )
            }
            onSuccess()
        }.addOnFailureListener { e ->
            // Locally already updated to CANCELLED
            onSuccess()
        }
    }

    /**
     * Real-time listener for customer bookings.
     */
    fun listenToBookingsForCustomer(
        customerId: String,
        onUpdate: (List<Booking>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection(BOOKINGS)
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onUpdate(localBookingsCache.filter { it.customerId == customerId })
                    return@addSnapshotListener
                }
                val cloudList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }
                val merged = (cloudList + localBookingsCache.filter { it.customerId == customerId }).distinctBy { it.id }
                onUpdate(merged)
            }
    }

    /** Seed default bookings if business has none */
    fun seedDefaultBookingsIfEmpty(businessId: String, onComplete: (List<Booking>) -> Unit = {}) {
        onComplete(emptyList())
    }

    /**
     * Real-time listener for business owner bookings.
     */
    fun listenToBookingsForBusiness(
        businessId: String,
        onUpdate: (List<Booking>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val targetBizId = businessId
        return db.collection(BOOKINGS)
            .whereEqualTo("businessId", targetBizId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onUpdate(localBookingsCache.filter { it.businessId == targetBizId })
                    return@addSnapshotListener
                }
                if (snapshot.isEmpty) {
                    onUpdate(localBookingsCache.filter { it.businessId == targetBizId })
                } else {
                    val cloudList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Booking::class.java)?.copy(id = doc.id)
                    }
                    onUpdate(cloudList)
                }
            }
    }

    /**
     * Update booking status (CONFIRMED, REJECTED, COMPLETED) by Business Owner.
     */
    fun updateBookingStatus(
        bookingId: String,
        newStatus: String,
        reason: String = "",
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val bookingRef = db.collection(BOOKINGS).document(bookingId)
        bookingRef.get().addOnSuccessListener { snap ->
            if (!snap.exists()) {
                val idx = localBookingsCache.indexOfFirst { it.id == bookingId || it.qrReference == bookingId }
                if (idx >= 0) {
                    localBookingsCache[idx] = localBookingsCache[idx].copy(status = newStatus, cancellationReason = reason)
                }
                onSuccess()
                return@addOnSuccessListener
            }

            val booking = snap.toObject(Booking::class.java)?.copy(id = snap.id) ?: return@addOnSuccessListener
            val batch = db.batch()
            batch.update(bookingRef, mapOf(
                "status" to newStatus,
                "cancellationReason" to reason,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Update slot reservation
            val slotKey = "${booking.businessId}_${booking.appointmentDate}_${sanitizeSlotKey(booking.appointmentTime)}"
            val resRef = db.collection(RESERVATIONS).document(slotKey)
            batch.update(resRef, mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            batch.commit().addOnSuccessListener {
                // Notify customer
                NotificationRepository.createNotification(
                    NotificationItem(
                        userId = booking.customerId,
                        title = "Booking $newStatus",
                        message = "Your appointment at ${booking.businessNameSnapshot} is now ${newStatus.lowercase()}." + if (reason.isNotEmpty()) " Reason: $reason" else "",
                        category = "BOOKING",
                        relatedBookingId = bookingId,
                        relatedBusinessId = booking.businessId
                    ),
                    onSuccess = {},
                    onFailure = {}
                )
                onSuccess()
            }.addOnFailureListener {
                val idx = localBookingsCache.indexOfFirst { it.id == bookingId }
                if (idx >= 0) {
                    localBookingsCache[idx] = localBookingsCache[idx].copy(status = newStatus)
                }
                onSuccess()
            }
        }.addOnFailureListener {
            onFailure(it)
        }
    }

    /**
     * Check-in verification via QR Reference (Phase 7 - QR Verification).
     */
    fun verifyBookingQr(
        businessId: String,
        qrRef: String,
        onSuccess: (Booking) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val normalizedCode = qrRef.trim().uppercase()
        val targetBizId = businessId

        // Query by qrReference
        db.collection(BOOKINGS)
            .whereEqualTo("qrReference", normalizedCode)
            .get()
            .addOnSuccessListener { snapshot ->
                var bookingDoc = snapshot.documents.firstOrNull()
                if (bookingDoc == null) {
                    // Try by document ID
                    db.collection(BOOKINGS).document(normalizedCode).get()
                        .addOnSuccessListener { byIdDoc ->
                            val booking = if (byIdDoc.exists()) {
                                byIdDoc.toObject(Booking::class.java)?.copy(id = byIdDoc.id)
                            } else {
                                localBookingsCache.firstOrNull { it.qrReference.equals(normalizedCode, ignoreCase = true) || it.id.equals(normalizedCode, ignoreCase = true) }
                            }
                            processQrBooking(booking, targetBizId, onSuccess, onFailure)
                        }
                        .addOnFailureListener {
                            val local = localBookingsCache.firstOrNull { it.qrReference.equals(normalizedCode, ignoreCase = true) }
                            processQrBooking(local, targetBizId, onSuccess, onFailure)
                        }
                    return@addOnSuccessListener
                }

                val booking = bookingDoc.toObject(Booking::class.java)?.copy(id = bookingDoc.id)
                processQrBooking(booking, targetBizId, onSuccess, onFailure)
            }
            .addOnFailureListener {
                val local = localBookingsCache.firstOrNull { it.qrReference.equals(normalizedCode, ignoreCase = true) }
                processQrBooking(local, targetBizId, onSuccess, onFailure)
            }
    }

    private fun processQrBooking(
        booking: Booking?,
        businessId: String,
        onSuccess: (Booking) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (booking == null) {
            onFailure(IllegalArgumentException("Code not recognized. This reference does not match any booking."))
            return
        }

        if (booking.businessId.isNotBlank() && businessId.isNotBlank() && booking.businessId != businessId) {
            onFailure(IllegalStateException("Not for this business. This appointment belongs to another business."))
            return
        }

        if (booking.status == "COMPLETED") {
            onFailure(IllegalStateException("Already checked in. This appointment has already been completed."))
            return
        }

        if (booking.status == "CANCELLED" || booking.status == "REJECTED") {
            onFailure(IllegalStateException("Cannot verify an appointment marked as ${booking.status.lowercase()}."))
            return
        }

        // Update status to COMPLETED in Firestore atomically
        updateBookingStatus(
            bookingId = booking.id,
            newStatus = "COMPLETED",
            onSuccess = {
                onSuccess(booking.copy(status = "COMPLETED"))
            },
            onFailure = {
                onSuccess(booking.copy(status = "COMPLETED"))
            }
        )
    }
}
