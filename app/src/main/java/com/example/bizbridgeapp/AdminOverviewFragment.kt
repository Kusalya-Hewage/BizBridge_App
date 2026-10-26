package com.example.bizbridgeapp

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.AdminAuditLog
import com.example.bizbridgeapp.repository.AdminRepository
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class AdminOverviewFragment : Fragment() {

    private var statsListener: ListenerRegistration? = null
    private var auditListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup bottom navigation for Admin Overview
        AdminNavigationHelper.setupBottomNav(this, view, AdminTab.OVERVIEW)

        val mainActivity = activity as? MainActivity

        // Connect quick access cards and buttons
        view.findViewById<View>(R.id.card_users)?.setOnClickListener {
            mainActivity?.navigateTo(AdminUserManagementFragment(), addToBackStack = true)
        }

        view.findViewById<View>(R.id.card_business_listings)?.setOnClickListener {
            mainActivity?.navigateTo(AdminBusinessListingsFragment(), addToBackStack = true)
        }

        view.findViewById<View>(R.id.card_pending_approvals)?.setOnClickListener {
            mainActivity?.navigateTo(AdminApplicationsFragment(), addToBackStack = true)
        }

        view.findViewById<View>(R.id.card_verified_businesses)?.setOnClickListener {
            mainActivity?.navigateTo(AdminBusinessListingsFragment(), addToBackStack = true)
        }

        // Action queue buttons
        view.findViewById<TextView>(R.id.btn_action_review_docs)?.setOnClickListener {
            mainActivity?.navigateTo(AdminApplicationsFragment(), addToBackStack = true)
        }

        view.findViewById<TextView>(R.id.btn_action_open_reports)?.setOnClickListener {
            mainActivity?.navigateTo(AdminReviewReportModerationFragment(), addToBackStack = true)
        }

        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(AdminAnnouncementsFragment(), addToBackStack = true)
        }

        // Immediately render local overview stats on frame 0 to eliminate mock/flash delay
        renderOverviewStats(AdminRepository.getLocalOverviewStats(), view)

        // Real-time Overview Statistics
        statsListener = AdminRepository.listenToOverviewStats { stats ->
            if (!isAdded) return@listenToOverviewStats
            renderOverviewStats(stats, view)
        }

        // Real-time Recent Admin Actions
        val actionsContainer = view.findViewById<LinearLayout>(R.id.layout_recent_admin_actions)
        val emptyActionsView = view.findViewById<TextView>(R.id.tv_no_recent_admin_actions)

        auditListener = AdminRepository.listenToRecentAuditLogs { logs ->
            if (!isAdded || actionsContainer == null) return@listenToRecentAuditLogs
            actionsContainer.removeAllViews()

            if (logs.isEmpty()) {
                emptyActionsView?.visibility = View.VISIBLE
            } else {
                emptyActionsView?.visibility = View.GONE
                val displayLogs = logs.take(5)
                for (log in displayLogs) {
                    val itemView = createAuditActionView(log)
                    actionsContainer.addView(itemView)
                }
            }
        }
    }

    private fun createAuditActionView(log: AdminAuditLog): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_admin_card)
            elevation = dpToPx(1).toFloat()
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)).apply {
                marginEnd = dpToPx(12)
            }
            val (iconRes, tintColor) = getActionIconAndTint(log.actionType)
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(tintColor)
        }

        val textContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }

        val titleTv = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = formatActionTitle(log)
            setTextColor(ContextCompat.getColor(context, R.color.admin_text_title))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        val subTv = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
            val timeStr = formatTimestamp(log.createdAt?.toDate())
            val actor = log.adminEmail.ifBlank { "Admin" }.substringBefore("@")
            text = "$actor · $timeStr"
            setTextColor(ContextCompat.getColor(context, R.color.admin_text_muted))
            textSize = 12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        textContainer.addView(titleTv)
        textContainer.addView(subTv)

        row.addView(icon)
        row.addView(textContainer)
        return row
    }

    private fun getActionIconAndTint(actionType: String): Pair<Int, Int> {
        val primary = ContextCompat.getColor(requireContext(), R.color.admin_primary)
        val alert = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        return when (actionType) {
            "APPROVE_BUSINESS" -> Pair(R.drawable.ic_shield_check, primary)
            "VERIFY_QUALIFICATION" -> Pair(R.drawable.ic_verified, primary)
            "REJECT_BUSINESS", "REJECT_QUALIFICATION" -> Pair(R.drawable.ic_admin_flag, alert)
            "SUSPEND_USER", "SUSPEND_LISTING" -> Pair(R.drawable.ic_lock, alert)
            "RESOLVE_REPORT" -> Pair(R.drawable.ic_shield_check, primary)
            else -> Pair(R.drawable.ic_shield_check, primary)
        }
    }

    private fun formatActionTitle(log: AdminAuditLog): String {
        return when (log.actionType) {
            "APPROVE_BUSINESS" -> "${log.targetId.removePrefix("biz_").replace("_", " ").capitalizeWords()} approved"
            "VERIFY_QUALIFICATION" -> "${log.targetId} verified"
            "REJECT_BUSINESS" -> "${log.targetId.removePrefix("biz_").replace("_", " ").capitalizeWords()} rejected"
            "REJECT_QUALIFICATION" -> "${log.targetId} rejected"
            "SUSPEND_USER" -> "Suspended user ${log.targetId.take(8)}"
            "ACTIVATE_USER" -> "Activated user ${log.targetId.take(8)}"
            "SUSPEND_LISTING" -> "Suspended listing ${log.targetId.take(8)}"
            "ACTIVATE_LISTING" -> "Activated listing ${log.targetId.take(8)}"
            "RESOLVE_REPORT" -> "Report resolved: ${log.reason.ifBlank { log.targetId }}"
            "SYSTEM_INITIALIZED" -> "Admin Console active"
            else -> "${log.actionType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} ${log.targetId}"
        }
    }

    private fun formatTimestamp(date: java.util.Date?): String {
        if (date == null) return "Just now"
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.US)
        return sdf.format(date)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    override fun onDestroyView() {
        super.onDestroyView()
        statsListener?.remove()
        auditListener?.remove()
    }
}
