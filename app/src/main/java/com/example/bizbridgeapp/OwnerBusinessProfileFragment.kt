package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

class OwnerBusinessProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_business_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_profile)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Edit Cover / Logo
        view.findViewById<View>(R.id.btn_edit_cover)?.setOnClickListener {
            Toast.makeText(requireContext(), "Select a new logo or cover photo (Demonstration).", Toast.LENGTH_SHORT).show()
        }

        // Save Changes Button
        val btnSave = view.findViewById<Button>(R.id.btn_save_business_profile)
        btnSave?.setOnClickListener {
            val name = view.findViewById<EditText>(R.id.et_profile_name)?.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Business name is required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Saving…"

            btnSave.postDelayed({
                if (isAdded) {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Changes"
                    Toast.makeText(requireContext(), "Changes saved successfully.", Toast.LENGTH_SHORT).show()
                }
            }, 800)
        }
    }
}
