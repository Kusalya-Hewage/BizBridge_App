package com.example.bizbridgeapp.repository

import com.example.bizbridgeapp.model.AdminAuditLog
import com.example.bizbridgeapp.model.Report
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object ReportRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val REPORTS = "reports"
    private const val AUDIT_LOGS = "adminAuditLogs"

    /** Submit a report against a business, review, or user */
    fun submitReport(report: Report, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = if (report.id.isNotEmpty()) db.collection(REPORTS).document(report.id) else db.collection(REPORTS).document()
        val toSave = report.copy(id = docRef.id)
        docRef.set(toSave.toMap())
            .addOnSuccessListener { onSuccess(docRef.id) }
            .addOnFailureListener { onFailure(it) }
    }

    /** Listen to open reports for Admin moderation */
    fun listenToOpenReports(onUpdate: (List<Report>) -> Unit): ListenerRegistration {
        return db.collection(REPORTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { it.toObject(Report::class.java)?.copy(id = it.id) }
                onUpdate(list)
            }
    }

    /** Resolve report (Admin only) */
    fun resolveReport(
        reportId: String,
        resolutionNotes: String,
        adminUid: String,
        adminEmail: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val ref = db.collection(REPORTS).document(reportId)
        val logRef = db.collection(AUDIT_LOGS).document()

        val batch = db.batch()
        batch.update(ref, mapOf(
            "status" to "RESOLVED",
            "resolutionNotes" to resolutionNotes,
            "resolvedAt" to FieldValue.serverTimestamp()
        ))
        val log = AdminAuditLog(
            id = logRef.id,
            adminUid = adminUid,
            adminEmail = adminEmail,
            actionType = "RESOLVE_REPORT",
            targetType = "REPORT",
            targetId = reportId,
            newStatus = "RESOLVED",
            reason = resolutionNotes
        )
        com.example.bizbridgeapp.UserSessionManager.saveAuditLog(log)
        batch.set(logRef, log.toMap())

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
