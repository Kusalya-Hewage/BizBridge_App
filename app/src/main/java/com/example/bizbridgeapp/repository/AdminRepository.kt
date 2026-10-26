package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.AdminAuditLog
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.BusinessApplication
import com.example.bizbridgeapp.model.NotificationItem
import com.example.bizbridgeapp.model.Report
import com.example.bizbridgeapp.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class AdminOverviewStats(
    val totalUsers: Int = 0,
    val totalBusinesses: Int = 0,
    val pendingApplications: Int = 0,
    val verifiedBusinesses: Int = 0,
    val openReports: Int = 0,
    val reviewReportsCount: Int = 0,
    val listingReportsCount: Int = 0,
    val totalBookings: Int = 0,
    val pendingBookings: Int = 0,
    val confirmedBookings: Int = 0,
    val completedBookings: Int = 0,
    val cancelledBookings: Int = 0,
    val pendingDocuments: Int = 0
)

object AdminRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val APPLICATIONS = "businessApplications"
    private const val BUSINESSES = "businesses"
    private const val USERS = "users"
    private const val REPORTS = "reports"
    private const val AUDIT_LOGS = "adminAuditLogs"
    private const val BOOKINGS = "bookings"

    /** Return current locally available overview stats synchronously to eliminate initial load flash */
    fun getLocalOverviewStats(): AdminOverviewStats {
        val localBiz = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
        val totalBiz = localBiz.size
        val verifiedBiz = localBiz.count { it.verificationStatus == "VERIFIED" || it.approved }
        val localPendingApps = com.example.bizbridgeapp.UserSessionManager.getAllApplications().count { it.status.equals("PENDING", ignoreCase = true) }
        val localPendingDocs = com.example.bizbridgeapp.UserSessionManager.getAllQualificationDocuments().count { it.verificationStatus.equals("PENDING", ignoreCase = true) }
        val localUsers = maxOf(com.example.bizbridgeapp.UserSessionManager.getAllRegisteredAccounts().size, if (totalBiz > 0) 1 else 0)
        return AdminOverviewStats(
            totalUsers = localUsers,
            totalBusinesses = totalBiz,
            pendingApplications = localPendingApps,
            verifiedBusinesses = verifiedBiz,
            openReports = 0,
            reviewReportsCount = 0,
            listingReportsCount = 0,
            totalBookings = 0,
            pendingBookings = 0,
            confirmedBookings = 0,
            completedBookings = 0,
            cancelledBookings = 0,
            pendingDocuments = localPendingDocs
        )
    }

    /** Listen to real-time Admin Overview statistics */
    fun listenToOverviewStats(onUpdate: (AdminOverviewStats) -> Unit): ListenerRegistration {
        // Emit local stats immediately so no placeholder or delay is visible
        try {
            onUpdate(getLocalOverviewStats())
        } catch (_: Exception) {}

        return db.collection(BUSINESSES).addSnapshotListener { bizSnap, _ ->
            val firestoreBiz = bizSnap?.documents?.mapNotNull { it.toObject(Business::class.java) } ?: emptyList()
            val localBiz = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
            val allBiz = (firestoreBiz + localBiz).distinctBy { it.id }
            val totalBiz = allBiz.size
            val verifiedBiz = allBiz.count { it.verificationStatus == "VERIFIED" || it.approved }

            val localPendingApps = com.example.bizbridgeapp.UserSessionManager.getAllApplications().count { it.status.equals("PENDING", ignoreCase = true) }
            val localPendingDocs = com.example.bizbridgeapp.UserSessionManager.getAllQualificationDocuments().count { it.verificationStatus.equals("PENDING", ignoreCase = true) }
            val localUsers = com.example.bizbridgeapp.UserSessionManager.getAllRegisteredAccounts().size

            db.collection(APPLICATIONS).whereEqualTo("status", "PENDING").get()
                .addOnSuccessListener { appSnap ->
                    val pendingApps = maxOf(appSnap.size(), localPendingApps)

                    db.collection(USERS).get().addOnSuccessListener { userSnap ->
                        val totalUsers = maxOf(userSnap.size(), localUsers)

                        db.collection(REPORTS).whereIn("status", listOf("OPEN", "PENDING")).get()
                            .addOnSuccessListener { repSnap ->
                                val openReports = repSnap.size()
                                val reviewReports = repSnap.documents.count { (it.getString("targetType") ?: "").equals("REVIEW", ignoreCase = true) }
                                val listingReports = maxOf(0, openReports - reviewReports)

                                db.collection(BOOKINGS).get().addOnSuccessListener { bkSnap ->
                                    val bookings = bkSnap.documents.mapNotNull { it.toObject(com.example.bizbridgeapp.model.Booking::class.java) }
                                    val totalBookings = bookings.size
                                    val pendingBookings = bookings.count { it.status == "PENDING" }
                                    val confirmedBookings = bookings.count { it.status == "CONFIRMED" }
                                    val completedBookings = bookings.count { it.status == "COMPLETED" }
                                    val cancelledBookings = bookings.count { it.status == "CANCELLED" }

                                    onUpdate(
                                        AdminOverviewStats(
                                            totalUsers = totalUsers,
                                            totalBusinesses = totalBiz,
                                            pendingApplications = pendingApps,
                                            verifiedBusinesses = verifiedBiz,
                                            openReports = openReports,
                                            reviewReportsCount = reviewReports,
                                            listingReportsCount = listingReports,
                                            totalBookings = totalBookings,
                                            pendingBookings = pendingBookings,
                                            confirmedBookings = confirmedBookings,
                                            completedBookings = completedBookings,
                                            cancelledBookings = cancelledBookings,
                                            pendingDocuments = localPendingDocs
                                        )
                                    )
                                }.addOnFailureListener {
                                    onUpdate(
                                        AdminOverviewStats(
                                            totalUsers = totalUsers,
                                            totalBusinesses = totalBiz,
                                            pendingApplications = pendingApps,
                                            verifiedBusinesses = verifiedBiz,
                                            openReports = openReports,
                                            reviewReportsCount = reviewReports,
                                            listingReportsCount = listingReports,
                                            pendingDocuments = localPendingDocs
                                        )
                                    )
                                }
                            }.addOnFailureListener {
                                onUpdate(
                                    AdminOverviewStats(
                                        totalUsers = totalUsers,
                                        totalBusinesses = totalBiz,
                                        pendingApplications = pendingApps,
                                        verifiedBusinesses = verifiedBiz,
                                        pendingDocuments = localPendingDocs
                                    )
                                )
                            }
                    }.addOnFailureListener {
                        onUpdate(
                            AdminOverviewStats(
                                totalUsers = localUsers,
                                totalBusinesses = totalBiz,
                                pendingApplications = pendingApps,
                                verifiedBusinesses = verifiedBiz,
                                pendingDocuments = localPendingDocs
                            )
                        )
                    }
                }.addOnFailureListener {
                    onUpdate(
                        AdminOverviewStats(
                            totalUsers = localUsers,
                            totalBusinesses = totalBiz,
                            pendingApplications = localPendingApps,
                            verifiedBusinesses = verifiedBiz,
                            pendingDocuments = localPendingDocs
                        )
                    )
                }
        }
    }

    /** Listen to all business applications (Real-Time) */
    /** Listen to all business applications (Real-Time) */
    fun listenToApplications(onUpdate: (List<BusinessApplication>) -> Unit): ListenerRegistration {
        return db.collection(APPLICATIONS)
            .addSnapshotListener { snapshot, _ ->
                val firestoreList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BusinessApplication::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                val localList = com.example.bizbridgeapp.UserSessionManager.getAllApplications()

                // Also check if any business has PENDING approval status but no application
                db.collection(BUSINESSES).whereEqualTo("approvalStatus", "PENDING").get()
                    .addOnSuccessListener { bizSnaps ->
                        val pendingBizApps = bizSnaps.documents.mapNotNull { bDoc ->
                            val bId = bDoc.id
                            val bName = bDoc.getString("name") ?: "Unnamed Business"
                            val oId = bDoc.getString("ownerId") ?: ""
                            val cat = bDoc.getString("category") ?: "Services"
                            val addr = bDoc.getString("address") ?: "Colombo, Sri Lanka"
                            val phone = bDoc.getString("contactNumber") ?: ""
                            BusinessApplication(
                                id = "app_${bId.removePrefix("biz_")}",
                                businessId = bId,
                                ownerId = oId,
                                businessName = bName,
                                ownerName = "$bName Owner",
                                email = "owner@bizbridge.example",
                                contactNumber = phone,
                                category = cat,
                                address = addr,
                                status = "PENDING"
                            )
                        }

                        val merged = (firestoreList + localList + pendingBizApps)
                            .distinctBy { it.businessId.ifEmpty { it.id } }
                            .sortedWith(
                                compareBy<BusinessApplication> {
                                    if (it.status.equals("PENDING", ignoreCase = true)) 0 else 1
                                }.thenByDescending { it.id }
                            )
                        onUpdate(merged)
                    }
                    .addOnFailureListener {
                        val merged = (firestoreList + localList)
                            .distinctBy { it.businessId.ifEmpty { it.id } }
                            .sortedWith(
                                compareBy<BusinessApplication> {
                                    if (it.status.equals("PENDING", ignoreCase = true)) 0 else 1
                                }.thenByDescending { it.id }
                            )
                        onUpdate(merged)
                    }
            }
    }

    /** Fetch a single application by ID */
    fun getApplicationById(id: String, onSuccess: (BusinessApplication?) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection(APPLICATIONS).document(id).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val app = snap.toObject(BusinessApplication::class.java)?.copy(id = snap.id)
                    onSuccess(app)
                } else {
                    val local = com.example.bizbridgeapp.UserSessionManager.getAllApplications()
                        .firstOrNull { it.id == id || it.businessId == id }
                    if (local != null) {
                        onSuccess(local)
                    } else {
                        // Check business directly
                        db.collection(BUSINESSES).document(id).get()
                            .addOnSuccessListener { bSnap ->
                                if (bSnap.exists()) {
                                    val b = bSnap.toObject(Business::class.java)?.copy(id = bSnap.id)
                                    if (b != null) {
                                        onSuccess(
                                            BusinessApplication(
                                                id = "app_${b.id.removePrefix("biz_")}",
                                                businessId = b.id,
                                                ownerId = b.ownerId,
                                                businessName = b.name,
                                                ownerName = "${b.name} Owner",
                                                email = "owner@bizbridge.example",
                                                contactNumber = b.contactNumber,
                                                category = b.category,
                                                address = b.address,
                                                status = b.approvalStatus
                                            )
                                        )
                                        return@addOnSuccessListener
                                    }
                                }
                                onSuccess(null)
                            }
                            .addOnFailureListener { onFailure(it) }
                    }
                }
            }
            .addOnFailureListener {
                val local = com.example.bizbridgeapp.UserSessionManager.getAllApplications()
                    .firstOrNull { it.id == id || it.businessId == id }
                onSuccess(local)
            }
    }

    /** Approve a business application atomically */
    fun approveApplication(
        applicationId: String,
        adminUid: String,
        adminEmail: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // First retrieve application to know businessId
        getApplicationById(
            id = applicationId,
            onSuccess = { app ->
                val finalApp = app ?: BusinessApplication(
                    id = applicationId,
                    businessId = if (applicationId.startsWith("biz_")) applicationId else "biz_${applicationId.removePrefix("app_")}",
                    businessName = "Business",
                    status = "PENDING"
                )

                val targetBizId = finalApp.businessId.ifBlank {
                    if (applicationId.startsWith("biz_")) applicationId else "biz_${applicationId.removePrefix("app_")}"
                }

                // Update local session & cache immediately
                com.example.bizbridgeapp.UserSessionManager.updateApplicationStatus(applicationId, "APPROVED")
                com.example.bizbridgeapp.UserSessionManager.updateApplicationStatus(targetBizId, "APPROVED")
                com.example.bizbridgeapp.UserSessionManager.updateBusinessApprovalStatus(
                    businessId = targetBizId,
                    approved = true,
                    active = true,
                    approvalStatus = "APPROVED",
                    verificationStatus = "VERIFIED",
                    name = finalApp.businessName,
                    category = finalApp.category,
                    address = finalApp.address,
                    ownerId = finalApp.ownerId,
                    contactNumber = finalApp.contactNumber
                )

                // Also auto-verify all attached qualification documents for this business in the same approval!
                val attachedDocs = com.example.bizbridgeapp.UserSessionManager.getQualificationsForBusiness(targetBizId, finalApp.ownerId)
                for (doc in attachedDocs) {
                    com.example.bizbridgeapp.UserSessionManager.updateQualificationStatus(doc.id, "VERIFIED")
                }
                TrustScoreRepository.recalculateTrustScore(targetBizId)

                val appRef = db.collection(APPLICATIONS).document(finalApp.id)
                val bizRef = db.collection(BUSINESSES).document(targetBizId)

                val batch = db.batch()
                for (doc in attachedDocs) {
                    val qDocRef = db.collection("qualificationVerifications").document(doc.id)
                    batch.set(
                        qDocRef,
                        mapOf(
                            "verificationStatus" to "VERIFIED",
                            "verifiedAt" to FieldValue.serverTimestamp(),
                            "verifiedBy" to adminEmail
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
                batch.set(
                    appRef,
                    mapOf(
                        "status" to "APPROVED",
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "reviewedBy" to adminEmail
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                val bizData = mutableMapOf<String, Any>(
                    "id" to targetBizId,
                    "approved" to true,
                    "active" to true,
                    "approvalStatus" to "APPROVED",
                    "verificationStatus" to "VERIFIED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (finalApp.businessName.isNotBlank()) bizData["name"] = finalApp.businessName
                if (finalApp.category.isNotBlank()) bizData["category"] = finalApp.category
                if (finalApp.address.isNotBlank()) bizData["address"] = finalApp.address
                if (finalApp.ownerId.isNotBlank()) bizData["ownerId"] = finalApp.ownerId
                if (finalApp.contactNumber.isNotBlank()) bizData["contactNumber"] = finalApp.contactNumber

                batch.set(bizRef, bizData, com.google.firebase.firestore.SetOptions.merge())

                // Audit log
                val logRef = db.collection(AUDIT_LOGS).document()
                val log = AdminAuditLog(
                    id = logRef.id,
                    adminUid = adminUid,
                    adminEmail = adminEmail,
                    actionType = "APPROVE_BUSINESS",
                    targetType = "APPLICATION",
                    targetId = applicationId,
                    previousStatus = finalApp.status,
                    newStatus = "APPROVED",
                    reason = "Application reviewed and verified by administrator"
                )
                com.example.bizbridgeapp.UserSessionManager.saveAuditLog(log)
                batch.set(logRef, log.toMap())

                batch.commit().addOnSuccessListener {
                    if (finalApp.ownerId.isNotEmpty()) {
                        NotificationRepository.createNotification(
                            NotificationItem(
                                userId = finalApp.ownerId,
                                title = "Business Approved!",
                                message = "Congratulations! Your business '${finalApp.businessName}' has been approved and is now live on BizBridge.",
                                category = "SYSTEM",
                                relatedBusinessId = targetBizId
                            ),
                            onSuccess = {},
                            onFailure = {}
                        )
                    }
                    onSuccess()
                }.addOnFailureListener {
                    // Even if Firestore batch failed due to permissions/offline, local state is approved
                    onSuccess()
                }
            },
            onFailure = {
                onFailure(it)
            }
        )
    }

    /** Reject a business application with reason */
    fun rejectApplication(
        applicationId: String,
        adminUid: String,
        adminEmail: String,
        reason: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getApplicationById(
            id = applicationId,
            onSuccess = { app ->
                val finalApp = app ?: BusinessApplication(
                    id = applicationId,
                    businessId = if (applicationId.startsWith("biz_")) applicationId else "biz_${applicationId.removePrefix("app_")}",
                    businessName = "Business",
                    status = "PENDING"
                )
                val targetBizId = finalApp.businessId.ifBlank {
                    if (applicationId.startsWith("biz_")) applicationId else "biz_${applicationId.removePrefix("app_")}"
                }

                com.example.bizbridgeapp.UserSessionManager.updateApplicationStatus(applicationId, "REJECTED", reason)
                com.example.bizbridgeapp.UserSessionManager.updateApplicationStatus(targetBizId, "REJECTED", reason)
                com.example.bizbridgeapp.UserSessionManager.updateBusinessApprovalStatus(
                    businessId = targetBizId,
                    approved = false,
                    active = false,
                    approvalStatus = "REJECTED",
                    verificationStatus = "PENDING"
                )

                val appRef = db.collection(APPLICATIONS).document(finalApp.id)
                val bizRef = db.collection(BUSINESSES).document(targetBizId)

                val batch = db.batch()
                batch.set(
                    appRef,
                    mapOf(
                        "status" to "REJECTED",
                        "rejectionReason" to reason,
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "reviewedBy" to adminEmail
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                batch.set(
                    bizRef,
                    mapOf(
                        "approved" to false,
                        "active" to false,
                        "approvalStatus" to "REJECTED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                val logRef = db.collection(AUDIT_LOGS).document()
                val log = AdminAuditLog(
                    id = logRef.id,
                    adminUid = adminUid,
                    adminEmail = adminEmail,
                    actionType = "REJECT_BUSINESS",
                    targetType = "APPLICATION",
                    targetId = applicationId,
                    previousStatus = finalApp.status,
                    newStatus = "REJECTED",
                    reason = reason
                )
                com.example.bizbridgeapp.UserSessionManager.saveAuditLog(log)
                batch.set(logRef, log.toMap())

                batch.commit().addOnSuccessListener {
                    if (finalApp.ownerId.isNotEmpty()) {
                        NotificationRepository.createNotification(
                            NotificationItem(
                                userId = finalApp.ownerId,
                                title = "Application Update",
                                message = "Your registration for '${finalApp.businessName}' was not approved: $reason",
                                category = "SYSTEM",
                                relatedBusinessId = targetBizId
                            ),
                            onSuccess = {},
                            onFailure = {}
                        )
                    }
                    onSuccess()
                }.addOnFailureListener {
                    onSuccess()
                }
            },
            onFailure = { onFailure(it) }
        )
    }

    /** Listen to all users for User Management */
    fun listenToUsers(onUpdate: (List<UserProfile>) -> Unit): ListenerRegistration {
        return db.collection(USERS).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
            }
            onUpdate(list)
        }
    }

    /** Suspend or Activate user */
    fun updateUserStatus(
        userId: String,
        newStatus: String, // ACTIVE, SUSPENDED
        adminUid: String,
        adminEmail: String,
        reason: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userRef = db.collection(USERS).document(userId)
        val logRef = db.collection(AUDIT_LOGS).document()

        val batch = db.batch()
        batch.update(userRef, mapOf(
            "accountStatus" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        ))
        val log = AdminAuditLog(
            id = logRef.id,
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = if (newStatus == "SUSPENDED") "SUSPEND_USER" else "ACTIVATE_USER",
            targetType = "USER",
            targetId = userId,
            newStatus = newStatus,
            reason = reason
        )
        com.example.bizbridgeapp.UserSessionManager.saveAuditLog(log)
        batch.set(logRef, log.toMap())

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /** Listen to all businesses for Business Listings moderation */
    fun listenToAllBusinesses(onUpdate: (List<Business>) -> Unit): ListenerRegistration {
        return db.collection(BUSINESSES).addSnapshotListener { snapshot, error ->
            val localBiz = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses()
            if (error != null || snapshot == null) {
                onUpdate(localBiz)
                return@addSnapshotListener
            }
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Business::class.java)?.copy(id = doc.id)
            }
            val merged = (list + localBiz).distinctBy { it.id }
            onUpdate(merged)
        }
    }

    /** Moderate business status (Active / Inactive) */
    fun updateBusinessStatus(
        businessId: String,
        active: Boolean,
        adminUid: String,
        adminEmail: String,
        reason: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val bizRef = db.collection(BUSINESSES).document(businessId)
        val logRef = db.collection(AUDIT_LOGS).document()

        val batch = db.batch()
        batch.set(
            bizRef,
            mapOf(
                "approved" to active,
                "active" to active,
                "approvalStatus" to (if (active) "APPROVED" else "SUSPENDED"),
                "verificationStatus" to (if (active) "VERIFIED" else "PENDING"),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
        val log = AdminAuditLog(
            id = logRef.id,
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = if (active) "ACTIVATE_LISTING" else "SUSPEND_LISTING",
            targetType = "BUSINESS",
            targetId = businessId,
            newStatus = if (active) "ACTIVE" else "SUSPENDED",
            reason = reason
        )
        com.example.bizbridgeapp.UserSessionManager.saveAuditLog(log)
        batch.set(logRef, log.toMap())

        batch.commit()
            .addOnSuccessListener {
                com.example.bizbridgeapp.UserSessionManager.updateBusinessApprovalStatus(
                    businessId = businessId,
                    approved = active,
                    active = active,
                    approvalStatus = if (active) "APPROVED" else "SUSPENDED",
                    verificationStatus = if (active) "VERIFIED" else "PENDING"
                )
                onSuccess()
            }
            .addOnFailureListener {
                com.example.bizbridgeapp.UserSessionManager.updateBusinessApprovalStatus(
                    businessId = businessId,
                    approved = active,
                    active = active,
                    approvalStatus = if (active) "APPROVED" else "SUSPENDED",
                    verificationStatus = if (active) "VERIFIED" else "PENDING"
                )
                onSuccess()
            }
    }

    /** Record an administrative audit log */
    fun logAdminAction(log: AdminAuditLog, onComplete: () -> Unit = {}) {
        com.example.bizbridgeapp.UserSessionManager.saveAuditLog(log)
        val ref = if (log.id.isNotEmpty()) db.collection(AUDIT_LOGS).document(log.id) else db.collection(AUDIT_LOGS).document()
        ref.set(log.copy(id = ref.id).toMap())
            .addOnCompleteListener { onComplete() }
    }

    /** Listen to recent administrative audit logs */
    fun listenToRecentAuditLogs(
        adminEmail: String? = null,
        onUpdate: (List<AdminAuditLog>) -> Unit
    ): ListenerRegistration {
        val localLogs = com.example.bizbridgeapp.UserSessionManager.getAllAuditLogs()
        if (localLogs.isNotEmpty()) {
            val filtered = if (adminEmail.isNullOrBlank()) localLogs else localLogs.filter { it.adminEmail.equals(adminEmail, ignoreCase = true) }
            onUpdate(filtered)
        }

        return db.collection(AUDIT_LOGS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                val currentLocal = com.example.bizbridgeapp.UserSessionManager.getAllAuditLogs()
                if (error != null || snapshot == null) {
                    val filtered = if (adminEmail.isNullOrBlank()) currentLocal else currentLocal.filter { it.adminEmail.equals(adminEmail, ignoreCase = true) }
                    onUpdate(filtered)
                    return@addSnapshotListener
                }

                val firestoreLogs = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AdminAuditLog::class.java)?.copy(id = doc.id)
                }
                val merged = (firestoreLogs + currentLocal)
                    .distinctBy { it.id }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }

                val filtered = if (adminEmail.isNullOrBlank()) merged else merged.filter { it.adminEmail.equals(adminEmail, ignoreCase = true) }
                onUpdate(filtered)
            }
    }
}
