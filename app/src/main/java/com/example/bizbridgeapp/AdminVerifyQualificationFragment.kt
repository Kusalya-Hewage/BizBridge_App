package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.QualificationDocument
import com.example.bizbridgeapp.repository.QualificationRepository

class AdminVerifyQualificationFragment : Fragment() {

    private var documentId: String = ""
    private var currentDocument: QualificationDocument? = null

    companion object {
        private const val ARG_DOC_ID = "arg_document_id"

        fun newInstance(documentId: String = ""): AdminVerifyQualificationFragment {
            val fragment = AdminVerifyQualificationFragment()
            val args = Bundle()
            args.putString(ARG_DOC_ID, documentId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        documentId = arguments?.getString(ARG_DOC_ID).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_verify_qualification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val tvTitle = view.findViewById<TextView>(R.id.tv_verify_cert_title)
        val tvIssuer = view.findViewById<TextView>(R.id.tv_verify_cert_issuer)
        val tvOwnerInfo = view.findViewById<TextView>(R.id.tv_verify_cert_owner_info)
        val tvCredentialId = view.findViewById<TextView>(R.id.tv_verify_cert_credential_id)
        val tvMetadata = view.findViewById<TextView>(R.id.tv_verify_cert_metadata)
        val etNotes = view.findViewById<EditText>(R.id.et_verify_notes)

        // Load document from Firestore
        if (documentId.isNotBlank()) {
            QualificationRepository.getDocumentById(
                docId = documentId,
                onSuccess = { doc ->
                    if (isAdded) {
                        if (doc != null) {
                            currentDocument = doc
                            tvTitle?.text = doc.title
                            if (doc.issuingOrg.isNotBlank()) tvIssuer?.text = doc.issuingOrg.uppercase()
                            tvOwnerInfo?.text = "${doc.businessName.ifEmpty { "Business" }} · ${doc.experience.ifEmpty { "Credential" }}"
                            tvCredentialId?.text = "STATUS: ${doc.verificationStatus.uppercase()} · ${doc.id.uppercase()}"
                            tvMetadata?.text = "Business ID: ${doc.businessId} · Submitted: ${doc.documentType}"
                        } else {
                            tvTitle?.text = "Document not found"
                            tvIssuer?.text = ""
                            tvOwnerInfo?.text = "The requested document could not be located."
                            tvCredentialId?.text = "STATUS: UNAVAILABLE"
                            tvMetadata?.text = "Document ID: $documentId"
                        }
                    }
                },
                onFailure = {
                    if (isAdded) {
                        tvTitle?.text = "Error loading document"
                    }
                }
            )
        } else {
            tvTitle?.text = "No document selected"
        }

        val adminUser = FirebaseAuthManager.getCurrentUser()
        val adminUid = adminUser?.uid ?: ""
        val adminEmail = adminUser?.email ?: ""

        view.findViewById<View>(R.id.btn_verify_document)?.setOnClickListener {
            if (documentId.isBlank()) {
                Toast.makeText(requireContext(), "No document ID specified.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val btnVerify = it
            btnVerify.isEnabled = false
            QualificationRepository.verifyDocument(
                docId = documentId,
                adminUid = adminUid,
                adminEmail = adminEmail,
                onSuccess = {
                    if (isAdded) {
                        btnVerify.isEnabled = true
                        Toast.makeText(requireContext(), "Document verified and Trust Score updated!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                },
                onFailure = { e ->
                    if (isAdded) {
                        btnVerify.isEnabled = true
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to verify document.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        view.findViewById<View>(R.id.btn_reject_document)?.setOnClickListener {
            if (documentId.isBlank()) {
                Toast.makeText(requireContext(), "No document ID specified.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val reason = etNotes?.text?.toString()?.trim().orEmpty().ifEmpty { "Document unreadable or invalid credentials" }
            val btnReject = it
            btnReject.isEnabled = false

            QualificationRepository.rejectDocument(
                docId = documentId,
                adminUid = adminUid,
                adminEmail = adminEmail,
                reason = reason,
                onSuccess = {
                    if (isAdded) {
                        btnReject.isEnabled = true
                        Toast.makeText(requireContext(), "Document rejected.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                },
                onFailure = { e ->
                    if (isAdded) {
                        btnReject.isEnabled = true
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to reject document.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
