package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val NOTIFICATIONS = "notifications"

    fun createNotification(
        notification: NotificationItem,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val id = if (notification.id.isNotEmpty()) notification.id else "notif_${System.currentTimeMillis()}"
        val toSave = notification.copy(id = id)

        // Save locally first for instant availability
        com.example.bizbridgeapp.UserSessionManager.saveNotification(toSave)

        val docRef = db.collection(NOTIFICATIONS).document(id)
        docRef.set(toSave.toMap(), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                // Local save already succeeded
                onSuccess()
            }
    }

    fun getNotificationsForUser(
        userId: String,
        onSuccess: (List<NotificationItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val local = com.example.bizbridgeapp.UserSessionManager.getAllNotifications()
            .filter { it.userId == userId || userId.isEmpty() }

        db.collection(NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val firestoreList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                }
                val merged = (local + firestoreList).distinctBy { it.id }
                    .sortedByDescending { it.createdAt }
                onSuccess(merged)
            }
            .addOnFailureListener { e ->
                onSuccess(local)
            }
    }

    fun getUnreadCount(
        userId: String,
        onSuccess: (Int) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val localCount = com.example.bizbridgeapp.UserSessionManager.getAllNotifications()
            .count { (it.userId == userId || userId.isEmpty()) && !it.isRead }

        db.collection(NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val total = Math.max(localCount, snapshot.size())
                onSuccess(total)
            }
            .addOnFailureListener {
                onSuccess(localCount)
            }
    }

    fun markAllAsRead(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        com.example.bizbridgeapp.UserSessionManager.markAllNotificationsRead(userId)

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
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        com.example.bizbridgeapp.UserSessionManager.markNotificationRead(notificationId)

        db.collection(NOTIFICATIONS).document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }

    /**
     * Real-time listener for user notifications.
     */
    fun listenToNotificationsForUser(
        userId: String,
        onUpdate: (List<NotificationItem>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val local = com.example.bizbridgeapp.UserSessionManager.getAllNotifications()
            .filter { it.userId == userId || userId.isEmpty() }
        if (local.isNotEmpty()) {
            onUpdate(local)
        }

        return db.collection(NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                val localCurrent = com.example.bizbridgeapp.UserSessionManager.getAllNotifications()
                    .filter { it.userId == userId || userId.isEmpty() }
                if (error != null || snapshot == null) {
                    onUpdate(localCurrent)
                    return@addSnapshotListener
                }
                val firestoreList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                }
                val merged = (localCurrent + firestoreList).distinctBy { it.id }
                    .sortedByDescending { it.createdAt }
                onUpdate(merged)
            }
    }
}
