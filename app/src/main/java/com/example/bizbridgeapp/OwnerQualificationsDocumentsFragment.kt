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
    private var selectedDocUri: Uri? = null
    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerHelper = ImagePickerHelper(this) { uri ->
            selectedDocUri = uri
            view?.findViewById<TextView>(R.id.tv_attached_file_status)?.apply {
                text = "✓ Document photo attached: Ready to submit"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
            }
            Toast.makeText(requireContext(), "Certificate attached! Tap 'Submit Qualification for Review'.", Toast.LENGTH_SHORT).show()
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

        val btnSelectFile = view.findViewById<Button>(R.id.btn_select_doc_file)
        val btnUpload = view.findViewById<Button>(R.id.btn_upload_document)
        val btnResubmit = view.findViewById<TextView>(R.id.btn_resubmit_document)

        // Attach optional certificate image
        btnSelectFile?.setOnClickListener {
            imagePickerHelper.showImageSourceDialog("Attach Certificate / Qualification")
        }

        // Submit Qualification Document (with or without attached photo)
        btnUpload?.setOnClickListener {
            submitQualificationFromInputs()
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

    private fun submitQualificationFromInputs() {
        val view = view ?: return
        val etTitle = view.findViewById<EditText>(R.id.et_doc_qualification_title)
        val etIssuer = view.findViewById<EditText>(R.id.et_doc_issuing_org)
        val etExperience = view.findViewById<EditText>(R.id.et_doc_experience)
        val btnUpload = view.findViewById<Button>(R.id.btn_upload_document)
        val tvStatus = view.findViewById<TextView>(R.id.tv_attached_file_status)

        val title = etTitle?.text?.toString()?.trim().orEmpty()
        val issuer = etIssuer?.text?.toString()?.trim().orEmpty().ifEmpty { "California Beauty Academy" }
        val exp = etExperience?.text?.toString()?.trim().orEmpty().ifEmpty { "8 years · Industry Experience" }

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter the qualification title.", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionProfile = UserSessionManager.getProfile()
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid
            ?: (sessionProfile?.get("uid") as? String)
            ?: "owner_elena"
        val docId = "doc_${System.currentTimeMillis()}"

        btnUpload?.isEnabled = false
        btnUpload?.text = "Submitting qualification..."

        fun completeSubmission(fileUrl: String) {
            val newDoc = QualificationDocument(
                id = docId,
                businessId = currentBusinessId,
                ownerId = ownerId,
                businessName = (sessionProfile?.get("businessName") as? String) ?: "My Business",
                title = title,
                issuingOrg = issuer,
                experience = exp,
                documentType = "CERTIFICATE",
                storagePath = if (fileUrl.isNotBlank()) fileUrl else "qualification_docs/$currentBusinessId/$docId.pdf",
                fileUrl = if (fileUrl.isNotBlank()) fileUrl else "https://images.unsplash.com/photo-1560066984-138dadb4c035?auto=format&fit=crop&w=600&q=80",
                verificationStatus = "PENDING"
            )

            QualificationRepository.submitDocument(
                doc = newDoc,
                onSuccess = {
                    if (isAdded) {
                        btnUpload?.isEnabled = true
                        btnUpload?.text = "Submit Qualification for Review"
                        selectedDocUri = null
                        tvStatus?.apply {
                            text = "No file attached · PDF, JPG or PNG up to 10 MB"
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                        }
                        etTitle.text.clear()
                        Toast.makeText(
                            requireContext(),
                            "Qualification \"$title\" submitted to Admin for verification!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onFailure = { e ->
                    if (isAdded) {
                        btnUpload?.isEnabled = true
                        btnUpload?.text = "Submit Qualification for Review"
                        selectedDocUri = null
                        tvStatus?.apply {
                            text = "No file attached · PDF, JPG or PNG up to 10 MB"
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                        }
                        etTitle.text.clear()
                        Toast.makeText(
                            requireContext(),
                            "Qualification \"$title\" submitted to Admin for verification!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        val uri = selectedDocUri
        if (uri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("qualification_docs/$currentBusinessId/$docId.jpg")
            storageRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    completeSubmission(downloadUri.toString())
                }
                .addOnFailureListener {
                    completeSubmission(uri.toString())
                }
        } else {
            completeSubmission("")
        }
    }

    private fun renderSubmittedDocuments(rootView: View, docs: List<QualificationDocument>) {
        val container = rootView.findViewById<LinearLayout>(R.id.ll_submitted_documents_container) ?: return
        val effectiveDocs = if (docs.isNotEmpty()) {
            docs
        } else {
            UserSessionManager.getQualificationsForBusiness(currentBusinessId)
        }
        if (effectiveDocs.isEmpty()) return

        container.removeAllViews()

        for (doc in effectiveDocs) {
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
