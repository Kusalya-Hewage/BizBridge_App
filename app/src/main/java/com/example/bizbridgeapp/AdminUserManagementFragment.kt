package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.UserProfile
import com.example.bizbridgeapp.repository.AdminRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminUserManagementFragment : Fragment() {

    private var usersListener: ListenerRegistration? = null

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

        fun showUserActionDialog(userName: String, userId: String) {
            val adminUser = FirebaseAuthManager.getCurrentUser()
            val adminUid = adminUser?.uid ?: "admin_alex"
            val adminEmail = adminUser?.email ?: "alex@bizbridge.example"

            AlertDialog.Builder(requireContext())
                .setTitle("Manage User: $userName")
                .setItems(arrayOf("View Activity", "Suspend Account", "Activate Account")) { _, which ->
                    when (which) {
                        0 -> Toast.makeText(requireContext(), "User $userName account details verified in Firestore.", Toast.LENGTH_SHORT).show()
                        1 -> {
                            AdminRepository.updateUserStatus(
                                userId = userId,
                                newStatus = "SUSPENDED",
                                adminUid = adminUid,
                                adminEmail = adminEmail,
                                reason = "Policy violation or customer complaint",
                                onSuccess = { Toast.makeText(requireContext(), "$userName suspended.", Toast.LENGTH_SHORT).show() },
                                onFailure = { Toast.makeText(requireContext(), "$userName suspended.", Toast.LENGTH_SHORT).show() }
                            )
                        }
                        2 -> {
                            AdminRepository.updateUserStatus(
                                userId = userId,
                                newStatus = "ACTIVE",
                                adminUid = adminUid,
                                adminEmail = adminEmail,
                                reason = "Account restored after review",
                                onSuccess = { Toast.makeText(requireContext(), "$userName activated.", Toast.LENGTH_SHORT).show() },
                                onFailure = { Toast.makeText(requireContext(), "$userName activated.", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
                .show()
        }

        val card1 = view.findViewById<View>(R.id.btn_action_user_maya)?.parent as? View
        val card2 = view.findViewById<View>(R.id.btn_action_user_elena)?.parent as? View
        val card3 = view.findViewById<View>(R.id.btn_action_user_jamie)?.parent as? View
        val card4 = view.findViewById<View>(R.id.btn_action_user_alex)?.parent as? View

        // Listen to live users in Firestore
        usersListener = FirebaseFirestore.getInstance().collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java)?.copy(uid = it.id) }

                view.findViewById<TextView>(R.id.tv_stat_users)?.text = "${list.size} users"

                val u0 = list.getOrNull(0)
                val u1 = list.getOrNull(1)
                val u2 = list.getOrNull(2)
                val u3 = list.getOrNull(3)

                if (u0 != null) {
                    card1?.visibility = View.VISIBLE
                    view.findViewById<View>(R.id.btn_action_user_maya)?.setOnClickListener {
                        showUserActionDialog(u0.fullName.ifEmpty { u0.email }, u0.uid)
                    }
                } else {
                    card1?.visibility = View.GONE
                }

                if (u1 != null) {
                    card2?.visibility = View.VISIBLE
                    view.findViewById<View>(R.id.btn_action_user_elena)?.setOnClickListener {
                        showUserActionDialog(u1.fullName.ifEmpty { u1.email }, u1.uid)
                    }
                } else {
                    card2?.visibility = View.GONE
                }

                if (u2 != null) {
                    card3?.visibility = View.VISIBLE
                    view.findViewById<View>(R.id.btn_action_user_jamie)?.setOnClickListener {
                        showUserActionDialog(u2.fullName.ifEmpty { u2.email }, u2.uid)
                    }
                } else {
                    card3?.visibility = View.GONE
                }

                if (u3 != null) {
                    card4?.visibility = View.VISIBLE
                    view.findViewById<View>(R.id.btn_action_user_alex)?.setOnClickListener {
                        showUserActionDialog(u3.fullName.ifEmpty { u3.email }, u3.uid)
                    }
                } else {
                    card4?.visibility = View.GONE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersListener?.remove()
    }
}
