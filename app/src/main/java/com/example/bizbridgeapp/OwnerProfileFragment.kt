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

import android.widget.ImageView

class OwnerProfileFragment : Fragment() {

    private var businessListener: ListenerRegistration? = null
    private var currentBusinessId: String = ""
    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerHelper = ImagePickerHelper(this) { uri ->
            val ivPhoto = view?.findViewById<ImageView>(R.id.iv_owner_profile_photo)
            val tvAvatar = view?.findViewById<TextView>(R.id.tv_owner_profile_avatar)

            // 1. Immediately show locally in UI
            ivPhoto?.visibility = View.VISIBLE
            tvAvatar?.visibility = View.GONE
            ivPhoto?.setImageURI(uri)

            val currentUser = FirebaseAuth.getInstance().currentUser
            val profile = UserSessionManager.getProfile()
            val uid = currentUser?.uid ?: (profile?.get("uid") as? String) ?: "owner"

            // 2. Save locally into persistent internal storage and Base64
            val localPath = com.example.bizbridgeapp.utils.ImageLoader.saveProfileImageLocally(requireContext(), uri, uid)
            val base64Data = com.example.bizbridgeapp.utils.ImageLoader.uriToBase64(requireContext(), uri, 600)
            val effectivePath = base64Data ?: localPath ?: uri.toString()

            // 3. Immediately update local session
            UserSessionManager.updateProfileImage(effectivePath)

            // 4. Upload to Firebase Storage and update Firestore
            FirebaseAuthManager.uploadProfileImage(
                uid = uid,
                uri = uri,
                onSuccess = { downloadUrl ->
                    activity?.runOnUiThread {
                        UserSessionManager.updateProfileImage(downloadUrl)
                        if (ivPhoto != null && isAdded) {
                            com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivPhoto, downloadUrl, R.drawable.bg_avatar_circle)
                        }
                        FirebaseAuthManager.updateUserProfileImage(uid, downloadUrl)
                        if (currentBusinessId.isNotBlank()) {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("businesses").document(currentBusinessId)
                                .update("ownerImageUrl", downloadUrl)
                        }
                        Toast.makeText(requireContext(), "Owner profile photo updated!", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = {
                    activity?.runOnUiThread {
                        FirebaseAuthManager.updateUserProfileImage(uid, effectivePath)
                        Toast.makeText(requireContext(), "Owner profile photo saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

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

        val ivPhoto = view.findViewById<ImageView>(R.id.iv_owner_profile_photo)
        val tvAvatar = view.findViewById<TextView>(R.id.tv_owner_profile_avatar)

        // Avatar click -> Camera or Gallery
        view.findViewById<View>(R.id.btn_edit_owner_avatar)?.setOnClickListener {
            imagePickerHelper.showImageSourceDialog("Update Owner Profile Photo")
        }

        // Bind owner profile data
        val currentUser = FirebaseAuth.getInstance().currentUser
        val profile = UserSessionManager.getProfile()

        val ownerName = (profile?.get("fullName") as? String)
            ?: currentUser?.displayName
            ?: "Business Owner"
        val ownerEmail = currentUser?.email
            ?: (profile?.get("email") as? String)
            ?: ""
        val ownerPhone = (profile?.get("phone") as? String) ?: ""

        view.findViewById<TextView>(R.id.tv_owner_profile_name)?.text = ownerName
        val contactText = if (ownerPhone.isNotEmpty()) "$ownerEmail · $ownerPhone" else ownerEmail
        view.findViewById<TextView>(R.id.tv_owner_profile_contact)?.text = contactText

        val initials = ownerName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
        tvAvatar?.text = if (initials.isNotEmpty()) initials else "BO"

        // Load profile photo from local session or business
        val savedImageUrl = (profile?.get("profileImageUrl") as? String)?.ifBlank { null }
            ?: (profile?.get("imageUrl") as? String)?.ifBlank { null }

        if (!savedImageUrl.isNullOrBlank()) {
            ivPhoto?.visibility = View.VISIBLE
            tvAvatar?.visibility = View.GONE
            if (ivPhoto != null) {
                com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivPhoto, savedImageUrl, R.drawable.bg_avatar_circle)
            }
        } else {
            ivPhoto?.visibility = View.GONE
            tvAvatar?.visibility = View.VISIBLE
        }

        // Background sync with Firestore
        val ownerUid = currentUser?.uid ?: (profile?.get("uid") as? String)
        if (!ownerUid.isNullOrBlank()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(ownerUid).get()
                .addOnSuccessListener { snap ->
                    if (isAdded && snap.exists()) {
                        val cloudImg = snap.getString("profileImageUrl")
                        if (!cloudImg.isNullOrBlank() && cloudImg != savedImageUrl) {
                            ivPhoto?.visibility = View.VISIBLE
                            tvAvatar?.visibility = View.GONE
                            if (ivPhoto != null) {
                                com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivPhoto, cloudImg, R.drawable.bg_avatar_circle)
                            }
                            UserSessionManager.updateProfileImage(cloudImg)
                        }
                    }
                }
        }

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

        // Edit Profile -> Dialog with direct Name & Phone edit + Business Listing option
        view.findViewById<Button>(R.id.btn_edit_owner_profile)?.setOnClickListener {
            val context = requireContext()
            val layout = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }
            val inputName = android.widget.EditText(context).apply {
                hint = "Owner Full Name"
                setText(view.findViewById<TextView>(R.id.tv_owner_profile_name)?.text)
            }
            val inputPhone = android.widget.EditText(context).apply {
                hint = "Contact Phone"
                val currContact = view.findViewById<TextView>(R.id.tv_owner_profile_contact)?.text?.toString().orEmpty()
                val phonePart = if (currContact.contains("·")) currContact.substringAfter("·").trim() else ""
                setText(phonePart)
            }
            layout.addView(inputName)
            layout.addView(inputPhone)

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Edit Owner Profile")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val newName = inputName.text.toString().trim()
                    val newPhone = inputPhone.text.toString().trim()
                    if (newName.isEmpty()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    view.findViewById<TextView>(R.id.tv_owner_profile_name)?.text = newName
                    val contactText = if (newPhone.isNotEmpty()) "$ownerEmail · $newPhone" else ownerEmail
                    view.findViewById<TextView>(R.id.tv_owner_profile_contact)?.text = contactText

                    val updatedProfile = UserSessionManager.getProfile()?.toMutableMap() ?: mutableMapOf()
                    updatedProfile["fullName"] = newName
                    updatedProfile["phone"] = newPhone
                    UserSessionManager.saveProfile(updatedProfile)

                    val uid = currentUser?.uid ?: (updatedProfile["uid"] as? String)
                    if (!uid.isNullOrBlank()) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .set(mapOf("fullName" to newName, "phone" to newPhone, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
                    }
                    Toast.makeText(context, "Owner profile updated!", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Business Listing") { _, _ ->
                    mainActivity?.navigateTo(OwnerBusinessProfileFragment(), addToBackStack = true)
                }
                .setNegativeButton("Cancel", null)
                .show()
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
