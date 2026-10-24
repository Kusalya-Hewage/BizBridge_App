package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class CustomerProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customer_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bell
        view.findViewById<ImageView>(R.id.btn_bell)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }

        // Edit Profile
        view.findViewById<Button>(R.id.btn_edit_profile)?.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile coming soon", Toast.LENGTH_SHORT).show()
        }

        // Settings items
        view.findViewById<LinearLayout>(R.id.row_notifications_pref)?.setOnClickListener {
            Toast.makeText(requireContext(), "Notification preferences", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.row_password_security)?.setOnClickListener {
            Toast.makeText(requireContext(), "Password & security settings", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.row_privacy_policy)?.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy Policy", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.row_terms_service)?.setOnClickListener {
            Toast.makeText(requireContext(), "Terms of Service", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.row_help_support)?.setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support", Toast.LENGTH_SHORT).show()
        }

        // Log out -> navigates back to RoleSelectionFragment
        view.findViewById<TextView>(R.id.btn_logout)?.setOnClickListener {
    com.example.bizbridgeapp.FirebaseAuthManager.signOut()
    com.example.bizbridgeapp.UserSessionManager.clear()
    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
    (activity as? MainActivity)?.navigateTo(RoleSelectionFragment(), addToBackStack = false)
}
                // Sign out from Firebase
                com.example.bizbridgeapp.FirebaseAuthManager.signOut()
                // Clear cached profile
                com.example.bizbridgeapp.UserSessionManager.clear()
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.navigateTo(RoleSelectionFragment(), addToBackStack = false)
            }
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.navigateTo(RoleSelectionFragment(), addToBackStack = false)
        }

        // Bottom Navigation
        view.findViewById<LinearLayout>(R.id.nav_home)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
        }
        view.findViewById<LinearLayout>(R.id.nav_search)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_bookings)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(MyBookingsFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_notifications)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }
    }
}
