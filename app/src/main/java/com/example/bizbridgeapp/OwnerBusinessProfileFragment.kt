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

    private var currentBusinessId: String = "biz_juniper_studio"
    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            view?.findViewById<ImageView>(R.id.iv_cover_photo)?.setImageURI(uri)
            Toast.makeText(requireContext(), "Cover photo selected. Tap 'Save Changes' to upload.", Toast.LENGTH_SHORT).show()
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

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_profile)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Edit Cover / Logo
        view.findViewById<View>(R.id.btn_edit_cover)?.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        // Load current business from Firestore
        BusinessRepository.getBusinessById(
            businessId = currentBusinessId,
            onSuccess = { biz ->
                if (biz != null && isAdded) {
                    currentBusinessId = biz.id
                    currentImageUrl = biz.imageUrl
                    etName?.setText(biz.name)
                    etCategory?.setText(biz.category)
                    etDesc?.setText(biz.description)
                    etAddress?.setText(biz.address)
                    if (biz.contactNumber.isNotBlank()) etPhone?.setText(biz.contactNumber)
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
                            Toast.makeText(requireContext(), "Business profile saved to Firestore.", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    },
                    onFailure = { e ->
                        if (isAdded) {
                            btnSave.isEnabled = true
                            btnSave.text = "Save Changes"
                            Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to save.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // If a new cover photo was picked, upload to Firebase Storage
            val uri = selectedImageUri
            if (uri != null) {
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
                        // If storage fails, still save profile details
                        saveToFirestore(currentImageUrl)
                    }
            } else {
                saveToFirestore(currentImageUrl)
            }
        }
    }
}
