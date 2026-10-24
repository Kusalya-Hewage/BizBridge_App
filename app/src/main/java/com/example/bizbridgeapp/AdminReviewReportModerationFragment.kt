package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.ReportRepository

class AdminReviewReportModerationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_review_report_moderation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val adminUser = FirebaseAuthManager.getCurrentUser()
        val adminUid = adminUser?.uid ?: "admin_alex"
        val adminEmail = adminUser?.email ?: "alex@bizbridge.example"

        view.findViewById<View>(R.id.btn_keep_review)?.setOnClickListener {
            ReportRepository.resolveReport(
                reportId = "rpt_0920",
                resolutionNotes = "Report dismissed. Review kept active.",
                adminUid = adminUid,
                adminEmail = adminEmail,
                onSuccess = {
                    Toast.makeText(requireContext(), "Review kept. Report dismissed.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Review kept. Report dismissed.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            )
        }

        view.findViewById<View>(R.id.btn_hide_review)?.setOnClickListener {
            ReportRepository.resolveReport(
                reportId = "rpt_0920",
                resolutionNotes = "Review hidden due to policy violation.",
                adminUid = adminUid,
                adminEmail = adminEmail,
                onSuccess = {
                    Toast.makeText(requireContext(), "Review hidden from public view.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Review hidden from public view.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            )
        }
    }
}
