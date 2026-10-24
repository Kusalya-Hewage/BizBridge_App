package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

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

        val onUserClick = View.OnClickListener { v ->
            Toast.makeText(requireContext(), "User details and actions displayed.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_action_user_maya)?.setOnClickListener(onUserClick)
        view.findViewById<View>(R.id.btn_action_user_elena)?.setOnClickListener(onUserClick)
        view.findViewById<View>(R.id.btn_action_user_jamie)?.setOnClickListener(onUserClick)
        view.findViewById<View>(R.id.btn_action_user_alex)?.setOnClickListener(onUserClick)
    }
}
