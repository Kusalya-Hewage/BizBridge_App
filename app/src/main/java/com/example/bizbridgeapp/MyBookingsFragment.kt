package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class MyBookingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bell
        view.findViewById<ImageView>(R.id.btn_bell)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }

        // View details for card 1 (BB-24102) -> navigates to C10 BookingDetailsFragment
        view.findViewById<TextView>(R.id.btn_details_1)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.card_booking_1)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment(), addToBackStack = true)
        }

        view.findViewById<TextView>(R.id.btn_details_2)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment(), addToBackStack = true)
        }
        view.findViewById<TextView>(R.id.btn_details_3)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment(), addToBackStack = true)
        }

        // Write a review for card 3 -> navigates to C11 WriteReviewFragment
        view.findViewById<TextView>(R.id.btn_write_review)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(WriteReviewFragment(), addToBackStack = true)
        }

        // Cancel clicks
        view.findViewById<TextView>(R.id.btn_cancel_1)?.setOnClickListener {
            Toast.makeText(requireContext(), "Booking BB-24102 cancellation flow", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<TextView>(R.id.btn_cancel_2)?.setOnClickListener {
            Toast.makeText(requireContext(), "Request BB-24110 cancelled", Toast.LENGTH_SHORT).show()
        }

        // Bottom Navigation
        view.findViewById<LinearLayout>(R.id.nav_home)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
        }
        view.findViewById<LinearLayout>(R.id.nav_search)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_notifications)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_profile)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerProfileFragment(), addToBackStack = true)
        }
    }
}
