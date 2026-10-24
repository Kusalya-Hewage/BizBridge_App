package com.example.bizbridgeapp.model

import com.google.firebase.Timestamp

/**
 * Data class representing a customer profile stored in Firestore.
 * Fields match the document written by FirebaseAuthManager during registration.
 */
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "CUSTOMER",
    val profileImageUrl: String = "",
    val accountStatus: String = "ACTIVE",
    val notificationPreferences: Map<String, Boolean> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "fullName" to fullName,
        "email" to email,
        "phone" to phone,
        "role" to role,
        "profileImageUrl" to profileImageUrl,
        "accountStatus" to accountStatus,
        "notificationPreferences" to notificationPreferences,
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}
