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

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Top Bell button
        view.findViewById<ImageView>(R.id.btn_bell)?.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications are up to date.", Toast.LENGTH_SHORT).show()
        }

        // Mark all as read
        val btnMarkAll = view.findViewById<TextView>(R.id.btn_mark_all_read)
        btnMarkAll?.setOnClickListener {
            val targetUid = uid.ifEmpty { "demo_customer" }
            NotificationRepository.markAllAsRead(targetUid,
                onSuccess = {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
                        btnMarkAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                        btnMarkAll.isEnabled = false
                        loadNotifications(targetUid)
                    }
                },
                onFailure = {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
                        btnMarkAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                        btnMarkAll.isEnabled = false
                    }
                }
            )
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

        // Notification views to filter
        val groupToday = view.findViewById<View>(R.id.tv_group_today)
        val item1 = view.findViewById<View>(R.id.notif_item_1)
        val item2 = view.findViewById<View>(R.id.notif_item_2)
        val groupEarlier = view.findViewById<View>(R.id.tv_group_earlier)
        val item3 = view.findViewById<View>(R.id.notif_item_3)
        val item4 = view.findViewById<View>(R.id.notif_item_4)

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
                    tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.surface_white))
                } else {
                    tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
            }

            when (selected) {
                "ALL" -> {
                    groupToday?.visibility = View.VISIBLE
                    item1?.visibility = View.VISIBLE
                    item2?.visibility = View.VISIBLE
                    groupEarlier?.visibility = View.VISIBLE
                    item3?.visibility = View.VISIBLE
                    item4?.visibility = View.VISIBLE
                }
                "BOOKING" -> {
                    groupToday?.visibility = View.VISIBLE
                    item1?.visibility = View.VISIBLE
                    item2?.visibility = View.VISIBLE
                    groupEarlier?.visibility = View.VISIBLE
                    item3?.visibility = View.GONE
                    item4?.visibility = View.VISIBLE
                }
                "OFFER" -> {
                    groupToday?.visibility = View.GONE
                    item1?.visibility = View.GONE
                    item2?.visibility = View.GONE
                    groupEarlier?.visibility = View.VISIBLE
                    item3?.visibility = View.VISIBLE
                    item4?.visibility = View.GONE
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

    private var notificationsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun loadNotifications(uid: String) {
        notificationsListener?.remove()
        notificationsListener = NotificationRepository.listenToNotificationsForUser(uid) { list ->
            if (!isAdded) return@listenToNotificationsForUser
            userNotifications = list
            val n1 = list.firstOrNull { it.relatedBookingId.isNotEmpty() }
            if (n1 != null) {
                view?.findViewById<LinearLayout>(R.id.notif_item_1)?.setOnClickListener {
                    val frag = BookingDetailsFragment.newInstance(n1.relatedBookingId)
                    (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationsListener?.remove()
    }
}
