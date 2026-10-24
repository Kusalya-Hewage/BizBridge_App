package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp

data class Booking(
    val id: String = "",
    val customerId: String = "",
    val businessId: String = "",
    val businessNameSnapshot: String = "",
    val businessAddressSnapshot: String = "",
    val serviceId: String = "",
    val serviceNameSnapshot: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val priceAtBooking: Double = 0.0,
    val durationMinutes: Int = 30,
    val status: String = "PENDING", // PENDING, CONFIRMED, REJECTED, CANCELLED, COMPLETED
    val customerNotes: String = "",
    val qrReference: String = "",
    val cancellationReason: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "customerId" to customerId,
        "businessId" to businessId,
        "businessNameSnapshot" to businessNameSnapshot,
        "businessAddressSnapshot" to businessAddressSnapshot,
        "serviceId" to serviceId,
        "serviceNameSnapshot" to serviceNameSnapshot,
        "appointmentDate" to appointmentDate,
        "appointmentTime" to appointmentTime,
        "priceAtBooking" to priceAtBooking,
        "durationMinutes" to durationMinutes,
        "status" to status,
        "customerNotes" to customerNotes,
        "qrReference" to qrReference,
        "cancellationReason" to cancellationReason,
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}
