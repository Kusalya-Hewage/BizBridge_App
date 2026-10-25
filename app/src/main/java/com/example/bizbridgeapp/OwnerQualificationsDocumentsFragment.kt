package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.QualificationDocument
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.QualificationRepository
import com.google.firebase.firestore.ListenerRegistration

class OwnerQualificationsDocumentsFragment : Fragment() {

    private var docsListener: ListenerRegistration? = null
    private var currentBusinessId: String = "biz_juniper_studio"
    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerHelper = ImagePickerHelper(this) { uri ->
            val ownerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "owner_elena"
            val newDoc = QualificationDocument(
                businessId = currentBusinessId,
                ownerId = ownerId,
                title = "Professional Qualification Certificate",
                documentType = "PROFESSIONAL_CERT",
                verificationStatus = "PENDING"
            )

            QualificationRepository.submitDocument(
                doc = newDoc,
                onSuccess = {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Certificate photo captured! Submitted to Admin for verification.", Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { e ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to upload document.", Toast.LENGTH_SHORT).show()
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
        return inflater.inflate(R.layout.fragment_owner_qualifications_documents, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_qualifications)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val btnUpload = view.findViewById<Button>(R.id.btn_upload_document)
        val btnResubmit = view.findViewById<TextView>(R.id.btn_resubmit_document)

        // Upload new Qualification Document via Camera or Gallery
        btnUpload?.setOnClickListener {
            imagePickerHelper.showImageSourceDialog("Upload Certificate / Qualification")
        }

        btnResubmit?.setOnClickListener {
            imagePickerHelper.showImageSourceDialog("Resubmit Certificate")
        }
                        btnUpload.text = "Upload document"
                        Toast.makeText(requireContext(), "Document submitted to Admin for verification.", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { e ->
                    if (isAdded) {
                        btnUpload.isEnabled = true
                        btnUpload.text = "Upload document"
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to upload document.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Resubmit document
        btnResubmit?.setOnClickListener {
            Toast.makeText(requireContext(), "Document re-submission queued for Admin review.", Toast.LENGTH_SHORT).show()
        }

        // Real-time listener for Qualification Documents
        docsListener = QualificationRepository.listenToQualificationsForBusiness(currentBusinessId) { list ->
            if (!isAdded) return@listenToQualificationsForBusiness
            // Live documents verified by Admin will automatically sync here!
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        docsListener?.remove()
    }
}
