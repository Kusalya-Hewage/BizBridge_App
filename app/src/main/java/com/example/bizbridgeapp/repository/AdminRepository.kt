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
    val totalBookings: Int = 0
)

object AdminRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val APPLICATIONS = "businessApplications"
    private const val BUSINESSES = "businesses"
    private const val USERS = "users"
    private const val REPORTS = "reports"
    private const val AUDIT_LOGS = "adminAuditLogs"
    private const val BOOKINGS = "bookings"

    /** Listen to real-time Admin Overview statistics */
    fun listenToOverviewStats(onUpdate: (AdminOverviewStats) -> Unit): ListenerRegistration {
        return db.collection(BUSINESSES).addSnapshotListener { bizSnap, _ ->
            val allBiz = bizSnap?.documents?.mapNotNull { it.toObject(Business::class.java) } ?: emptyList()
            val totalBiz = allBiz.size
            val verifiedBiz = allBiz.count { it.verificationStatus == "VERIFIED" }

            db.collection(APPLICATIONS).whereEqualTo("status", "PENDING").get()
                .addOnSuccessListener { appSnap ->
                    val pendingApps = appSnap.size()

                    db.collection(USERS).get().addOnSuccessListener { userSnap ->
                        val totalUsers = userSnap.size()

                        db.collection(REPORTS).whereEqualTo("status", "OPEN").get()
                            .addOnSuccessListener { repSnap ->
                                val openReports = repSnap.size()

                                db.collection(BOOKINGS).get().addOnSuccessListener { bkSnap ->
                                    val totalBookings = bkSnap.size()
                                    onUpdate(
                                        AdminOverviewStats(
                                            totalUsers = totalUsers,
                                            totalBusinesses = totalBiz,
                                            pendingApplications = pendingApps,
                                            verifiedBusinesses = verifiedBiz,
                                            openReports = openReports,
                                            totalBookings = totalBookings
                                        )
                                    )
                                }.addOnFailureListener {
                                    onUpdate(AdminOverviewStats(totalUsers, totalBiz, pendingApps, verifiedBiz, openReports))
                                }
                            }
                    }
                }
        }
    }

    /** Listen to all business applications (Real-Time) */
    fun listenToApplications(onUpdate: (List<BusinessApplication>) -> Unit): ListenerRegistration {
        return db.collection(APPLICATIONS)
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BusinessApplication::class.java)?.copy(id = doc.id)
                }
                onUpdate(list)
            }
    }

    /** Fetch a single application by ID */
    fun getApplicationById(id: String, onSuccess: (BusinessApplication?) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection(APPLICATIONS).document(id).get()
            .addOnSuccessListener { snap ->
                val app = snap.toObject(BusinessApplication::class.java)?.copy(id = snap.id)
                onSuccess(app)
            }
            .addOnFailureListener { onFailure(it) }
    }

    /** Approve a business application atomically */
    fun approveApplication(
        applicationId: String,
        adminUid: String,
        adminEmail: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val appRef = db.collection(APPLICATIONS).document(applicationId)
        appRef.get().addOnSuccessListener { appSnap ->
            if (!appSnap.exists()) {
                onFailure(Exception("Application not found"))
                return@addOnSuccessListener
            }
            val app = appSnap.toObject(BusinessApplication::class.java)?.copy(id = appSnap.id)
                ?: return@addOnSuccessListener
            val bizRef = db.collection(BUSINESSES).document(app.businessId)

            val batch = db.batch()
            batch.update(appRef, mapOf(
                "status" to "APPROVED",
                "reviewedAt" to FieldValue.serverTimestamp(),
                "reviewedBy" to adminEmail
            ))
            batch.update(bizRef, mapOf(
                "approved" to true,
                "active" to true,
                "approvalStatus" to "APPROVED",
                "verificationStatus" to "VERIFIED",
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Audit log
            val logRef = db.collection(AUDIT_LOGS).document()
            val log = AdminAuditLog(
                id = logRef.id,
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "APPROVE_BUSINESS",
                targetType = "APPLICATION",
                targetId = applicationId,
                previousStatus = app.status,
                newStatus = "APPROVED",
                reason = "Application reviewed and verified by administrator"
            )
            batch.set(logRef, log.toMap())

            batch.commit().addOnSuccessListener {
                // Send notification to owner
                NotificationRepository.createNotification(
                    NotificationItem(
                        userId = app.ownerId,
                        title = "Business Approved!",
                        message = "Congratulations! Your business '${app.businessName}' has been approved and is now live on BizBridge.",
                        category = "SYSTEM",
                        relatedBusinessId = app.businessId
                    ),
                    onSuccess = {},
                    onFailure = {}
                )
                onSuccess()
            }.addOnFailureListener { onFailure(it) }
        }.addOnFailureListener { onFailure(it) }
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
        val appRef = db.collection(APPLICATIONS).document(applicationId)
        appRef.get().addOnSuccessListener { appSnap ->
            if (!appSnap.exists()) {
                onFailure(Exception("Application not found"))
                return@addOnSuccessListener
            }
            val app = appSnap.toObject(BusinessApplication::class.java)?.copy(id = appSnap.id)
                ?: return@addOnSuccessListener
            val bizRef = db.collection(BUSINESSES).document(app.businessId)

            val batch = db.batch()
            batch.update(appRef, mapOf(
                "status" to "REJECTED",
                "rejectionReason" to reason,
                "reviewedAt" to FieldValue.serverTimestamp(),
                "reviewedBy" to adminEmail
            ))
            batch.update(bizRef, mapOf(
                "approved" to false,
                "active" to false,
                "approvalStatus" to "REJECTED",
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            val logRef = db.collection(AUDIT_LOGS).document()
            val log = AdminAuditLog(
                id = logRef.id,
                adminUid = adminUid,
                adminEmail = adminEmail,
                actionType = "REJECT_BUSINESS",
                targetType = "APPLICATION",
                targetId = applicationId,
                previousStatus = app.status,
                newStatus = "REJECTED",
                reason = reason
            )
            batch.set(logRef, log.toMap())

            batch.commit().addOnSuccessListener {
                NotificationRepository.createNotification(
                    NotificationItem(
                        userId = app.ownerId,
                        title = "Application Update",
                        message = "Your registration for '${app.businessName}' was not approved: $reason",
                        category = "SYSTEM",
                        relatedBusinessId = app.businessId
                    ),
                    onSuccess = {},
                    onFailure = {}
                )
                onSuccess()
            }.addOnFailureListener { onFailure(it) }
        }.addOnFailureListener { onFailure(it) }
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
        batch.set(logRef, log.toMap())

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /** Listen to all businesses for Business Listings moderation */
    fun listenToAllBusinesses(onUpdate: (List<Business>) -> Unit): ListenerRegistration {
        return db.collection(BUSINESSES).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Business::class.java)?.copy(id = doc.id)
            }
            onUpdate(list)
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
        batch.update(bizRef, mapOf(
            "active" to active,
            "updatedAt" to FieldValue.serverTimestamp()
        ))
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
        batch.set(logRef, log.toMap())

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /** Record an administrative audit log */
    fun logAdminAction(log: AdminAuditLog, onComplete: () -> Unit = {}) {
        val ref = if (log.id.isNotEmpty()) db.collection(AUDIT_LOGS).document(log.id) else db.collection(AUDIT_LOGS).document()
        ref.set(log.copy(id = ref.id).toMap())
            .addOnCompleteListener { onComplete() }
    }
}
