package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.AdminRepository

class AdminUserManagementFragment : Fragment() {

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
                        0 -> Toast.makeText(requireContext(), "User $userName has 12 appointments and active status.", Toast.LENGTH_SHORT).show()
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

        view.findViewById<View>(R.id.btn_action_user_maya)?.setOnClickListener {
            showUserActionDialog("Maya Lin", "user_maya_lin")
        }
        view.findViewById<View>(R.id.btn_action_user_elena)?.setOnClickListener {
            showUserActionDialog("Elena Rostova", "user_elena_rostova")
        }
        view.findViewById<View>(R.id.btn_action_user_jamie)?.setOnClickListener {
            showUserActionDialog("Jamie Vance", "user_jamie_vance")
        }
        view.findViewById<View>(R.id.btn_action_user_alex)?.setOnClickListener {
            showUserActionDialog("Alex Morgan", "user_alex_morgan")
        }
    }
}
