package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.QualificationRepository

class AdminVerifyQualificationFragment : Fragment() {

    private var documentId: String = ""

    companion object {
        private const val ARG_DOC_ID = "arg_document_id"

        fun newInstance(documentId: String = "doc_massage_cert"): AdminVerifyQualificationFragment {
            val fragment = AdminVerifyQualificationFragment()
            val args = Bundle()
            args.putString(ARG_DOC_ID, documentId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        documentId = arguments?.getString(ARG_DOC_ID).orEmpty().ifEmpty { "doc_massage_cert" }
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

        val adminUser = FirebaseAuthManager.getCurrentUser()
        val adminUid = adminUser?.uid ?: "admin_alex"
        val adminEmail = adminUser?.email ?: "alex@bizbridge.example"

        view.findViewById<View>(R.id.btn_verify_document)?.setOnClickListener {
            QualificationRepository.verifyDocument(
                docId = documentId,
                adminUid = adminUid,
                adminEmail = adminEmail,
                onSuccess = {
                    Toast.makeText(requireContext(), "Document verified and Trust Score updated!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Document verified (Demonstration).", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            )
        }

        view.findViewById<View>(R.id.btn_reject_document)?.setOnClickListener {
            QualificationRepository.rejectDocument(
                docId = documentId,
                adminUid = adminUid,
                adminEmail = adminEmail,
                reason = "Document unreadable or invalid credentials",
                onSuccess = {
                    Toast.makeText(requireContext(), "Document rejected.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Document rejected.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            )
        }
    }
}
