package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.UserSessionManager
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
        val targetBizId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        
        // Immediately emit local cached services so UI is fast and offline-ready
        val initialLocal = UserSessionManager.getServicesForBusiness(targetBizId)
        if (initialLocal.isNotEmpty()) {
            onUpdate(initialLocal)
        }

        val colRef = db.collection("businesses").document(targetBizId).collection("services")
        return colRef.addSnapshotListener { snapshot, error ->
            val localServices = UserSessionManager.getServicesForBusiness(targetBizId)
            if (error != null || snapshot == null) {
                onUpdate(localServices)
                return@addSnapshotListener
            }
            if (snapshot.isEmpty) {
                onUpdate(localServices)
            } else {
                val cloudList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Service::class.java)?.copy(id = doc.id, businessId = targetBizId)
                }
                val merged = (localServices + cloudList).distinctBy { it.id }
                onUpdate(merged)
            }
        }
    }

    fun getServicesForBusiness(
        businessId: String,
        onSuccess: (List<Service>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val targetBizId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val localList = UserSessionManager.getServicesForBusiness(targetBizId).filter { it.isActive }

        db.collection("businesses").document(targetBizId).collection("services")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onSuccess(localList)
                } else {
                    val cloudList = snapshot.documents.mapNotNull { doc ->
                        val service = doc.toObject(Service::class.java)
                        service?.copy(id = doc.id, businessId = targetBizId)
                    }
                    val merged = (localList + cloudList).distinctBy { it.id }
                    onSuccess(merged)
                }
            }
            .addOnFailureListener { e ->
                // Fallback to local services if network or PERMISSION_DENIED occurs
                onSuccess(localList)
            }
    }

    fun getServiceById(
        businessId: String,
        serviceId: String,
        onSuccess: (Service?) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val targetBizId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val local = UserSessionManager.getServicesForBusiness(targetBizId).firstOrNull { it.id == serviceId }
        if (local != null) {
            onSuccess(local)
            return
        }

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
            .addOnFailureListener { e -> 
                onSuccess(local)
            }
    }

    fun createOrUpdateService(
        businessId: String,
        service: Service,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val targetBizId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val docId = if (service.id.isNotEmpty()) service.id else "svc_${System.currentTimeMillis()}"
        val toSave = service.copy(id = docId, businessId = targetBizId)

        // Always save locally immediately so the service is NEVER lost
        UserSessionManager.saveService(toSave)

        val docRef = db.collection("businesses").document(targetBizId).collection("services").document(docId)
        docRef.set(toSave.toMap(), SetOptions.merge())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                // If cloud fails (e.g. PERMISSION_DENIED or offline), succeed locally
                onSuccess()
            }
    }

    fun deleteService(
        businessId: String,
        serviceId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val targetBizId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        UserSessionManager.deleteService(serviceId)

        db.collection("businesses").document(targetBizId).collection("services").document(serviceId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }

    fun toggleServiceStatus(
        businessId: String,
        serviceId: String,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val targetBizId = if (businessId.isNotBlank()) businessId else BusinessRepository.getOwnerBusinessId()
        val local = UserSessionManager.getServicesForBusiness(targetBizId).firstOrNull { it.id == serviceId }
        if (local != null) {
            UserSessionManager.saveService(local.copy(isActive = isActive))
        }

        db.collection("businesses").document(targetBizId).collection("services").document(serviceId)
            .update(mapOf(
                "isActive" to isActive,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onSuccess() }
    }
}
