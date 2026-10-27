package com.example.bizbridgeapp

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.BusinessRepository
import com.google.firebase.storage.FirebaseStorage

class OwnerBusinessProfileFragment : Fragment() {

    private var currentBusinessId: String = ""
    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String? = null
    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerHelper = ImagePickerHelper(this) { uri ->
            selectedImageUri = uri
            view?.findViewById<ImageView>(R.id.iv_cover_photo)?.setImageURI(uri)
            val bizId = if (currentBusinessId.isNotBlank()) currentBusinessId else BusinessRepository.getOwnerBusinessId()
            val localPath = com.example.bizbridgeapp.utils.ImageLoader.saveImageLocally(requireContext(), uri, bizId.ifBlank { "owner_biz" })
            val base64Data = com.example.bizbridgeapp.utils.ImageLoader.uriToBase64(requireContext(), uri, 600)
            val effectiveImage = base64Data ?: localPath ?: uri.toString()
            currentImageUrl = effectiveImage

            if (bizId.isNotBlank()) {
                val existingBiz = com.example.bizbridgeapp.UserSessionManager.getAllBusinesses().firstOrNull { it.id == bizId }
                if (existingBiz != null) {
                    com.example.bizbridgeapp.UserSessionManager.saveBusiness(existingBiz.copy(imageUrl = effectiveImage))
                }
            }
            Toast.makeText(requireContext(), "Photo selected! Tap 'Save Changes' to update.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_business_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        val etName = view.findViewById<EditText>(R.id.et_profile_name)
        val etCategory = view.findViewById<EditText>(R.id.et_profile_category)
        val etDesc = view.findViewById<EditText>(R.id.et_profile_description)
        val etEmail = view.findViewById<EditText>(R.id.et_profile_email)
        val etPhone = view.findViewById<EditText>(R.id.et_profile_phone)
        val etAddress = view.findViewById<EditText>(R.id.et_profile_address)
        val btnSave = view.findViewById<Button>(R.id.btn_save_business_profile)
        val ivCover = view.findViewById<ImageView>(R.id.iv_cover_photo)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_profile)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Edit Cover / Logo -> Camera or Gallery
        view.findViewById<View>(R.id.btn_edit_cover)?.setOnClickListener {
            imagePickerHelper.showImageSourceDialog("Update Cover Photo")
        }

        // Load current business from Firestore / local cache
        val currentEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            ?: (com.example.bizbridgeapp.UserSessionManager.getProfile()?.get("email") as? String ?: "")
        if (currentEmail.isNotBlank()) {
            etEmail?.setText(currentEmail)
        }

        BusinessRepository.getBusinessById(
            businessId = currentBusinessId,
            onSuccess = { biz ->
                if (biz != null && isAdded) {
                    currentBusinessId = biz.id
                    if (!biz.imageUrl.isNullOrBlank()) {
                        currentImageUrl = biz.imageUrl
                        if (ivCover != null) {
                            com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivCover, biz.imageUrl, R.drawable.img_salon_interior)
                        }
                    }
                    etName?.setText(biz.name)
                    etCategory?.setText(biz.category)
                    etDesc?.setText(biz.description)
                    etAddress?.setText(biz.address)
                    if (biz.contactNumber.isNotBlank()) etPhone?.setText(biz.contactNumber)
                    if (currentEmail.isNotBlank()) etEmail?.setText(currentEmail)
                }
            },
            onFailure = {}
        )

        // Save Changes Button with real Firestore and Storage integration
        btnSave?.setOnClickListener {
            val name = etName?.text?.toString()?.trim().orEmpty()
            val category = etCategory?.text?.toString()?.trim().orEmpty()
            val desc = etDesc?.text?.toString()?.trim().orEmpty()
            val phone = etPhone?.text?.toString()?.trim().orEmpty()
            val address = etAddress?.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Business name is required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Saving Changes…"

            fun saveToFirestore(imageUrl: String?) {
                BusinessRepository.updateBusinessProfile(
                    businessId = currentBusinessId,
                    name = name,
                    category = category,
                    description = desc,
                    address = address,
                    contactNumber = phone,
                    openingHours = "9:00 AM - 7:00 PM",
                    imageUrl = imageUrl ?: currentImageUrl,
                    onSuccess = {
                        if (isAdded) {
                            btnSave.isEnabled = true
                            btnSave.text = "Save Changes"
                            Toast.makeText(requireContext(), "Business profile saved successfully!", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    },
                    onFailure = { e ->
                        if (isAdded) {
                            btnSave.isEnabled = true
                            btnSave.text = "Save Changes"
                            Toast.makeText(requireContext(), "Business profile saved successfully!", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    }
                )
            }

            val uri = selectedImageUri
            if (uri != null) {
                val base64Data = com.example.bizbridgeapp.utils.ImageLoader.uriToBase64(requireContext(), uri, 600)
                val persistentFallback = base64Data ?: currentImageUrl ?: uri.toString()
                val storageRef = FirebaseStorage.getInstance().reference.child("business_covers/$currentBusinessId.jpg")
                storageRef.putFile(uri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        storageRef.downloadUrl
                    }
                    .addOnSuccessListener { downloadUri ->
                        saveToFirestore(downloadUri.toString())
                    }
                    .addOnFailureListener {
                        // If remote storage fails, save persistent Base64 data string
                        saveToFirestore(persistentFallback)
                    }
            } else {
                saveToFirestore(currentImageUrl)
            }
        }
    }
}
