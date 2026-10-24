package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class OwnerProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bottom Navigation
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.PROFILE)

        val mainActivity = activity as? MainActivity

        // Notifications bell
        view.findViewById<View>(R.id.iv_owner_profile_bell)?.setOnClickListener {
            Toast.makeText(requireContext(), "You're all caught up with notifications.", Toast.LENGTH_SHORT).show()
        }

        // Edit Profile -> B02
        view.findViewById<Button>(R.id.btn_edit_owner_profile)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerBusinessProfileFragment(), addToBackStack = true)
        }

        // Manage business card -> B02
        view.findViewById<View>(R.id.card_owner_business_item)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerBusinessProfileFragment(), addToBackStack = true)
        }

        // Workspace Menu options
        view.findViewById<View>(R.id.row_owner_schedule)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerAvailabilityScheduleFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.row_owner_qualifications)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerQualificationsDocumentsFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.row_owner_notifications)?.setOnClickListener {
            Toast.makeText(requireContext(), "Notification settings (Demonstration).", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.row_owner_security)?.setOnClickListener {
            Toast.makeText(requireContext(), "Password & security settings (Demonstration).", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.row_owner_help)?.setOnClickListener {
            Toast.makeText(requireContext(), "BizBridge Help & Support (Demonstration).", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.row_owner_terms)?.setOnClickListener {
            Toast.makeText(requireContext(), "Terms & Privacy Policy (Demonstration).", Toast.LENGTH_SHORT).show()
        }

        // Log Out Button with Specimen 04 Confirmation Dialog
        view.findViewById<Button>(R.id.btn_owner_profile_logout)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Logout confirmation",
                message = "Log out of BizBridge on this device? Unsaved changes are not submitted.",
                confirmButtonText = "Log Out",
                cancelButtonText = "Stay Logged In",
                isDestructive = true,
                onConfirm = {
                    FirebaseAuthManager.signOut()
                    UserSessionManager.clear()
                    mainActivity?.navigateTo(LoginFragment(), addToBackStack = false)
                }
            )
        }
    }
}
