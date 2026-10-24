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
        val docRef = if (notification.id.isNotEmpty()) {
            db.collection(NOTIFICATIONS).document(notification.id)
        } else {
            db.collection(NOTIFICATIONS).document()
        }

        val toSave = notification.copy(id = docRef.id)
        docRef.set(toSave.toMap(), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getNotificationsForUser(
        userId: String,
        onSuccess: (List<NotificationItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt }
                onSuccess(list)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getUnreadCount(
        userId: String,
        onSuccess: (Int) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.size())
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun markAllAsRead(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
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
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun markAsRead(
        notificationId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(NOTIFICATIONS).document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
