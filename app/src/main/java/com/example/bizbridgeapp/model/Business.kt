package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp

/**
 * Data class representing a business listing stored in Firestore.
 * Fields are based on the UI placeholders and typical business schema.
 */
 data class Business(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val address: String = "",
    val imageUrl: String? = null,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val price: String? = null,
    val approved: Boolean = false,
    val active: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "category" to category,
        "description" to description,
        "address" to address,
        "imageUrl" to imageUrl,
        "rating" to rating,
        "reviewCount" to reviewCount,
        "price" to price,
        "approved" to approved,
        "active" to active,
        "latitude" to latitude,
        "longitude" to longitude,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}
