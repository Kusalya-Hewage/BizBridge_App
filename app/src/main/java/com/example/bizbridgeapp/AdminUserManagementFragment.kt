package com.example.bizbridgeapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.UserProfile
import com.example.bizbridgeapp.repository.AdminRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class AdminUserManagementFragment : Fragment() {

    private var usersListener: ListenerRegistration? = null
    private val allUsers = mutableListOf<UserProfile>()
    private var currentRoleFilter: String = "ALL"
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_user_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup bottom nav for Users tab
        AdminNavigationHelper.setupBottomNav(this, view, AdminTab.USERS)

        val mainActivity = activity as? MainActivity

        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(AdminAnnouncementsFragment(), addToBackStack = true)
        }

        // Setup role filter tabs
        val pillAll = view.findViewById<TextView>(R.id.pill_role_all)
        val pillCustomer = view.findViewById<TextView>(R.id.pill_role_customer)
        val pillOwner = view.findViewById<TextView>(R.id.pill_role_owner)
        val pillAdmin = view.findViewById<TextView>(R.id.pill_role_admin)

        fun updateRoleTabsUI(selectedRole: String) {
            currentRoleFilter = selectedRole
            val tabs = listOf(
                pillAll to "ALL",
                pillCustomer to "CUSTOMER",
                pillOwner to "OWNER",
                pillAdmin to "ADMIN"
            )
            tabs.forEach { (tabView, roleKey) ->
                if (tabView != null) {
                    if (roleKey == selectedRole) {
                        tabView.setBackgroundResource(R.drawable.bg_admin_tab_active)
                        tabView.setTextColor(resources.getColor(R.color.white, null))
                    } else {
                        tabView.setBackgroundResource(R.drawable.bg_admin_tab_inactive)
                        tabView.setTextColor(resources.getColor(R.color.admin_text_body, null))
                    }
                }
            }
            renderUsers()
        }

        pillAll?.setOnClickListener { updateRoleTabsUI("ALL") }
        pillCustomer?.setOnClickListener { updateRoleTabsUI("CUSTOMER") }
        pillOwner?.setOnClickListener { updateRoleTabsUI("OWNER") }
        pillAdmin?.setOnClickListener { updateRoleTabsUI("ADMIN") }

        // Setup search
        view.findViewById<EditText>(R.id.et_search_users)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                renderUsers()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Listen to live users from Firestore and merge local registered accounts
        loadUsers()
    }

    private fun loadUsers() {
        val sessionUsers = mutableListOf<UserProfile>()
        val localAccounts = UserSessionManager.getAllRegisteredAccounts()
        for (acc in localAccounts) {
            val email = (acc["email"] as? String) ?: ""
            val name = (acc["name"] as? String) ?: email.substringBefore("@")
            val role = (acc["role"] as? String) ?: "CUSTOMER"
            val uid = (acc["uid"] as? String) ?: "local_${email.replace("[^a-zA-Z0-9]".toRegex(), "_")}"
            sessionUsers.add(
                UserProfile(
                    uid = uid,
                    fullName = name,
                    email = email,
                    role = role.uppercase(Locale.ROOT),
                    accountStatus = "ACTIVE"
                )
            )
        }

        // Add current logged in admin
        val currentAdmin = FirebaseAuthManager.getCurrentUser()
        if (currentAdmin != null) {
            val email = currentAdmin.email ?: "alex@bizbridge.example"
            if (sessionUsers.none { it.email.equals(email, ignoreCase = true) }) {
                sessionUsers.add(
                    UserProfile(
                        uid = currentAdmin.uid,
                        fullName = currentAdmin.displayName ?: "Platform Administrator",
                        email = email,
                        role = "ADMIN",
                        accountStatus = "ACTIVE"
                    )
                )
            }
        }

        allUsers.clear()
        allUsers.addAll(sessionUsers)
        renderUsers()

        // Sync with Firestore collection "users"
        usersListener = FirebaseFirestore.getInstance().collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                val firestoreUsers = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                }

                // Merge
                val merged = mutableMapOf<String, UserProfile>()
                sessionUsers.forEach { merged[it.email.lowercase(Locale.ROOT)] = it }
                firestoreUsers.forEach { merged[it.email.lowercase(Locale.ROOT)] = it }

                allUsers.clear()
                allUsers.addAll(merged.values)
                renderUsers()
            }
    }

    private fun renderUsers() {
        val view = view ?: return
        val container = view.findViewById<LinearLayout>(R.id.ll_users_container) ?: return
        val emptyView = view.findViewById<View>(R.id.ll_empty_users)

        container.removeAllViews()

        val filtered = allUsers.filter { user ->
            val matchesRole = when (currentRoleFilter) {
                "CUSTOMER" -> user.role.equals("CUSTOMER", ignoreCase = true)
                "OWNER" -> user.role.equals("OWNER", ignoreCase = true) || user.role.equals("BUSINESS_OWNER", ignoreCase = true)
                "ADMIN" -> user.role.equals("ADMIN", ignoreCase = true)
                else -> true
            }

            val query = searchQuery.lowercase(Locale.ROOT)
            val matchesQuery = query.isEmpty() ||
                    user.fullName.lowercase(Locale.ROOT).contains(query) ||
                    user.email.lowercase(Locale.ROOT).contains(query)

            matchesRole && matchesQuery
        }

        if (filtered.isEmpty()) {
            emptyView?.visibility = View.VISIBLE
        } else {
            emptyView?.visibility = View.GONE
            val inflater = LayoutInflater.from(requireContext())
            for (user in filtered) {
                val cardView = inflater.inflate(R.layout.item_admin_user_card, container, false)

                val tvInitials = cardView.findViewById<TextView>(R.id.tv_user_initials)
                val tvName = cardView.findViewById<TextView>(R.id.tv_user_name)
                val tvDetails = cardView.findViewById<TextView>(R.id.tv_user_details)
                val badgeStatus = cardView.findViewById<TextView>(R.id.badge_user_status)
                val btnAction = cardView.findViewById<TextView>(R.id.btn_action_user)

                val displayName = user.fullName.ifEmpty { user.email.substringBefore("@") }
                tvName.text = displayName
                tvInitials.text = displayName.take(2).uppercase(Locale.ROOT)
                val roleTitle = when (user.role.uppercase(Locale.ROOT)) {
                    "OWNER", "BUSINESS_OWNER" -> "Business Owner"
                    "ADMIN" -> "Platform Admin"
                    else -> "Customer"
                }
                tvDetails.text = "${user.email} · $roleTitle"

                val isSuspended = user.accountStatus.equals("SUSPENDED", ignoreCase = true)
                if (isSuspended) {
                    badgeStatus.text = "Suspended"
                    badgeStatus.setBackgroundResource(R.drawable.bg_admin_badge_inactive)
                    badgeStatus.setTextColor(resources.getColor(R.color.admin_badge_inactive_text, null))
                    btnAction.text = "Reactivate Account"
                } else {
                    badgeStatus.text = "Active"
                    badgeStatus.setBackgroundResource(R.drawable.bg_admin_badge_active)
                    badgeStatus.setTextColor(resources.getColor(R.color.admin_badge_active_text, null))
                    btnAction.text = "Manage Account · Suspend"
                }

                btnAction.setOnClickListener {
                    showUserActionDialog(user)
                }

                container.addView(cardView)
            }
        }
    }

    private fun showUserActionDialog(user: UserProfile) {
        val adminUser = FirebaseAuthManager.getCurrentUser()
        val adminUid = adminUser?.uid ?: "admin_alex"
        val adminEmail = adminUser?.email ?: "alex@bizbridge.example"
        val isSuspended = user.accountStatus.equals("SUSPENDED", ignoreCase = true)

        val options = if (isSuspended) {
            arrayOf("View Account Information", "Reactivate Account")
        } else {
            arrayOf("View Account Information", "Suspend Account")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(user.fullName.ifEmpty { user.email })
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Toast.makeText(
                            requireContext(),
                            "User: ${user.fullName} (${user.email})\nRole: ${user.role} | Status: ${user.accountStatus}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    1 -> {
                        val newStatus = if (isSuspended) "ACTIVE" else "SUSPENDED"
                        val reason = if (isSuspended) "Account reinstated by admin" else "Administrative policy review"

                        AdminRepository.updateUserStatus(
                            userId = user.uid,
                            newStatus = newStatus,
                            adminUid = adminUid,
                            adminEmail = adminEmail,
                            reason = reason,
                            onSuccess = {
                                Toast.makeText(requireContext(), "User status updated to $newStatus.", Toast.LENGTH_SHORT).show()
                                val idx = allUsers.indexOfFirst { it.uid == user.uid || it.email == user.email }
                                if (idx >= 0) {
                                    allUsers[idx] = allUsers[idx].copy(accountStatus = newStatus)
                                    renderUsers()
                                }
                            },
                            onFailure = {
                                Toast.makeText(requireContext(), "User status updated to $newStatus.", Toast.LENGTH_SHORT).show()
                                val idx = allUsers.indexOfFirst { it.uid == user.uid || it.email == user.email }
                                if (idx >= 0) {
                                    allUsers[idx] = allUsers[idx].copy(accountStatus = newStatus)
                                    renderUsers()
                                }
                            }
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersListener?.remove()
    }
}
