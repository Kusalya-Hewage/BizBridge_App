package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.UserSessionManager
import com.example.bizbridgeapp.model.NotificationItem
import com.example.bizbridgeapp.model.Promotion
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

object NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val NOTIFICATIONS = "notifications"

    fun isRelevantForUser(item: NotificationItem, userId: String): Boolean {
        if (userId.isBlank()) return true
        if (item.userId == userId || item.userId == "ALL" || item.userId.isBlank()) return true
        if (item.category.equals("OFFER", ignoreCase = true) || item.category.equals("PROMOTION", ignoreCase = true)) return true
        return false
    }

    fun createNotification(
        notification: NotificationItem,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val id = if (notification.id.isNotEmpty()) notification.id else "notif_${System.currentTimeMillis()}"
        val toSave = notification.copy(id = id)

        // Save locally first for instant availability
        UserSessionManager.saveNotification(toSave)

        val docRef = db.collection(NOTIFICATIONS).document(id)
        docRef.set(toSave.toMap(), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                // Local save already succeeded
                onSuccess()
            }
    }

    /**
     * Dispatch an OFFER notification when a business owner publishes or activates a promotion.
     * Delivers to all customers locally and in cloud Firestore.
     */
    fun dispatchPromotionNotification(
        promotion: Promotion,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val promoDocId = "notif_promo_${promotion.id}"
        val titleText = if (promotion.title.isNotBlank()) "Special Offer: ${promotion.title}" else "Special Offer Available"
        val bizName = promotion.businessName.ifBlank { "BizBridge Partner" }
        val offerDetails = listOfNotNull(
            promotion.discountText.takeIf { it.isNotBlank() },
            promotion.description.takeIf { it.isNotBlank() }
        ).joinToString(" - ")
        val messageText = if (offerDetails.isNotBlank()) "$bizName: $offerDetails" else "$bizName announced a new exclusive offer!"

        val broadcastNotif = NotificationItem(
            id = promoDocId,
            userId = "ALL",
            title = titleText,
            message = messageText,
            category = "OFFER",
            relatedBusinessId = promotion.businessId,
            isRead = false,
            createdAt = Timestamp.now()
        )

        // 1. Save broadcast locally for offline & instant customer access
        UserSessionManager.saveNotification(broadcastNotif)

        // 2. Also register copy for known registered customer accounts
        val registered = UserSessionManager.getAllRegisteredAccounts()
        for (acc in registered) {
            val role = acc["role"] as? String ?: ""
            if (role.equals("CUSTOMER", ignoreCase = true)) {
                val custUid = (acc["uid"] as? String)?.takeIf { it.isNotBlank() }
                    ?: "cust_${(acc["email"] as? String ?: "").hashCode()}"
                val custNotif = broadcastNotif.copy(
                    id = "${promoDocId}_$custUid",
                    userId = custUid
                )
                UserSessionManager.saveNotification(custNotif)
                db.collection(NOTIFICATIONS).document(custNotif.id).set(custNotif.toMap(), SetOptions.merge())
            }
        }

        // 3. Save broadcast document to Firestore under "notifications"
        db.collection(NOTIFICATIONS).document(promoDocId)
            .set(broadcastNotif.toMap(), SetOptions.merge())
            .addOnSuccessListener {
                // Sync to all cloud users whose role is CUSTOMER
                db.collection("users")
                    .whereEqualTo("role", "CUSTOMER")
                    .get()
                    .addOnSuccessListener { snap ->
                        if (!snap.isEmpty) {
                            val batch = db.batch()
                            for (doc in snap.documents) {
                                val uId = doc.id
                                val uNotif = broadcastNotif.copy(id = "${promoDocId}_$uId", userId = uId)
                                batch.set(db.collection(NOTIFICATIONS).document(uNotif.id), uNotif.toMap(), SetOptions.merge())
                            }
                            batch.commit().addOnCompleteListener { onSuccess() }
                        } else {
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { onSuccess() }
            }
            .addOnFailureListener {
                // Local save already succeeded
                onSuccess()
            }
    }

    fun getNotificationsForUser(
        userId: String,
        onSuccess: (List<NotificationItem>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val local = UserSessionManager.getAllNotifications()
            .filter { isRelevantForUser(it, userId) }

        if (userId.isBlank()) {
            db.collection(NOTIFICATIONS)
                .whereEqualTo("category", "OFFER")
                .get()
                .addOnSuccessListener { snapshot ->
                    val offerDocs = snapshot.documents.mapNotNull { it.toObject(NotificationItem::class.java)?.copy(id = it.id) }
                    val merged = (local + offerDocs).distinctBy { it.id }.sortedByDescending { it.createdAt }
                    onSuccess(merged)
                }
                .addOnFailureListener {
                    onSuccess(local)
                }
            return
        }

        db.collection(NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { userSnap ->
                val userDocs = userSnap.documents.mapNotNull { it.toObject(NotificationItem::class.java)?.copy(id = it.id) }
                db.collection(NOTIFICATIONS)
                    .whereEqualTo("category", "OFFER")
                    .get()
                    .addOnSuccessListener { offerSnap ->
                        val offerDocs = offerSnap.documents.mapNotNull { it.toObject(NotificationItem::class.java)?.copy(id = it.id) }
                        val merged = (local + userDocs + offerDocs).distinctBy { it.id }.sortedByDescending { it.createdAt }
                        onSuccess(merged)
                    }
                    .addOnFailureListener {
                        val merged = (local + userDocs).distinctBy { it.id }.sortedByDescending { it.createdAt }
                        onSuccess(merged)
                    }
            }
            .addOnFailureListener {
                onSuccess(local)
            }
    }

    fun getUnreadCount(
        userId: String,
        onSuccess: (Int) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val localCount = UserSessionManager.getAllNotifications()
            .count { isRelevantForUser(it, userId) && !it.isRead }

        if (userId.isBlank()) {
            onSuccess(localCount)
            return
        }

        db.collection(NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val userUnreadIds = snapshot.documents.map { it.id }.toSet()
                db.collection(NOTIFICATIONS)
                    .whereEqualTo("category", "OFFER")
                    .whereEqualTo("isRead", false)
                    .get()
                    .addOnSuccessListener { offerSnap ->
                        val offerUnreadIds = offerSnap.documents.map { it.id }.toSet()
                        val localUnreadIds = UserSessionManager.getAllNotifications()
                            .filter { isRelevantForUser(it, userId) && !it.isRead }
                            .map { it.id }
                            .toSet()
                        val total = (userUnreadIds + offerUnreadIds + localUnreadIds).size
                        onSuccess(total)
                    }
                    .addOnFailureListener {
                        val total = Math.max(localCount, snapshot.size())
                        onSuccess(total)
                    }
            }
            .addOnFailureListener {
                onSuccess(localCount)
            }
    }

    fun markAllAsRead(
        userId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        UserSessionManager.markAllNotificationsRead(userId)

        if (userId.isBlank()) {
            onSuccess()
            return
        }

        db.collection(NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onSuccess() }
            }
            .addOnFailureListener {
                onSuccess()
            }
    }

    fun markAsRead(
        notificationId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        UserSessionManager.markNotificationRead(notificationId)

        db.collection(NOTIFICATIONS).document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }

    /**
     * Real-time listener for user notifications including OFFER broadcast alerts.
     */
    fun listenToNotificationsForUser(
        userId: String,
        onUpdate: (List<NotificationItem>) -> Unit
    ): ListenerRegistration {
        var cachedUserDocs: List<NotificationItem> = emptyList()
        var cachedOfferDocs: List<NotificationItem> = emptyList()

        fun deliverMerged() {
            val localCurrent = UserSessionManager.getAllNotifications().filter { isRelevantForUser(it, userId) }
            val merged = (localCurrent + cachedUserDocs + cachedOfferDocs)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
            onUpdate(merged)
        }

        // Initial local delivery
        deliverMerged()

        val registrations = mutableListOf<ListenerRegistration>()

        // 1. Listen for user-specific notifications if userId is not blank
        if (userId.isNotBlank()) {
            val userReg = db.collection(NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    cachedUserDocs = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    deliverMerged()
                }
            registrations.add(userReg)
        }

        // 2. Real-time listener for OFFER notifications so customer alerts arrive immediately
        val offerReg = db.collection(NOTIFICATIONS)
            .whereEqualTo("category", "OFFER")
            .addSnapshotListener { snapshot, _ ->
                cachedOfferDocs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                deliverMerged()
            }
        registrations.add(offerReg)

        return object : ListenerRegistration {
            override fun remove() {
                for (r in registrations) {
                    try {
                        r.remove()
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
