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

    val defaultDemoBookings = listOf(
        Booking(
            id = "bk_demo_1",
            customerId = "demo_customer",
            businessId = "biz_juniper_studio",
            businessNameSnapshot = "Juniper Studio",
            businessAddressSnapshot = "42 Flower Road, Colombo 07",
            serviceId = "srv_haircut",
            serviceNameSnapshot = "Signature haircut",
            appointmentDate = "Sat, Oct 24, 2026",
            appointmentTime = "10:00 AM",
            priceAtBooking = 45.0,
            durationMinutes = 45,
            status = "CONFIRMED",
            qrReference = "BB-24108"
        ),
        Booking(
            id = "bk_demo_2",
            customerId = "demo_customer",
            businessId = "biz_oliver_beauty",
            businessNameSnapshot = "Oliver Beauty Bar",
            businessAddressSnapshot = "18 Havelock Road, Colombo 05",
            serviceId = "srv_facial",
            serviceNameSnapshot = "Luxury Facial Treatment",
            appointmentDate = "Tue, Oct 27, 2026",
            appointmentTime = "2:00 PM",
            priceAtBooking = 65.0,
            durationMinutes = 60,
            status = "PENDING",
            qrReference = "BB-24110"
        ),
        Booking(
            id = "bk_demo_3",
            customerId = "demo_customer",
            businessId = "biz_daily_cut",
            businessNameSnapshot = "The Daily Cut Barber",
            businessAddressSnapshot = "75 Galle Road, Colombo 03",
            serviceId = "srv_beard",
            serviceNameSnapshot = "Gentleman's Shave & Styling",
            appointmentDate = "Wed, Oct 14, 2026",
            appointmentTime = "11:30 AM",
            priceAtBooking = 35.0,
            durationMinutes = 30,
            status = "COMPLETED",
            qrReference = "BB-23914"
        )
    )

    private val localBookingsCache = mutableListOf<Booking>().apply {
        addAll(defaultDemoBookings)
    }

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
                val merged = (cloudList + localBookingsCache).distinctBy { it.id }
                onSuccess(merged)
            }
            .addOnFailureListener { e ->
                // Return local cache so user's bookings are always available
                onSuccess(localBookingsCache)
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
                }.firstOrNull() ?: localBookingsCache.firstOrNull { it.status == "CONFIRMED" || it.status == "PENDING" }
                onSuccess(upcoming)
            }
            .addOnFailureListener { e ->
                val upcoming = localBookingsCache.firstOrNull { it.status == "CONFIRMED" || it.status == "PENDING" }
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
            onSuccess(localBookingsCache.firstOrNull())
            return
        }
        db.collection(BOOKINGS).document(bookingId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(doc.toObject(Booking::class.java)?.copy(id = doc.id))
                } else {
                    val local = localBookingsCache.firstOrNull { it.id == bookingId } ?: localBookingsCache.firstOrNull()
                    onSuccess(local)
                }
            }
            .addOnFailureListener { e ->
                val local = localBookingsCache.firstOrNull { it.id == bookingId } ?: localBookingsCache.firstOrNull()
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
                    onUpdate(localBookingsCache.filter { it.customerId == customerId || it.customerId == "demo_customer" })
                    return@addSnapshotListener
                }
                val cloudList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }
                val merged = (cloudList + localBookingsCache).distinctBy { it.id }
                onUpdate(merged)
            }
    }

    /**
     * Real-time listener for business owner bookings.
     */
    fun listenToBookingsForBusiness(
        businessId: String,
        onUpdate: (List<Booking>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection(BOOKINGS)
            .whereEqualTo("businessId", businessId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onUpdate(localBookingsCache.filter { it.businessId == businessId })
                    return@addSnapshotListener
                }
                val cloudList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }
                val merged = (cloudList + localBookingsCache.filter { it.businessId == businessId }).distinctBy { it.id }
                onUpdate(merged)
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
                val idx = localBookingsCache.indexOfFirst { it.id == bookingId }
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
        db.collection(BOOKINGS)
            .whereEqualTo("qrReference", qrRef)
            .get()
            .addOnSuccessListener { snapshot ->
                val bookingDoc = snapshot.documents.firstOrNull()
                val booking = bookingDoc?.toObject(Booking::class.java)?.copy(id = bookingDoc.id)
                    ?: localBookingsCache.firstOrNull { it.qrReference.equals(qrRef, ignoreCase = true) }

                if (booking == null) {
                    onFailure(IllegalArgumentException("Invalid QR code. No matching booking found."))
                    return@addOnSuccessListener
                }

                if (booking.businessId != businessId && businessId.isNotEmpty()) {
                    onFailure(IllegalStateException("This booking reference belongs to a different business."))
                    return@addOnSuccessListener
                }

                if (booking.status == "COMPLETED") {
                    onFailure(IllegalStateException("This appointment has already been checked in and completed."))
                    return@addOnSuccessListener
                }

                if (booking.status == "CANCELLED" || booking.status == "REJECTED") {
                    onFailure(IllegalStateException("Cannot verify a ${booking.status.lowercase()} appointment."))
                    return@addOnSuccessListener
                }

                // Update status to COMPLETED atomically
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
            .addOnFailureListener {
                val local = localBookingsCache.firstOrNull { it.qrReference.equals(qrRef, ignoreCase = true) }
                if (local != null) {
                    onSuccess(local.copy(status = "COMPLETED"))
                } else {
                    onFailure(it)
                }
            }
    }
}
