package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.Service
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object ServiceRepository {
    private val db = FirebaseFirestore.getInstance()

    val defaultDemoServices = listOf(
        Service(
            id = "srv_haircut",
            name = "Signature haircut",
            description = "Consultation, cut and effortless finish.",
            price = 45.0,
            durationMinutes = 45,
            isActive = true
        ),
        Service(
            id = "srv_color",
            name = "Color refresh",
            description = "Gloss, tone and a head-turn adding finish.",
            price = 65.0,
            durationMinutes = 50,
            isActive = true
        ),
        Service(
            id = "srv_blowdry",
            name = "Blow-dry & style",
            description = "Smooth, bouncy styling with a relaxed finish.",
            price = 35.0,
            durationMinutes = 30,
            isActive = true
        )
    )

    fun seedDefaultServicesIfEmpty(businessId: String, onComplete: (List<Service>) -> Unit = {}) {
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        val colRef = db.collection("businesses").document(targetBizId).collection("services")
        colRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val batch = db.batch()
                val seededList = defaultDemoServices.map { srv ->
                    val docRef = colRef.document(srv.id)
                    val toSave = srv.copy(businessId = targetBizId)
                    batch.set(docRef, toSave.toMap())
                    toSave
                }
                batch.commit()
                    .addOnSuccessListener { onComplete(seededList) }
                    .addOnFailureListener { onComplete(seededList) }
            } else {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Service::class.java)?.copy(id = doc.id, businessId = targetBizId)
                }
                onComplete(list)
            }
        }.addOnFailureListener {
            onComplete(defaultDemoServices.map { it.copy(businessId = targetBizId) })
        }
    }

    /** Real-time listener for all services belonging to a business */
    fun listenToServicesForBusiness(
        businessId: String,
        onUpdate: (List<Service>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        val colRef = db.collection("businesses").document(targetBizId).collection("services")

        return colRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                if (targetBizId == "biz_juniper_studio") {
                    onUpdate(defaultDemoServices.map { it.copy(businessId = targetBizId) })
                } else {
                    onUpdate(emptyList())
                }
                return@addSnapshotListener
            }
            if (snapshot.isEmpty) {
                if (targetBizId == "biz_juniper_studio") {
                    seedDefaultServicesIfEmpty(targetBizId) { seeded ->
                        onUpdate(seeded)
                    }
                } else {
                    onUpdate(emptyList())
                }
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
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        db.collection("businesses").document(targetBizId).collection("services")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    if (targetBizId == "biz_juniper_studio") {
                        seedDefaultServicesIfEmpty(targetBizId) { seeded ->
                            onSuccess(seeded.filter { it.isActive })
                        }
                    } else {
                        onSuccess(emptyList())
                    }
                } else {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val service = doc.toObject(Service::class.java)
                        service?.copy(id = doc.id, businessId = targetBizId)
                    }
                    onSuccess(list)
                }
            }
            .addOnFailureListener { e ->
                if (targetBizId == "biz_juniper_studio") {
                    onSuccess(defaultDemoServices.map { it.copy(businessId = targetBizId) })
                } else {
                    onSuccess(emptyList())
                }
            }
    }

    fun getServiceById(
        businessId: String,
        serviceId: String,
        onSuccess: (Service?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        db.collection("businesses").document(targetBizId).collection("services").document(serviceId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val s = doc.toObject(Service::class.java)?.copy(id = doc.id, businessId = targetBizId)
                    onSuccess(s)
                } else {
                    val fallback = defaultDemoServices.firstOrNull { it.id == serviceId }?.copy(businessId = targetBizId)
                    onSuccess(fallback)
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
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
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
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
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
        val targetBizId = if (businessId.isBlank()) "biz_juniper_studio" else businessId
        db.collection("businesses").document(targetBizId).collection("services").document(serviceId)
            .update(mapOf(
                "isActive" to isActive,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
