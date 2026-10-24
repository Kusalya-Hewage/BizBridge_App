package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class AdminVerifyQualificationFragment : Fragment() {

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

        view.findViewById<View>(R.id.btn_verify_document)?.setOnClickListener {
            Toast.makeText(requireContext(), "Document verified successfully!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btn_reject_document)?.setOnClickListener {
            Toast.makeText(requireContext(), "Document rejected.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}
