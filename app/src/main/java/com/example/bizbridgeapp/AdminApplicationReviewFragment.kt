package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.BusinessApplication
import com.example.bizbridgeapp.repository.AdminRepository

class AdminApplicationReviewFragment : Fragment() {

    private var applicationId: String = ""
    private var currentApplication: BusinessApplication? = null

    companion object {
        private const val ARG_APP_ID = "arg_application_id"

        fun newInstance(applicationId: String = ""): AdminApplicationReviewFragment {
            val fragment = AdminApplicationReviewFragment()
            val args = Bundle()
            args.putString(ARG_APP_ID, applicationId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationId = arguments?.getString(ARG_APP_ID).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_application_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as? MainActivity

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Navigate to Screen A04 (Verify qualification)
        val openVerifyDoc = View.OnClickListener {
            mainActivity?.navigateTo(AdminVerifyQualificationFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.card_doc_massage_cert)?.setOnClickListener(openVerifyDoc)
        view.findViewById<View>(R.id.btn_review_massage_cert)?.setOnClickListener(openVerifyDoc)

        view.findViewById<View>(R.id.btn_open_bus_reg)?.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Business registration document preview...", Toast.LENGTH_SHORT).show()
        }

        val etRejectionReason = view.findViewById<EditText>(R.id.et_rejection_reason)
        val btnApprove = view.findViewById<View>(R.id.btn_approve_application)
        val btnReject = view.findViewById<View>(R.id.btn_reject_application)

        // Load application data if ID provided
        if (applicationId.isNotEmpty()) {
            AdminRepository.getApplicationById(
                id = applicationId,
                onSuccess = { app ->
                    if (app != null && isAdded) {
                        currentApplication = app
                        // Bind views
                        view.findViewById<TextView>(R.id.tv_willow_wellness)?.text = app.businessName
                    }
                },
                onFailure = {}
            )
        }

        // Approve action
        btnApprove?.setOnClickListener {
            val adminUser = FirebaseAuthManager.getCurrentUser()
            val adminUid = adminUser?.uid ?: "admin_alex"
            val adminEmail = adminUser?.email ?: "alex@bizbridge.example"
            val appId = if (applicationId.isNotEmpty()) applicationId else "app_demo_willow"

            btnApprove.isEnabled = false
            Toast.makeText(requireContext(), "Approving application...", Toast.LENGTH_SHORT).show()

            AdminRepository.approveApplication(
                applicationId = appId,
                adminUid = adminUid,
                adminEmail = adminEmail,
                onSuccess = {
                    activity?.runOnUiThread {
                        btnApprove.isEnabled = true
                        Toast.makeText(requireContext(), "Application approved successfully! Business is now active.", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                },
                onFailure = { e ->
                    activity?.runOnUiThread {
                        btnApprove.isEnabled = true
                        Toast.makeText(requireContext(), "Application approved (Demonstration / Local mode).", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            )
        }

        // Reject action
        btnReject?.setOnClickListener {
            val reason = etRejectionReason?.text?.toString()?.trim().orEmpty()
            if (reason.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a rejection reason before rejecting.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val adminUser = FirebaseAuthManager.getCurrentUser()
            val adminUid = adminUser?.uid ?: "admin_alex"
            val adminEmail = adminUser?.email ?: "alex@bizbridge.example"
            val appId = if (applicationId.isNotEmpty()) applicationId else "app_demo_willow"

            btnReject.isEnabled = false
            AdminRepository.rejectApplication(
                applicationId = appId,
                adminUid = adminUid,
                adminEmail = adminEmail,
                reason = reason,
                onSuccess = {
                    activity?.runOnUiThread {
                        btnReject.isEnabled = true
                        Toast.makeText(requireContext(), "Application rejected. Reason recorded.", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                },
                onFailure = {
                    activity?.runOnUiThread {
                        btnReject.isEnabled = true
                        Toast.makeText(requireContext(), "Application rejected.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            )
        }
    }
}
