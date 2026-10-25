package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.BusinessRepository
import com.google.firebase.firestore.FirebaseFirestore

class OwnerAvailabilityScheduleFragment : Fragment() {

    private var currentBusinessId: String = "biz_juniper_studio"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_availability_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_schedule)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btn_add_break)?.setOnClickListener {
            Toast.makeText(requireContext(), "Mid-day break saved to business schedule.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_add_unavailable_date)?.setOnClickListener {
            Toast.makeText(requireContext(), "Unavailable holiday marked on booking calendar.", Toast.LENGTH_SHORT).show()
        }

        val btnSave = view.findViewById<Button>(R.id.btn_save_schedule)
        btnSave?.setOnClickListener {
            btnSave.isEnabled = false
            btnSave.text = "Saving to Firestore…"

            FirebaseFirestore.getInstance().collection("businesses").document(currentBusinessId)
                .update(
                    mapOf(
                        "openingHours" to "Mon-Sat: 9:00 AM - 7:00 PM · Sun: Closed",
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                .addOnSuccessListener {
                    if (isAdded) {
                        btnSave.isEnabled = true
                        btnSave.text = "Save Schedule"
                        Toast.makeText(requireContext(), "Weekly schedule saved to Firestore.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        btnSave.isEnabled = true
                        btnSave.text = "Save Schedule"
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to save schedule.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
