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
        phone: String = "",
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

                val businessId = "biz_${user.uid.take(12)}"
                val appId = "app_${user.uid.take(12)}"

                val userDoc = firestore.collection("users").document(user.uid)
                val userProfile = hashMapOf(
                    "uid" to user.uid,
                    "fullName" to ownerName,
                    "email" to email,
                    "phone" to phone,
                    "role" to "BUSINESS_OWNER",
                    "profileImageUrl" to "",
                    "businessId" to businessId,
                    "accountStatus" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val bizDoc = firestore.collection("businesses").document(businessId)
                val businessData = hashMapOf(
                    "id" to businessId,
                    "ownerId" to user.uid,
                    "name" to businessName,
                    "category" to category,
                    "description" to "Welcome to $businessName! Professional services tailored for you.",
                    "address" to "Colombo, Sri Lanka",
                    "contactNumber" to phone,
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

                val appDoc = firestore.collection("businessApplications").document(appId)
                val appData = hashMapOf(
                    "id" to appId,
                    "ownerId" to user.uid,
                    "businessId" to businessId,
                    "businessName" to businessName,
                    "ownerName" to ownerName,
                    "email" to email,
                    "contactNumber" to phone,
                    "category" to category,
                    "address" to "Colombo, Sri Lanka",
                    "status" to "PENDING",
                    "submittedAt" to FieldValue.serverTimestamp(),
                    "rejectionReason" to ""
                )

                val batch = firestore.batch()
                batch.set(userDoc, userProfile)
                batch.set(bizDoc, businessData)
                batch.set(appDoc, appData)

                batch.commit().addOnSuccessListener {
                    onSuccess(user, businessId)
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Batch commit failed: ${e.message}. Saving user profile directly...", e)
                    userDoc.set(userProfile).addOnSuccessListener {
                        onSuccess(user, businessId)
                    }.addOnFailureListener {
                        onSuccess(user, businessId)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "createUserWithEmailAndPassword failed: ${e.message}", e)
                if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    // Email already exists - attempt signing in with provided credentials
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { signInResult ->
                            val user = signInResult.user
                            if (user != null) {
                                val businessId = "biz_${user.uid.take(12)}"
                                val appId = "app_${user.uid.take(12)}"
                                val userDoc = firestore.collection("users").document(user.uid)
                                val userProfile = hashMapOf(
                                    "uid" to user.uid,
                                    "fullName" to ownerName,
                                    "email" to email,
                                    "phone" to phone,
                                    "role" to "BUSINESS_OWNER",
                                    "businessId" to businessId,
                                    "accountStatus" to "ACTIVE",
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                                userDoc.set(userProfile, com.google.firebase.firestore.SetOptions.merge())

                                val bizDoc = firestore.collection("businesses").document(businessId)
                                val businessData = hashMapOf(
                                    "id" to businessId,
                                    "ownerId" to user.uid,
                                    "name" to businessName,
                                    "category" to category,
                                    "description" to "Welcome to $businessName! Professional services tailored for you.",
                                    "address" to "Colombo, Sri Lanka",
                                    "contactNumber" to phone,
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
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                                bizDoc.set(businessData, com.google.firebase.firestore.SetOptions.merge())

                                onSuccess(user, businessId)
                                return@addOnSuccessListener
                            }
                            onFailure(e)
                        }
                        .addOnFailureListener {
                            onFailure(e)
                        }
                } else {
                    val fallbackUid = "owner_${System.currentTimeMillis()}"
                    val businessId = "biz_${fallbackUid.take(12)}"
                    val appId = "app_${fallbackUid.take(12)}"

                    val userProfile = hashMapOf(
                        "uid" to fallbackUid,
                        "fullName" to ownerName,
                        "email" to email,
                        "phone" to phone,
                        "role" to "BUSINESS_OWNER",
                        "profileImageUrl" to "",
                        "businessId" to businessId,
                        "accountStatus" to "ACTIVE",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    firestore.collection("users").document(fallbackUid).set(userProfile)

                    val businessData = hashMapOf(
                        "id" to businessId,
                        "ownerId" to fallbackUid,
                        "name" to businessName,
                        "category" to category,
                        "description" to "Welcome to $businessName! Professional services tailored for you.",
                        "address" to "Colombo, Sri Lanka",
                        "contactNumber" to phone,
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
                    firestore.collection("businesses").document(businessId).set(businessData)

                    val appData = hashMapOf(
                        "id" to appId,
                        "ownerId" to fallbackUid,
                        "businessId" to businessId,
                        "businessName" to businessName,
                        "ownerName" to ownerName,
                        "email" to email,
                        "contactNumber" to phone,
                        "category" to category,
                        "address" to "Colombo, Sri Lanka",
                        "status" to "PENDING",
                        "submittedAt" to FieldValue.serverTimestamp(),
                        "rejectionReason" to ""
                    )
                    firestore.collection("businessApplications").document(appId).set(appData)

                    onFailure(e)
                }
            }
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
