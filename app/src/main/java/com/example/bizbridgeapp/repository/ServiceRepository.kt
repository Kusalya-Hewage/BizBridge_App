package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.Service
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object ServiceRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getServicesForBusiness(
        businessId: String,
        onSuccess: (List<Service>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("businesses").document(businessId).collection("services")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    val service = doc.toObject(Service::class.java)
                    service?.copy(id = doc.id, businessId = businessId)
                }
                onSuccess(list)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getServiceById(
        businessId: String,
        serviceId: String,
        onSuccess: (Service?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("businesses").document(businessId).collection("services").document(serviceId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val s = doc.toObject(Service::class.java)?.copy(id = doc.id, businessId = businessId)
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
        val docRef = if (service.id.isNotEmpty()) {
            db.collection("businesses").document(businessId).collection("services").document(service.id)
        } else {
            db.collection("businesses").document(businessId).collection("services").document()
        }

        val toSave = service.copy(id = docRef.id, businessId = businessId)
        docRef.set(toSave.toMap(), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
