package com.example.bizbridgeapp

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

/**
 * Singleton that handles Firebase Authentication and Firestore profile storage for customers.
 * All callbacks use simple lambdas to keep existing fragment code style (no coroutines).
 */
object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage = com.google.firebase.storage.FirebaseStorage.getInstance()

    fun getCurrentUser() = auth.currentUser

    /** Register a new customer and persist profile in Firestore */
    fun registerCustomer(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        profileImageUri: android.net.Uri? = null,
        onSuccess: (firebaseUser: com.google.firebase.auth.FirebaseUser) -> Unit,
        onFailure: (exception: Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: run {
                    onFailure(Exception("Firebase returned null user after registration"))
                    return@addOnSuccessListener
                }

                fun saveFirestoreProfile(imageUrl: String) {
                    val profile = hashMapOf(
                        "uid" to user.uid,
                        "fullName" to fullName,
                        "email" to email,
                        "phone" to phone,
                        "role" to "CUSTOMER",
                        "profileImageUrl" to imageUrl,
                        "accountStatus" to "ACTIVE",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    firestore.collection("users").document(user.uid).set(profile)
                        .addOnSuccessListener {
                            Log.d(TAG, "Profile saved for uid=${user.uid}")
                            onSuccess(user)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to save profile for uid=${user.uid}", e)
                            onFailure(e)
                        }
                }

                if (profileImageUri != null) {
                    uploadProfileImage(user.uid, profileImageUri,
                        onSuccess = { downloadUrl -> saveFirestoreProfile(downloadUrl) },
                        onFailure = {
                            // If image upload fails, don't fail registration; continue with empty image URL
                            saveFirestoreProfile("")
                        }
                    )
                } else {
                    saveFirestoreProfile("")
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /** Register a new Business Owner, save business profile & submit business application */
    fun registerOwner(
        ownerName: String,
        businessName: String,
        category: String,
        email: String,
        password: String,
        onSuccess: (firebaseUser: com.google.firebase.auth.FirebaseUser, businessId: String) -> Unit,
        onFailure: (exception: Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: run {
                    onFailure(Exception("Firebase returned null user after owner registration"))
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()

                // 1. User profile
                val userDoc = firestore.collection("users").document(user.uid)
                val userProfile = hashMapOf(
                    "uid" to user.uid,
                    "fullName" to ownerName,
                    "email" to email,
                    "phone" to "",
                    "role" to "BUSINESS_OWNER",
                    "profileImageUrl" to "",
                    "accountStatus" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(userDoc, userProfile)

                // 2. Business document
                val bizDoc = firestore.collection("businesses").document()
                val businessId = bizDoc.id
                val businessData = hashMapOf(
                    "id" to businessId,
                    "ownerId" to user.uid,
                    "name" to businessName,
                    "category" to category,
                    "description" to "Welcome to $businessName! Professional services tailored for you.",
                    "address" to "Colombo, Sri Lanka",
                    "contactNumber" to "",
                    "openingHours" to "9:00 AM - 7:00 PM",
                    "approved" to false,
                    "active" to false,
                    "approvalStatus" to "PENDING",
                    "verificationStatus" to "PENDING",
                    "trustScore" to 80,
                    "rating" to 5.0,
                    "reviewCount" to 0,
                    "latitude" to 6.9271,
                    "longitude" to 79.8612,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(bizDoc, businessData)

                // 3. Business application
                val appDoc = firestore.collection("businessApplications").document()
                val appData = hashMapOf(
                    "id" to appDoc.id,
                    "ownerId" to user.uid,
                    "businessId" to businessId,
                    "businessName" to businessName,
                    "ownerName" to ownerName,
                    "email" to email,
                    "category" to category,
                    "address" to "Colombo, Sri Lanka",
                    "status" to "PENDING",
                    "submittedAt" to FieldValue.serverTimestamp(),
                    "rejectionReason" to ""
                )
                batch.set(appDoc, appData)

                batch.commit().addOnSuccessListener {
                    onSuccess(user, businessId)
                }.addOnFailureListener { e ->
                    onFailure(e)
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /** Upload profile image to Firebase Storage and retrieve download URL */
    fun uploadProfileImage(
        uid: String,
        uri: android.net.Uri,
        onSuccess: (downloadUrl: String) -> Unit,
        onFailure: (exception: Exception) -> Unit
    ) {
        val ref = storage.reference.child("profile_images/$uid.jpg")
        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { uriResult ->
                onSuccess(uriResult.toString())
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /** Sign in existing user */
    fun signIn(
        email: String,
        password: String,
        onSuccess: (firebaseUser: com.google.firebase.auth.FirebaseUser) -> Unit,
        onFailure: (exception: Exception) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: run {
                    onFailure(Exception("Firebase returned null user after sign‑in"))
                    return@addOnSuccessListener
                }
                onSuccess(user)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /** Send password‑reset email */
    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (exception: Exception) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /** Sign out current user */
    fun signOut() {
        auth.signOut()
    }

    /** Fetch Firestore profile for given UID */
    fun fetchUserProfile(
        uid: String,
        onSuccess: (profile: Map<String, Any>) -> Unit,
        onFailure: (exception: Exception) -> Unit
    ) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    onSuccess(snap.data ?: emptyMap())
                } else {
                    onFailure(Exception("User profile not found in Firestore"))
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
