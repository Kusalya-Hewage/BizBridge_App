package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

class BookingDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booking_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // View Demonstration QR -> navigates to C08 BookingConfirmationFragment
        view.findViewById<Button>(R.id.btn_view_demo_qr)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingConfirmationFragment(), addToBackStack = true)
        }

        // Cancel Booking
        view.findViewById<Button>(R.id.btn_cancel_booking)?.setOnClickListener {
            Toast.makeText(requireContext(), "Booking BB-24102 cancelled", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}
