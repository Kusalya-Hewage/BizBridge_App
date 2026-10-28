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
import android.widget.ProgressBar

import com.example.bizbridgeapp.repository.UserRepository
import com.example.bizbridgeapp.UserSessionManager
import com.example.bizbridgeapp.FirebaseAuthManager
import com.example.bizbridgeapp.LoginFragment
import com.example.bizbridgeapp.RoleSelectionFragment

import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.Fragment

class CustomerProfileFragment : Fragment() {

    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerHelper = ImagePickerHelper(this) { uri ->
            val currentUser = FirebaseAuth.getInstance().currentUser
            val cachedProfile = UserSessionManager.getProfile()
            val isUserLoggedIn = currentUser != null || (!cachedProfile.isNullOrEmpty() && (cachedProfile["email"] as? String).orEmpty().isNotBlank())

            if (!isUserLoggedIn) {
                Toast.makeText(requireContext(), "Please log in to upload a profile photo.", Toast.LENGTH_SHORT).show()
                return@ImagePickerHelper
            }

            val ivPhoto = view?.findViewById<ImageView>(R.id.iv_profile_photo)
            val tvInitials = view?.findViewById<TextView>(R.id.tv_avatar_initials)
            val progressBar = view?.findViewById<ProgressBar>(R.id.profile_progress)

            // 1. Immediately show locally in UI
            ivPhoto?.visibility = View.VISIBLE
            tvInitials?.visibility = View.GONE
            ivPhoto?.setImageURI(uri)

            val uid = currentUser?.uid ?: (cachedProfile?.get("uid") as? String) ?: "customer_${System.currentTimeMillis()}"

            // 2. Save locally into persistent internal storage and Base64
            val localPath = com.example.bizbridgeapp.utils.ImageLoader.saveProfileImageLocally(requireContext(), uri, uid)
            val base64Data = com.example.bizbridgeapp.utils.ImageLoader.uriToBase64(requireContext(), uri, 600)
            val effectivePath = base64Data ?: localPath ?: uri.toString()

            // 3. Immediately update local session
            UserSessionManager.updateProfileImage(effectivePath)

            // 4. Upload to Firebase Storage and update Firestore if user exists in Firebase
            if (currentUser != null) {
                progressBar?.visibility = View.VISIBLE
                FirebaseAuthManager.uploadProfileImage(
                    uid = uid,
                    uri = uri,
                    onSuccess = { downloadUrl ->
                        activity?.runOnUiThread {
                            progressBar?.visibility = View.GONE
                            UserSessionManager.updateProfileImage(downloadUrl)
                            if (ivPhoto != null && isAdded) {
                                com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivPhoto, downloadUrl, R.drawable.bg_score_badge)
                            }
                            FirebaseAuthManager.updateUserProfileImage(uid, downloadUrl)
                            Toast.makeText(requireContext(), "Profile photo updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = {
                        activity?.runOnUiThread {
                            progressBar?.visibility = View.GONE
                            FirebaseAuthManager.updateUserProfileImage(uid, effectivePath)
                            Toast.makeText(requireContext(), "Profile photo updated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Profile photo updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customer_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivPhoto = view.findViewById<ImageView>(R.id.iv_profile_photo)
        val tvInitials = view.findViewById<TextView>(R.id.tv_avatar_initials)
        val tvName = view.findViewById<TextView>(R.id.tv_profile_name)
        val tvEmail = view.findViewById<TextView>(R.id.tv_profile_email)
        val tvPhone = view.findViewById<TextView>(R.id.tv_profile_phone)
        val btnEditProfile = view.findViewById<Button>(R.id.btn_edit_profile)
        val btnLogout = view.findViewById<TextView>(R.id.btn_logout)
        val progressBar = view.findViewById<ProgressBar>(R.id.profile_progress)
        val rowPasswordSecurity = view.findViewById<LinearLayout>(R.id.row_password_security)
        val rowAdminWorkspace = view.findViewById<LinearLayout>(R.id.row_admin_workspace)

        // Determine if user is logged in via Firebase Auth or session
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cachedProfile = UserSessionManager.getProfile()
        val isUserLoggedIn = currentUser != null || (!cachedProfile.isNullOrEmpty() && (cachedProfile["email"] as? String).orEmpty().isNotBlank())

        // Header buttons
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
            }
        }

        view.findViewById<ImageView>(R.id.btn_bell)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }

        if (isUserLoggedIn) {
            // ─── LOGGED IN STATE ──────────────────────────────────────────
            val cachedName = (cachedProfile?.get("fullName") as? String).orEmpty()
                .ifEmpty { currentUser?.displayName.orEmpty().ifEmpty { "Customer" } }
            val cachedEmail = (cachedProfile?.get("email") as? String).orEmpty()
                .ifEmpty { currentUser?.email.orEmpty() }
            val cachedPhone = (cachedProfile?.get("phone") as? String).orEmpty()
            val cachedImageUrl = (cachedProfile?.get("profileImageUrl") as? String)?.ifBlank { null }

            tvName?.text = cachedName
            val initials = if (cachedName.contains(" ")) {
                cachedName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
            } else {
                cachedName.take(2).uppercase()
            }
            tvInitials?.text = if (initials.isNotEmpty()) initials else "CU"

            tvEmail?.text = cachedEmail

            if (cachedPhone.isNotEmpty()) {
                tvPhone?.visibility = View.VISIBLE
                tvPhone?.text = cachedPhone
            } else {
                tvPhone?.visibility = View.GONE
            }

            if (!cachedImageUrl.isNullOrBlank()) {
                ivPhoto?.visibility = View.VISIBLE
                tvInitials?.visibility = View.GONE
                if (ivPhoto != null) {
                    com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivPhoto, cachedImageUrl, R.drawable.bg_score_badge)
                }
            } else {
                ivPhoto?.visibility = View.GONE
                tvInitials?.visibility = View.VISIBLE
            }

            view.findViewById<View>(R.id.btn_edit_avatar)?.setOnClickListener {
                imagePickerHelper.showImageSourceDialog("Update Profile Photo")
            }

            btnEditProfile?.text = "Edit Profile"
            btnEditProfile?.setOnClickListener {
                val context = requireContext()
                val effectiveUid = currentUser?.uid ?: (cachedProfile?.get("uid") as? String).orEmpty()
                    .ifEmpty { "cust_${System.currentTimeMillis()}" }

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 40, 50, 10)
                }
                val inputName = android.widget.EditText(context).apply {
                    hint = "Full Name"
                    setText(tvName?.text)
                }
                val inputPhone = android.widget.EditText(context).apply {
                    hint = "Phone Number"
                    setText(if (tvPhone?.visibility == View.VISIBLE) tvPhone?.text else "")
                }
                layout.addView(inputName)
                layout.addView(inputPhone)

                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Edit Profile")
                    .setView(layout)
                    .setPositiveButton("Save") { _, _ ->
                        val newName = inputName.text.toString().trim()
                        val newPhone = inputPhone.text.toString().trim()
                        if (newName.isEmpty()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        val existingImg = (UserSessionManager.getProfile()?.get("profileImageUrl") as? String).orEmpty()
                        val sessionRole = (UserSessionManager.getProfile()?.get("role") as? String)?.ifBlank { "CUSTOMER" } ?: "CUSTOMER"
                        val updated = com.example.bizbridgeapp.model.UserProfile(
                            uid = effectiveUid,
                            fullName = newName,
                            email = cachedEmail,
                            phone = newPhone,
                            role = sessionRole,
                            profileImageUrl = existingImg,
                            accountStatus = "ACTIVE"
                        )
                        tvName?.text = newName
                        if (newPhone.isNotEmpty()) {
                            tvPhone?.visibility = View.VISIBLE
                            tvPhone?.text = newPhone
                        } else {
                            tvPhone?.visibility = View.GONE
                        }
                        UserSessionManager.saveProfile(updated.toMap())
                        val registered = UserSessionManager.getRegisteredAccount(cachedEmail)
                        if (registered != null) {
                            val pass = registered["password"] as? String ?: ""
                            UserSessionManager.saveRegisteredAccount(cachedEmail, pass, updated.toMap())
                        }

                        if (currentUser != null) {
                            UserRepository.createOrUpdate(updated, onSuccess = {}, onFailure = {})
                        }
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            btnLogout?.visibility = View.VISIBLE
            btnLogout?.text = "Log Out"
            btnLogout?.setOnClickListener {
                FirebaseAuthManager.signOut()
                UserSessionManager.clear()
                // Clear entire Fragment backstack so pressing Back does not reload logged-in session
                activity?.supportFragmentManager?.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = false)
            }

            rowPasswordSecurity?.visibility = if (cachedEmail.isNotEmpty()) View.VISIBLE else View.GONE
            rowPasswordSecurity?.setOnClickListener {
                if (cachedEmail.isNotEmpty()) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Password & Security")
                        .setMessage("Send a password reset email to $cachedEmail?")
                        .setPositiveButton("Send Reset Email") { _, _ ->
                            FirebaseAuthManager.sendPasswordReset(cachedEmail,
                                onSuccess = {
                                    Toast.makeText(requireContext(), "Password reset email sent to $cachedEmail", Toast.LENGTH_LONG).show()
                                },
                                onFailure = { e ->
                                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }

            val sessionRole = (cachedProfile?.get("role") as? String).orEmpty()
            val isAdminUser = sessionRole.equals("ADMIN", ignoreCase = true)
            rowAdminWorkspace?.visibility = if (isAdminUser) View.VISIBLE else View.GONE
            rowAdminWorkspace?.setOnClickListener {
                (activity as? MainActivity)?.navigateTo(AdminOverviewFragment(), addToBackStack = true)
            }

            // Sync with Firestore if cloud account exists
            if (currentUser != null) {
                progressBar?.visibility = View.VISIBLE
                UserRepository.getProfile(currentUser.uid,
                    onSuccess = { profile ->
                        if (!isAdded) return@getProfile
                        tvName?.text = profile.fullName
                        tvEmail?.text = profile.email
                        if (profile.phone.isNotEmpty()) {
                            tvPhone?.visibility = View.VISIBLE
                            tvPhone?.text = profile.phone
                        }
                        val effectiveImageUrl = if (profile.profileImageUrl.isNotBlank()) {
                            profile.profileImageUrl
                        } else {
                            (UserSessionManager.getProfile()?.get("profileImageUrl") as? String).orEmpty()
                        }
                        if (effectiveImageUrl.isNotBlank()) {
                            ivPhoto?.visibility = View.VISIBLE
                            tvInitials?.visibility = View.GONE
                            if (ivPhoto != null) {
                                com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivPhoto, effectiveImageUrl, R.drawable.bg_score_badge)
                            }
                            UserSessionManager.updateProfileImage(effectiveImageUrl)
                        }
                        val updatedMap = profile.toMap().toMutableMap()
                        if (effectiveImageUrl.isNotBlank()) {
                            updatedMap["profileImageUrl"] = effectiveImageUrl
                        }
                        UserSessionManager.saveProfile(updatedMap)
                        progressBar?.visibility = View.GONE
                    },
                    onFailure = {
                        if (isAdded) {
                            progressBar?.visibility = View.GONE
                        }
                    }
                )
            } else {
                progressBar?.visibility = View.GONE
            }

        } else {
            // ─── GUEST STATE (NOT LOGGED IN) ──────────────────────────────
            progressBar?.visibility = View.GONE
            tvName?.text = "Guest Customer"
            tvEmail?.text = "Sign in to manage your appointments & profile"
            tvPhone?.visibility = View.GONE
            tvInitials?.text = "G"
            ivPhoto?.visibility = View.GONE
            tvInitials?.visibility = View.VISIBLE

            view.findViewById<View>(R.id.btn_edit_avatar)?.setOnClickListener {
                Toast.makeText(requireContext(), "Please log in to add a profile photo.", Toast.LENGTH_SHORT).show()
            }

            btnEditProfile?.text = "Log In / Register"
            btnEditProfile?.setOnClickListener {
                (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = true)
            }

            btnLogout?.visibility = View.VISIBLE
            btnLogout?.text = "Log In to Account"
            btnLogout?.setOnClickListener {
                (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = true)
            }

            rowPasswordSecurity?.visibility = View.GONE
            rowAdminWorkspace?.visibility = View.GONE
        }

        // Common settings items
        view.findViewById<LinearLayout>(R.id.row_notifications_pref)?.setOnClickListener {
            val options = arrayOf("Booking updates & reminders", "Promotions & local deals")
            val checked = booleanArrayOf(true, true)
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Notification Preferences")
                .setMultiChoiceItems(options, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("Save") { _, _ ->
                    Toast.makeText(requireContext(), "Preferences saved", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        view.findViewById<LinearLayout>(R.id.row_privacy_policy)?.setOnClickListener {
            Toast.makeText(requireContext(), "BizBridge connects customers to local verified businesses. Your data is protected.", Toast.LENGTH_LONG).show()
        }
        view.findViewById<LinearLayout>(R.id.row_terms_service)?.setOnClickListener {
            Toast.makeText(requireContext(), "BizBridge Terms of Service: Safe, reliable local booking.", Toast.LENGTH_LONG).show()
        }
        view.findViewById<LinearLayout>(R.id.row_help_support)?.setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support: support@bizbridge.local", Toast.LENGTH_SHORT).show()
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
