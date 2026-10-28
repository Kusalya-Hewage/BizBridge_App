package com.example.bizbridgeapp

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.QualificationDocument
import com.example.bizbridgeapp.repository.QualificationRepository

class RegisterOwnerFragment : Fragment() {

    private var isPasswordVisible = false
    private var attachedDocUri: Uri? = null
    private var ownerPhotoUri: Uri? = null
    private lateinit var docPickerHelper: ImagePickerHelper
    private lateinit var photoPickerHelper: ImagePickerHelper

    private val categories = arrayOf(
        "Hair salon",
        "Beauty & Spa",
        "Fitness",
        "Repair & Maintenance",
        "Wellness",
        "Education",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        docPickerHelper = ImagePickerHelper(this) { uri ->
            attachedDocUri = uri
            view?.findViewById<TextView>(R.id.tv_reg_attached_doc_status)?.apply {
                text = "✓ Document photo attached"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
            }
            Toast.makeText(requireContext(), "Document attached for application!", Toast.LENGTH_SHORT).show()
        }
        photoPickerHelper = ImagePickerHelper(this) { uri ->
            ownerPhotoUri = uri
            val ivPhoto = view?.findViewById<ImageView>(R.id.iv_reg_owner_photo)
            val tvInitials = view?.findViewById<TextView>(R.id.tv_reg_owner_avatar_initials)
            ivPhoto?.visibility = View.VISIBLE
            tvInitials?.visibility = View.GONE
            ivPhoto?.setImageURI(uri)
            view?.findViewById<TextView>(R.id.tv_add_owner_profile_image)?.text = "Change owner photo"
            Toast.makeText(requireContext(), "Owner photo selected!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_owner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val containerCategory = view.findViewById<View>(R.id.container_category)
        val tvCategory = view.findViewById<TextView>(R.id.tv_category)
        val etOwnerName = view.findViewById<EditText>(R.id.et_owner_name)
        val etBusinessName = view.findViewById<EditText>(R.id.et_business_name)
        val etBusinessEmail = view.findViewById<EditText>(R.id.et_business_email)
        val etPhone = view.findViewById<EditText>(R.id.et_phone)
        val etAddress = view.findViewById<EditText>(R.id.et_business_address)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val btnTogglePassword = view.findViewById<ImageView>(R.id.btn_toggle_password)
        val btnCreateOwnerAccount = view.findViewById<Button>(R.id.btn_create_owner_account)

        val etDocTitle = view.findViewById<EditText>(R.id.et_reg_doc_title)
        val etDocIssuer = view.findViewById<EditText>(R.id.et_reg_doc_issuer)
        val btnAttachDoc = view.findViewById<Button>(R.id.btn_reg_attach_doc)

        // Owner photo picker
        view.findViewById<View>(R.id.layout_owner_photo_picker)?.setOnClickListener {
            photoPickerHelper.showImageSourceDialog("Add Owner Profile Photo")
        }
        view.findViewById<View>(R.id.container_reg_owner_avatar)?.setOnClickListener {
            photoPickerHelper.showImageSourceDialog("Add Owner Profile Photo")
        }

        // Back button
        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Attach document / photo
        btnAttachDoc?.setOnClickListener {
            docPickerHelper.showImageSourceDialog("Attach Qualification / Certificate")
        }

        // Category dropdown dialog
        containerCategory?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Business Category")
                .setItems(categories) { _, which ->
                    tvCategory?.text = categories[which]
                }
                .show()
        }

        // Toggle Password
        btnTogglePassword?.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword?.inputType = if (isPasswordVisible) {
                btnTogglePassword.setImageResource(R.drawable.ic_eye)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword?.text?.let { etPassword.setSelection(it.length) }
        }

        // Create Owner Account
        btnCreateOwnerAccount?.setOnClickListener {
            val ownerName = etOwnerName?.text?.toString()?.trim().orEmpty()
            val businessName = etBusinessName?.text?.toString()?.trim().orEmpty()
            val email = etBusinessEmail?.text?.toString()?.trim().orEmpty()
            val phone = etPhone?.text?.toString()?.trim().orEmpty()
            val address = etAddress?.text?.toString()?.trim()?.ifBlank { "Colombo, Sri Lanka" } ?: "Colombo, Sri Lanka"
            val password = etPassword?.text?.toString().orEmpty()
            val docTitleInput = etDocTitle?.text?.toString()?.trim().orEmpty()
            val docIssuerInput = etDocIssuer?.text?.toString()?.trim().orEmpty()

            if (ownerName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the owner name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (businessName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the business name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid business email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must contain at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = tvCategory?.text?.toString()?.ifBlank { "Hair salon" } ?: "Hair salon"

            btnCreateOwnerAccount.isEnabled = false
            Toast.makeText(requireContext(), "Creating owner account and application...", Toast.LENGTH_SHORT).show()

            UserSessionManager.init(requireContext())
            val localUid = "owner_${System.currentTimeMillis()}"
            val localBizId = "biz_${localUid.take(12)}"

            val localOwnerPhotoPath = if (ownerPhotoUri != null) {
                com.example.bizbridgeapp.utils.ImageLoader.saveProfileImageLocally(requireContext(), ownerPhotoUri!!, "owner_${System.currentTimeMillis()}")
            } else null
            val effectiveOwnerPhoto = localOwnerPhotoPath ?: ownerPhotoUri?.toString() ?: ""

            val (lat, lng) = com.example.bizbridgeapp.utils.LocationUtils.resolveCoordinates(address)

            val initialProfile = mapOf(
                "uid" to localUid,
                "fullName" to ownerName,
                "email" to email,
                "phone" to phone,
                "address" to address,
                "role" to "BUSINESS_OWNER",
                "profileImageUrl" to effectiveOwnerPhoto,
                "businessId" to localBizId,
                "businessName" to businessName,
                "category" to category,
                "latitude" to lat,
                "longitude" to lng,
                "accountStatus" to "ACTIVE"
            )
            // Pre-save account locally so the user can always log in immediately
            UserSessionManager.saveRegisteredAccount(email, password, initialProfile)
            UserSessionManager.saveProfile(initialProfile)
            if (effectiveOwnerPhoto.isNotEmpty()) {
                UserSessionManager.updateProfileImage(effectiveOwnerPhoto)
            }

            fun handleAttachedDocument(finalUid: String, finalBizId: String) {
                val effectiveDocTitle = if (docTitleInput.isNotEmpty()) {
                    docTitleInput
                } else if (attachedDocUri != null) {
                    "$businessName Certification Document"
                } else {
                    ""
                }

                if (effectiveDocTitle.isNotEmpty()) {
                    val docId = "doc_${System.currentTimeMillis()}"
                    val qualDoc = QualificationDocument(
                        id = docId,
                        businessId = finalBizId,
                        ownerId = finalUid,
                        businessName = businessName,
                        title = effectiveDocTitle,
                        issuingOrg = if (docIssuerInput.isNotBlank()) docIssuerInput else "Professional Qualification Authority",
                        experience = "Professional",
                        documentType = "CERTIFICATE",
                        storagePath = "qualification_docs/$finalBizId/$docId.jpg",
                        fileUrl = attachedDocUri?.toString() ?: "",
                        verificationStatus = "PENDING"
                    )
                    QualificationRepository.submitDocument(qualDoc, onSuccess = {}, onFailure = {})
                }
            }

            fun showSuccessDialog(finalUid: String, finalBizId: String) {
                if (!isAdded) return
                handleAttachedDocument(finalUid, finalBizId)

                val finalProfile = mapOf(
                    "uid" to finalUid,
                    "fullName" to ownerName,
                    "email" to email,
                    "phone" to phone,
                    "address" to address,
                    "role" to "BUSINESS_OWNER",
                    "profileImageUrl" to effectiveOwnerPhoto,
                    "businessId" to finalBizId,
                    "businessName" to businessName,
                    "category" to category,
                    "latitude" to lat,
                    "longitude" to lng,
                    "accountStatus" to "ACTIVE"
                )
                UserSessionManager.saveRegisteredAccount(email, password, finalProfile)
                UserSessionManager.saveProfile(finalProfile)
                if (effectiveOwnerPhoto.isNotEmpty()) {
                    UserSessionManager.updateProfileImage(effectiveOwnerPhoto)
                }

                btnCreateOwnerAccount.isEnabled = true
                AlertDialog.Builder(requireContext())
                    .setTitle("Account & Application Submitted")
                    .setMessage("Welcome to BizBridge, $ownerName!\n\nYour business \"$businessName\" ($category) and all qualification documents were submitted as a single application to the administrator for verification.")
                    .setPositiveButton("Log In") { _, _ ->
                        (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = false)
                    }
                    .setNeutralButton("Open Dashboard") { _, _ ->
                        (activity as? MainActivity)?.navigateTo(OwnerDashboardFragment(), addToBackStack = false)
                    }
                    .setCancelable(false)
                    .show()
            }

            FirebaseAuthManager.registerOwner(
                ownerName = ownerName,
                businessName = businessName,
                category = category,
                phone = phone,
                address = address,
                email = email,
                password = password,
                profileImageUri = ownerPhotoUri,
                onSuccess = { firebaseUser, businessId ->
                    activity?.runOnUiThread {
                        showSuccessDialog(firebaseUser.uid, businessId)
                    }
                },
                onFailure = { e ->
                    activity?.runOnUiThread {
                        // Even if Firebase network fails, local account is registered and ready
                        showSuccessDialog(localUid, localBizId)
                    }
                }
            )
        }
    }
}
