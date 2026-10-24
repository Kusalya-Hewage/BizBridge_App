package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

class OwnerAvailabilityScheduleFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_availability_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_schedule)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btn_add_break)?.setOnClickListener {
            Toast.makeText(requireContext(), "Add break interval (Demonstration).", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_add_unavailable_date)?.setOnClickListener {
            Toast.makeText(requireContext(), "Select unavailable date (Demonstration).", Toast.LENGTH_SHORT).show()
        }

        val btnSave = view.findViewById<Button>(R.id.btn_save_schedule)
        btnSave?.setOnClickListener {
            btnSave.isEnabled = false
            btnSave.text = "Saving…"

            btnSave.postDelayed({
                if (isAdded) {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Schedule"
                    Toast.makeText(requireContext(), "Weekly schedule saved successfully.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }, 600)
        }
    }
}
