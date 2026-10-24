package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp

data class Service(
    val id: String = "",
    val businessId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val durationMinutes: Int = 30,
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "businessId" to businessId,
        "name" to name,
        "description" to description,
        "price" to price,
        "durationMinutes" to durationMinutes,
        "isActive" to isActive,
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}
