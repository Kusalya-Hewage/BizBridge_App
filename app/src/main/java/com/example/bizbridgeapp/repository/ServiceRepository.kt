package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.Service
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object ServiceRepository {
    private val db = FirebaseFirestore.getInstance()

    val defaultDemoServices = emptyList<Service>()

    fun seedDefaultServicesIfEmpty(businessId: String, onComplete: (List<Service>) -> Unit = {}) {
        onComplete(emptyList())
    }

    /** Real-time listener for all services belonging to a business */
    fun listenToServicesForBusiness(
        businessId: String,
        onUpdate: (List<Service>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val targetBizId = businessId
        val colRef = db.collection("businesses").document(targetBizId).collection("services")

        return colRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                onUpdate(emptyList())
                return@addSnapshotListener
            }
            if (snapshot.isEmpty) {
                onUpdate(emptyList())
            } else {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Service::class.java)?.copy(id = doc.id, businessId = targetBizId)
                }
                onUpdate(list)
            }
        }
    }

    fun getServicesForBusiness(
        businessId: String,
        onSuccess: (List<Service>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val targetBizId = businessId
        db.collection("businesses").document(targetBizId).collection("services")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onSuccess(emptyList())
                } else {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val service = doc.toObject(Service::class.java)
                        service?.copy(id = doc.id, businessId = targetBizId)
                    }
                    onSuccess(list)
                }
            }
            .addOnFailureListener { e ->
                onSuccess(emptyList())
            }
    }

    fun getServiceById(
        businessId: String,
        serviceId: String,
        onSuccess: (Service?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val targetBizId = businessId
        db.collection("businesses").document(targetBizId).collection("services").document(serviceId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val s = doc.toObject(Service::class.java)?.copy(id = doc.id, businessId = targetBizId)
                    onSuccess(s)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun createOrUpdateService(
        businessId: String,
        service: Service,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val targetBizId = businessId
        val docRef = if (service.id.isNotEmpty()) {
            db.collection("businesses").document(targetBizId).collection("services").document(service.id)
        } else {
            db.collection("businesses").document(targetBizId).collection("services").document()
        }

        val toSave = service.copy(id = docRef.id, businessId = targetBizId)
        docRef.set(toSave.toMap(), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteService(
        businessId: String,
        serviceId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val targetBizId = businessId
        db.collection("businesses").document(targetBizId).collection("services").document(serviceId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun toggleServiceStatus(
        businessId: String,
        serviceId: String,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val targetBizId = businessId
        db.collection("businesses").document(targetBizId).collection("services").document(serviceId)
            .update(mapOf(
                "isActive" to isActive,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
