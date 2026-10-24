package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

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

        view.findViewById<View>(R.id.btn_keep_review)?.setOnClickListener {
            Toast.makeText(requireContext(), "Review kept. Report RPT-0920 dismissed.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btn_hide_review)?.setOnClickListener {
            Toast.makeText(requireContext(), "Review hidden from public view.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}
