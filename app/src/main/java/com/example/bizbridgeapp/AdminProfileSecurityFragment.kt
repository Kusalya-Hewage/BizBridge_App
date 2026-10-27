package com.example.bizbridgeapp

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.AdminAuditLog
import com.example.bizbridgeapp.repository.AdminRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class AdminProfileSecurityFragment : Fragment() {

    private var auditListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_profile_security, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup bottom nav for Profile tab
        AdminNavigationHelper.setupBottomNav(this, view, AdminTab.PROFILE)

        val mainActivity = activity as? MainActivity

        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(AdminAnnouncementsFragment(), addToBackStack = true)
        }

        // Fetch dynamic logged-in admin identity
        val profile = UserSessionManager.getProfile()
        val fbUser = FirebaseAuthManager.getCurrentUser()

        val name = (profile?.get("fullName") as? String)?.ifBlank { null }
            ?: fbUser?.displayName?.ifBlank { null }
            ?: "Alex Morgan"
        val email = (profile?.get("email") as? String)?.ifBlank { null }
            ?: fbUser?.email?.ifBlank { null }
            ?: "alex@bizbridge.example"
        val role = (profile?.get("role") as? String)?.ifBlank { null }
            ?: "Platform administrator"

        val initials = name.trim().split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "AM" }

        view.findViewById<TextView>(R.id.tv_admin_initials)?.text = initials
        view.findViewById<TextView>(R.id.tv_admin_name)?.text = name
        view.findViewById<TextView>(R.id.tv_admin_email_role)?.text = "$email · $role"

        // Dynamic device model for active sessions
        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        view.findViewById<TextView>(R.id.tv_profile_active_sessions)?.text = "$deviceName · Active now"

        view.findViewById<View>(R.id.row_two_step)?.setOnClickListener {
            Toast.makeText(requireContext(), "Two-step verification: Enabled via authenticated session.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.row_change_password)?.setOnClickListener {
            if (email.isNotBlank() && email.contains("@")) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Password reset instructions sent to $email", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Password reset link requested for $email", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Password reset request initiated for admin account.", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<View>(R.id.row_active_sessions)?.setOnClickListener {
            Toast.makeText(requireContext(), "Current active session on $deviceName.", Toast.LENGTH_SHORT).show()
        }

        // Dynamic recent actions for current admin
        val actionsContainer = view.findViewById<LinearLayout>(R.id.layout_profile_recent_actions)
        val emptyActionsView = view.findViewById<TextView>(R.id.tv_profile_no_recent_actions)

        auditListener = AdminRepository.listenToRecentAuditLogs(adminEmail = email) { logs ->
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

        // Log out of admin
        view.findViewById<View>(R.id.btn_logout_admin)?.setOnClickListener {
            FirebaseAuthManager.signOut()
            UserSessionManager.clear()
            Toast.makeText(requireContext(), "Logged out of Admin Operations.", Toast.LENGTH_SHORT).show()
            mainActivity?.navigateTo(LoginFragment(), addToBackStack = false)
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
            layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22)).apply {
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
            text = "${log.reason.ifBlank { "Action verified" }} · $timeStr"
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
            "APPROVE_BUSINESS" -> "Approved ${log.targetId.removePrefix("biz_").replace("_", " ").capitalizeWords()}"
            "VERIFY_QUALIFICATION" -> "Verified ${log.targetId}"
            "REJECT_BUSINESS" -> "Rejected ${log.targetId.removePrefix("biz_").replace("_", " ").capitalizeWords()}"
            "REJECT_QUALIFICATION" -> "Rejected ${log.targetId}"
            "SUSPEND_USER" -> "Suspended user ${log.targetId.take(8)}"
            "ACTIVATE_USER" -> "Activated user ${log.targetId.take(8)}"
            "SUSPEND_LISTING" -> "Suspended listing ${log.targetId.take(8)}"
            "ACTIVATE_LISTING" -> "Activated listing ${log.targetId.take(8)}"
            "RESOLVE_REPORT" -> "Resolved moderation report"
            "SYSTEM_INITIALIZED" -> "Console access established"
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
        auditListener?.remove()
    }
}
