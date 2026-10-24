package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class AdminProfileSecurityFragment : Fragment() {

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

        view.findViewById<View>(R.id.row_two_step)?.setOnClickListener {
            Toast.makeText(requireContext(), "Two-step verification is enabled via Authenticator App.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.row_change_password)?.setOnClickListener {
            Toast.makeText(requireContext(), "Change password request initiated.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.row_active_sessions)?.setOnClickListener {
            Toast.makeText(requireContext(), "Active sessions: 1 current device.", Toast.LENGTH_SHORT).show()
        }

        // Log out of admin
        view.findViewById<View>(R.id.btn_logout_admin)?.setOnClickListener {
            Toast.makeText(requireContext(), "Logged out of Admin Operations.", Toast.LENGTH_SHORT).show()
            mainActivity?.navigateTo(RoleSelectionFragment(), addToBackStack = false)
        }
    }
}
