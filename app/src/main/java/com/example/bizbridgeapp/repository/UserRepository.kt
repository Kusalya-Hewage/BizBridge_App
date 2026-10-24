package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val USERS = "users"

    fun createOrUpdate(profile: UserProfile, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val map = profile.toMap()
        db.collection(USERS).document(profile.uid)
            .set(map, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getProfile(uid: String, onSuccess: (UserProfile) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection(USERS).document(uid).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val user = snap.toObject(UserProfile::class.java) ?: UserProfile(uid = uid)
                    onSuccess(user)
                } else {
                    onFailure(Exception("User profile not found"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
}
