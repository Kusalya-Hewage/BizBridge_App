package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class AdminApplicationReviewFragment : Fragment() {

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

        view.findViewById<View>(R.id.btn_approve_application)?.setOnClickListener {
            Toast.makeText(requireContext(), "Willow Wellness application approved!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btn_reject_application)?.setOnClickListener {
            Toast.makeText(requireContext(), "Willow Wellness application rejected.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}
