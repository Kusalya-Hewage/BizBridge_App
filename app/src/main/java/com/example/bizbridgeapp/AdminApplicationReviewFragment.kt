package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Navigate to Screen A04 (Verify qualification)
        val openVerifyDoc = View.OnClickListener {
            val docId = "doc_${currentApplication?.businessId?.removePrefix("biz_") ?: "qual"}"
            (activity as? MainActivity)?.navigateTo(AdminVerifyQualificationFragment.newInstance(docId), addToBackStack = true)
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
                        bindApplication(view, app)
                    }
                },
                onFailure = {}
            )
        }

        // Approve action
        btnApprove?.setOnClickListener {
            val adminUser = FirebaseAuthManager.getCurrentUser()
            val adminUid = adminUser?.uid ?: "admin_alex"
            val adminEmail = adminUser?.email ?: "admin@bizbridge.com"
            val appId = if (applicationId.isNotEmpty()) applicationId else "app_hairloft"

            btnApprove.isEnabled = false
            Toast.makeText(requireContext(), "Approving application...", Toast.LENGTH_SHORT).show()

            AdminRepository.approveApplication(
                applicationId = appId,
                adminUid = adminUid,
                adminEmail = adminEmail,
                onSuccess = {
                    activity?.runOnUiThread {
                        btnApprove.isEnabled = true
                        Toast.makeText(requireContext(), "Business approved! It is now active and verified.", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                },
                onFailure = { e ->
                    activity?.runOnUiThread {
                        btnApprove.isEnabled = true
                        Toast.makeText(requireContext(), "Application approved successfully.", Toast.LENGTH_SHORT).show()
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
            val adminEmail = adminUser?.email ?: "admin@bizbridge.com"
            val appId = if (applicationId.isNotEmpty()) applicationId else "app_hairloft"

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

    private fun bindApplication(rootView: View, app: BusinessApplication) {
        rootView.findViewById<TextView>(R.id.tv_review_business_name)?.text = app.businessName
        rootView.findViewById<TextView>(R.id.tv_review_submitted_info)?.text =
            "APPLICATION ID · ${app.id.uppercase()}"

        val badge = rootView.findViewById<TextView>(R.id.badge_review_status)
        when (app.status.uppercase()) {
            "APPROVED" -> {
                badge?.text = "Approved"
                badge?.setBackgroundResource(R.drawable.bg_admin_badge_active)
                badge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_badge_active_text))
            }
            "REJECTED" -> {
                badge?.text = "Rejected"
                badge?.setBackgroundResource(R.drawable.bg_badge_cancelled)
                badge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_text))
            }
            else -> {
                badge?.text = "Pending Review"
                badge?.setBackgroundResource(R.drawable.bg_admin_badge_pending)
                badge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_badge_pending_text))
            }
        }

        val initials = app.ownerName.trim().split(" ")
            .filter { it.isNotEmpty() }
            .map { it.first().uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "BO" }

        rootView.findViewById<TextView>(R.id.tv_review_owner_initials)?.text = initials
        rootView.findViewById<TextView>(R.id.tv_review_owner_name)?.text = "${app.ownerName} · Owner"
        rootView.findViewById<TextView>(R.id.tv_review_owner_contact)?.text =
            "${app.email} · ${app.contactNumber.ifEmpty { "No phone number" }}"

        rootView.findViewById<TextView>(R.id.tv_review_category)?.text = app.category
        rootView.findViewById<TextView>(R.id.tv_review_address)?.text = app.address

        rootView.findViewById<TextView>(R.id.tv_review_description)?.text =
            "“Registration submitted for ${app.businessName}. Services in ${app.category} category located at ${app.address}.”"

        // Load submitted qualification documents for this business/owner
        val docsContainer = rootView.findViewById<android.widget.LinearLayout>(R.id.ll_admin_submitted_docs_container)
        if (docsContainer != null) {
            com.example.bizbridgeapp.repository.QualificationRepository.getDocumentsForBusiness(
                businessId = app.businessId,
                ownerId = app.ownerId,
                onSuccess = { docs ->
                    if (!isAdded || docs.isEmpty()) return@getDocumentsForBusiness
                    docsContainer.removeAllViews()

                    for (doc in docs) {
                        val docCard = android.widget.LinearLayout(requireContext()).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = (10 * resources.displayMetrics.density).toInt()
                            }
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER_VERTICAL
                            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_admin_card)
                            elevation = (1 * resources.displayMetrics.density)
                            val pad = (14 * resources.displayMetrics.density).toInt()
                            setPadding(pad, pad, pad, pad)
                        }

                        val icon = android.widget.ImageView(requireContext()).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                (24 * resources.displayMetrics.density).toInt(),
                                (24 * resources.displayMetrics.density).toInt()
                            ).apply {
                                marginEnd = (12 * resources.displayMetrics.density).toInt()
                            }
                            setImageResource(R.drawable.ic_admin_certificate_seal)
                            setColorFilter(ContextCompat.getColor(requireContext(), R.color.admin_primary))
                        }

                        val textLayout = android.widget.LinearLayout(requireContext()).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                0,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                            orientation = android.widget.LinearLayout.VERTICAL
                        }

                        val titleView = TextView(requireContext()).apply {
                            text = doc.title
                            textSize = 14f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_text_title))
                        }

                        val subtitleView = TextView(requireContext()).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = (2 * resources.displayMetrics.density).toInt()
                            }
                            val org = if (doc.issuingOrg.isNotBlank()) doc.issuingOrg else "Professional Credential"
                            text = "$org · Status: ${doc.verificationStatus}"
                            textSize = 12f
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_text_muted))
                        }

                        textLayout.addView(titleView)
                        textLayout.addView(subtitleView)

                        val actionBtn = TextView(requireContext()).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_admin_pill_button)
                            val padH = (12 * resources.displayMetrics.density).toInt()
                            val padV = (6 * resources.displayMetrics.density).toInt()
                            setPadding(padH, padV, padH, padV)
                            textSize = 12f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            isClickable = true
                            isFocusable = true

                            when (doc.verificationStatus.uppercase()) {
                                "VERIFIED" -> {
                                    text = "Verified ✓"
                                    setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
                                }
                                "REJECTED" -> {
                                    text = "Rejected ✗"
                                    setTextColor(ContextCompat.getColor(requireContext(), R.color.error_text))
                                }
                                else -> {
                                    text = "Review"
                                    setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_primary))
                                }
                            }

                            setOnClickListener {
                                (activity as? MainActivity)?.navigateTo(
                                    AdminVerifyQualificationFragment.newInstance(doc.id),
                                    addToBackStack = true
                                )
                            }
                        }

                        docCard.addView(icon)
                        docCard.addView(textLayout)
                        docCard.addView(actionBtn)

                        docCard.setOnClickListener {
                            (activity as? MainActivity)?.navigateTo(
                                AdminVerifyQualificationFragment.newInstance(doc.id),
                                addToBackStack = true
                            )
                        }

                        docsContainer.addView(docCard)
                    }
                }
            )
        }
    }
}
