package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

import com.example.bizbridgeapp.model.NotificationItem
import com.example.bizbridgeapp.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth

class NotificationsFragment : Fragment() {

    private var userNotifications: List<NotificationItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid.orEmpty()

        // Mark all as read
        view.findViewById<TextView>(R.id.btn_mark_all_read)?.setOnClickListener {
            if (uid.isNotEmpty()) {
                NotificationRepository.markAllAsRead(uid,
                    onSuccess = {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
                            loadNotifications(uid)
                        }
                    },
                    onFailure = { e ->
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        if (uid.isNotEmpty()) {
            loadNotifications(uid)
        }

        // Tabs
        val tabAll = view.findViewById<LinearLayout>(R.id.tab_all)
        val tabBookings = view.findViewById<LinearLayout>(R.id.tab_bookings)
        val tabOffers = view.findViewById<LinearLayout>(R.id.tab_offers)
        val tvTabAll = view.findViewById<TextView>(R.id.tv_tab_all)
        val tvTabBookings = view.findViewById<TextView>(R.id.tv_tab_bookings)
        val tvTabOffers = view.findViewById<TextView>(R.id.tv_tab_offers)

        fun selectTab(selected: String) {
            val tabs = listOf(
                Triple(tabAll, tvTabAll, "ALL"),
                Triple(tabBookings, tvTabBookings, "BOOKING"),
                Triple(tabOffers, tvTabOffers, "OFFER")
            )
            for ((layout, tv, key) in tabs) {
                val isSel = selected == key
                layout?.setBackgroundResource(if (isSel) R.drawable.bg_chip_selected else R.drawable.bg_chip_default)
                if (isSel) {
                    tv?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.surface_white))
                } else {
                    tv?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
            }
        }

        tabAll?.setOnClickListener { selectTab("ALL") }
        tvTabAll?.setOnClickListener { selectTab("ALL") }
        tabBookings?.setOnClickListener { selectTab("BOOKING") }
        tvTabBookings?.setOnClickListener { selectTab("BOOKING") }
        tabOffers?.setOnClickListener { selectTab("OFFER") }
        tvTabOffers?.setOnClickListener { selectTab("OFFER") }

        // Notification items click
        view.findViewById<LinearLayout>(R.id.notif_item_1)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment.newInstance("bk_demo_1"), addToBackStack = true)
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

    private fun loadNotifications(uid: String) {
        NotificationRepository.getNotificationsForUser(uid,
            onSuccess = { list ->
                if (!isAdded) return@getNotificationsForUser
                userNotifications = list
                // Dynamic items can update or link to relevant booking
                val n1 = list.firstOrNull { it.relatedBookingId.isNotEmpty() }
                if (n1 != null) {
                    view?.findViewById<LinearLayout>(R.id.notif_item_1)?.setOnClickListener {
                        val frag = BookingDetailsFragment.newInstance(n1.relatedBookingId)
                        (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
                    }
                }
            },
            onFailure = {}
        )
    }
}
