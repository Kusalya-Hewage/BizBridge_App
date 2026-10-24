package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class NotificationsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mark all as read
        view.findViewById<TextView>(R.id.btn_mark_all_read)?.setOnClickListener {
            Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }

        // Notification items click
        view.findViewById<LinearLayout>(R.id.notif_item_1)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment(), addToBackStack = true)
        }

        view.findViewById<LinearLayout>(R.id.notif_item_2)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment(), addToBackStack = true)
        }

        view.findViewById<LinearLayout>(R.id.notif_item_3)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment(), addToBackStack = true)
        }

        view.findViewById<LinearLayout>(R.id.notif_item_4)?.setOnClickListener {
            Toast.makeText(requireContext(), "Booking BB-24090 was canceled", Toast.LENGTH_SHORT).show()
        }

        // Bottom Navigation
        view.findViewById<LinearLayout>(R.id.nav_home)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
        }
        view.findViewById<LinearLayout>(R.id.nav_search)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_bookings)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(MyBookingsFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_profile)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerProfileFragment(), addToBackStack = true)
        }
    }
}
