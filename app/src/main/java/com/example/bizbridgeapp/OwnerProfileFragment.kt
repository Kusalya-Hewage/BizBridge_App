package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.BusinessRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class OwnerProfileFragment : Fragment() {

    private var businessListener: ListenerRegistration? = null
    private var currentBusinessId: String = "biz_juniper_studio"

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
        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Bind owner profile data
        val currentUser = FirebaseAuth.getInstance().currentUser
        val profile = UserSessionManager.getProfile()

        val ownerName = (profile?.get("fullName") as? String)
            ?: currentUser?.displayName
            ?: "Elena Rivera"
        val ownerEmail = currentUser?.email
            ?: (profile?.get("email") as? String)
            ?: "elena@juniper.example"
        val ownerPhone = (profile?.get("phone") as? String) ?: "+1 (415) 555-2782"

        view.findViewById<TextView>(R.id.tv_owner_profile_name)?.text = ownerName
        view.findViewById<TextView>(R.id.tv_owner_profile_contact)?.text = "$ownerEmail · $ownerPhone"

        val initials = ownerName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
        view.findViewById<TextView>(R.id.tv_owner_profile_avatar)?.text = if (initials.isNotEmpty()) initials else "ER"

        // Real-time listener for owner's business
        businessListener = BusinessRepository.listenToOwnerBusiness(currentBusinessId) { biz ->
            if (!isAdded || biz == null) return@listenToOwnerBusiness
            view.findViewById<TextView>(R.id.tv_owner_profile_biz_name)?.text = biz.name
            val statusDesc = if (biz.approved && biz.active) "Approved listing · ${biz.category}" else "Pending review · ${biz.category}"
            view.findViewById<TextView>(R.id.tv_owner_profile_biz_status)?.text = statusDesc
        }

        // Notifications bell
        view.findViewById<View>(R.id.iv_owner_profile_bell)?.setOnClickListener {
            mainActivity?.navigateTo(NotificationsFragment(), addToBackStack = true)
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
            mainActivity?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.row_owner_security)?.setOnClickListener {
            Toast.makeText(requireContext(), "Password & security settings via Firebase Auth.", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.row_owner_help)?.setOnClickListener {
            Toast.makeText(requireContext(), "BizBridge Help & Support Center.", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.row_owner_terms)?.setOnClickListener {
            Toast.makeText(requireContext(), "Terms & Privacy Policy.", Toast.LENGTH_SHORT).show()
        }

        // Log Out Button with Confirmation Dialog
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

    override fun onDestroyView() {
        super.onDestroyView()
        businessListener?.remove()
    }
}
