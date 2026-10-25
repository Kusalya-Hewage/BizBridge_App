package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class OwnerQualificationsDocumentsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_qualifications_documents, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_qualifications)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Upload Button
        view.findViewById<Button>(R.id.btn_upload_document)?.setOnClickListener {
            Toast.makeText(requireContext(), "Uploading certificate.pdf · 60% (Demonstration).", Toast.LENGTH_SHORT).show()
        }

        // Resubmit document
        view.findViewById<TextView>(R.id.btn_resubmit_document)?.setOnClickListener {
            Toast.makeText(requireContext(), "Upload submitted · Pending review.", Toast.LENGTH_SHORT).show()
        }
    }
}
