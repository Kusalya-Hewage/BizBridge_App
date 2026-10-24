package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.Announcement
import com.example.bizbridgeapp.model.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object AnnouncementRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val ANNOUNCEMENTS = "announcements"

    /** Listen to announcements by audience (ALL, CUSTOMERS, OWNERS) */
    fun listenToAnnouncements(audience: String, onUpdate: (List<Announcement>) -> Unit): ListenerRegistration {
        val query = if (audience == "ALL") {
            db.collection(ANNOUNCEMENTS).orderBy("createdAt", Query.Direction.DESCENDING)
        } else {
            db.collection(ANNOUNCEMENTS)
                .whereIn("audience", listOf("ALL", audience))
                .orderBy("createdAt", Query.Direction.DESCENDING)
        }
        return query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { it.toObject(Announcement::class.java)?.copy(id = it.id) }
            onUpdate(list)
        }
    }

    /** Create and publish announcement (Admin only) */
    fun publishAnnouncement(
        announcement: Announcement,
        onSuccess: (id: String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = if (announcement.id.isNotEmpty()) db.collection(ANNOUNCEMENTS).document(announcement.id) else db.collection(ANNOUNCEMENTS).document()
        val toSave = announcement.copy(id = docRef.id)
        docRef.set(toSave.toMap())
            .addOnSuccessListener {
                onSuccess(docRef.id)
            }
            .addOnFailureListener { onFailure(it) }
    }
}
