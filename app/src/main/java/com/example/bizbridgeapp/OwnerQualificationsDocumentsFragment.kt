package com.example.bizbridgeapp

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.QualificationDocument
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.QualificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

class OwnerQualificationsDocumentsFragment : Fragment() {

    private var docsListener: ListenerRegistration? = null
    private var currentBusinessId: String = "biz_juniper_studio"
    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerHelper = ImagePickerHelper(this) { uri ->
            uploadAndSubmitDocument(uri)
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

        // Resubmit document via Camera or Gallery
        btnResubmit?.setOnClickListener {
            imagePickerHelper.showImageSourceDialog("Resubmit Certificate")
        }

        // Real-time listener for Qualification Documents
        docsListener = QualificationRepository.listenToQualificationsForBusiness(currentBusinessId) { list ->
            if (!isAdded) return@listenToQualificationsForBusiness
            renderSubmittedDocuments(view, list)
        }
    }

    private fun uploadAndSubmitDocument(uri: Uri) {
        val view = view ?: return
        val etTitle = view.findViewById<EditText>(R.id.et_doc_qualification_title)
        val etIssuer = view.findViewById<EditText>(R.id.et_doc_issuing_org)
        val etExperience = view.findViewById<EditText>(R.id.et_doc_experience)
        val btnUpload = view.findViewById<Button>(R.id.btn_upload_document)

        val title = etTitle?.text?.toString()?.trim()?.ifEmpty { "Professional Qualification Certificate" }
            ?: "Professional Qualification Certificate"
        val issuer = etIssuer?.text?.toString()?.trim()?.ifEmpty { "Issuing Authority" } ?: "Issuing Authority"
        val exp = etExperience?.text?.toString()?.trim()?.ifEmpty { "Industry Experience" } ?: "Industry Experience"

        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: "owner_bizbridge"
        val docId = "doc_${System.currentTimeMillis()}"

        btnUpload?.isEnabled = false
        btnUpload?.text = "Uploading document..."

        fun saveDocumentRecord(downloadUrl: String) {
            val newDoc = QualificationDocument(
                id = docId,
                businessId = currentBusinessId,
                ownerId = ownerId,
                title = title,
                issuingOrg = issuer,
                experience = exp,
                documentType = "PROFESSIONAL_CERT",
                storagePath = downloadUrl,
                fileUrl = downloadUrl,
                verificationStatus = "PENDING"
            )

            QualificationRepository.submitDocument(
                doc = newDoc,
                onSuccess = {
                    if (isAdded) {
                        btnUpload?.isEnabled = true
                        btnUpload?.text = "Upload Certificate / Document"
                        Toast.makeText(
                            requireContext(),
                            "Document \"$title\" submitted to Admin for verification!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onFailure = { e ->
                    if (isAdded) {
                        btnUpload?.isEnabled = true
                        btnUpload?.text = "Upload Certificate / Document"
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to save document.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Upload certificate image to Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference.child("qualification_docs/$currentBusinessId/$docId.jpg")
        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                saveDocumentRecord(downloadUri.toString())
            }
            .addOnFailureListener {
                // If storage fails or is unconfigured, record metadata directly in Firestore
                saveDocumentRecord("")
            }
    }

    private fun renderSubmittedDocuments(rootView: View, docs: List<QualificationDocument>) {
        val container = rootView.findViewById<LinearLayout>(R.id.ll_submitted_documents_container) ?: return
        if (docs.isEmpty()) return

        container.removeAllViews()

        for (doc in docs) {
            val card = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (10 * resources.displayMetrics.density).toInt()
                }
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_white)
                elevation = (1 * resources.displayMetrics.density)
                val pad = (16 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            }

            val topRow = RelativeLayout(requireContext()).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val tvTitle = TextView(requireContext()).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                }
                text = doc.title
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }

            val tvBadge = TextView(requireContext()).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                }
                val padH = (8 * resources.displayMetrics.density).toInt()
                val padV = (2 * resources.displayMetrics.density).toInt()
                setPadding(padH, padV, padH, padV)
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)

                when (doc.verificationStatus.uppercase()) {
                    "VERIFIED" -> {
                        text = "Verified"
                        background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_tag_verified)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
                    }
                    "REJECTED" -> {
                        text = "Rejected"
                        background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_rejected)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.error_text))
                    }
                    else -> {
                        text = "Pending"
                        background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_pending)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_pending_text))
                    }
                }
            }

            topRow.addView(tvTitle)
            topRow.addView(tvBadge)
            card.addView(topRow)

            val tvSub = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (3 * resources.displayMetrics.density).toInt()
                }
                val orgText = if (doc.issuingOrg.isNotBlank()) doc.issuingOrg else "Professional Credential"
                val statusText = when (doc.verificationStatus.uppercase()) {
                    "VERIFIED" -> "Verified by Admin"
                    "REJECTED" -> doc.rejectionReason.ifEmpty { "Verification rejected" }
                    else -> "Review pending by Admin"
                }
                text = "$orgText · $statusText"
                textSize = 12f
                setTextColor(
                    if (doc.verificationStatus.equals("REJECTED", ignoreCase = true)) {
                        ContextCompat.getColor(requireContext(), R.color.error_text)
                    } else {
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    }
                )
            }
            card.addView(tvSub)

            if (doc.verificationStatus.equals("REJECTED", ignoreCase = true)) {
                val tvResubmit = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (8 * resources.displayMetrics.density).toInt()
                    }
                    text = "Resubmit document →"
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    setOnClickListener {
                        imagePickerHelper.showImageSourceDialog("Resubmit ${doc.title}")
                    }
                }
                card.addView(tvResubmit)
            }

            container.addView(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        docsListener?.remove()
    }
}
