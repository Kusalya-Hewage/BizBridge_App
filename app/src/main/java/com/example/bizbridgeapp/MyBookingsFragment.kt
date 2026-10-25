package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth

class MyBookingsFragment : Fragment() {

    private var allBookings: List<Booking> = emptyList()
    private var selectedTab: String = "ALL"

    private var llBookingsContainer: LinearLayout? = null
    private var pbLoading: ProgressBar? = null
    private var llEmptyState: LinearLayout? = null
    private var tvEmptyMessage: TextView? = null
    private var tvBookingsCount: TextView? = null
    private var btnExploreNow: Button? = null

    private var tabAll: LinearLayout? = null
    private var tabUpcoming: LinearLayout? = null
    private var tabPending: LinearLayout? = null
    private var tabCancelled: LinearLayout? = null

    private var tvTabAll: TextView? = null
    private var tvTabUpcoming: TextView? = null
    private var tvTabPending: TextView? = null
    private var tvTabCancelled: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llBookingsContainer = view.findViewById(R.id.ll_bookings_container)
        pbLoading = view.findViewById(R.id.pb_loading)
        llEmptyState = view.findViewById(R.id.ll_empty_state)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)
        tvBookingsCount = view.findViewById(R.id.tv_bookings_count)
        btnExploreNow = view.findViewById(R.id.btn_explore_now)

        tabAll = view.findViewById(R.id.tab_all)
        tabUpcoming = view.findViewById(R.id.tab_upcoming)
        tabPending = view.findViewById(R.id.tab_pending)
        tabCancelled = view.findViewById(R.id.tab_cancelled)

        tvTabAll = view.findViewById(R.id.tv_tab_all)
        tvTabUpcoming = view.findViewById(R.id.tv_tab_upcoming)
        tvTabPending = view.findViewById(R.id.tv_tab_pending)
        tvTabCancelled = view.findViewById(R.id.tv_tab_cancelled)

        // Notification Bell
        view.findViewById<ImageView>(R.id.btn_bell)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }

        // Empty state explore button
        btnExploreNow?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = true)
        }

        // Tab click listeners - attach to both layout and text view so touches are never missed
        val clickAll = View.OnClickListener { selectTab("ALL") }
        tabAll?.setOnClickListener(clickAll)
        tvTabAll?.setOnClickListener(clickAll)

        val clickUpcoming = View.OnClickListener { selectTab("UPCOMING") }
        tabUpcoming?.setOnClickListener(clickUpcoming)
        tvTabUpcoming?.setOnClickListener(clickUpcoming)

        val clickPending = View.OnClickListener { selectTab("PENDING") }
        tabPending?.setOnClickListener(clickPending)
        tvTabPending?.setOnClickListener(clickPending)

        val clickCancelled = View.OnClickListener { selectTab("CANCELLED") }
        tabCancelled?.setOnClickListener(clickCancelled)
        tvTabCancelled?.setOnClickListener(clickCancelled)

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

        loadCustomerBookings()
    }

    private fun selectTab(tab: String) {
        selectedTab = tab
        updateTabStyles()
        filterAndRenderBookings()
    }

    private fun updateTabStyles() {
        val tabs = listOf(
            Triple(tabAll, tvTabAll, "ALL"),
            Triple(tabUpcoming, tvTabUpcoming, "UPCOMING"),
            Triple(tabPending, tvTabPending, "PENDING"),
            Triple(tabCancelled, tvTabCancelled, "CANCELLED")
        )

        for ((layout, tv, key) in tabs) {
            val isSelected = selectedTab == key
            layout?.setBackgroundResource(
                if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_default
            )
            if (isSelected) {
                tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.surface_white))
            } else {
                tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }

    private var bookingsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun loadCustomerBookings() {
        val user = FirebaseAuth.getInstance().currentUser
        val profile = UserSessionManager.getProfile()
        val uid = user?.uid ?: (profile?.get("uid") as? String).orEmpty()
        if (uid.isEmpty()) {
            pbLoading?.visibility = View.GONE
            allBookings = emptyList()
            updateTabStyles()
            filterAndRenderBookings()
            return
        }

        pbLoading?.visibility = View.VISIBLE
        llEmptyState?.visibility = View.GONE
        tvBookingsCount?.text = "Loading..."

        bookingsListener?.remove()
        bookingsListener = BookingRepository.listenToBookingsForCustomer(uid) { bookings ->
            if (!isAdded) return@listenToBookingsForCustomer
            pbLoading?.visibility = View.GONE
            allBookings = bookings
            updateTabStyles()
            filterAndRenderBookings()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bookingsListener?.remove()
    }

    private fun filterAndRenderBookings() {
        val filtered = when (selectedTab) {
            "ALL" -> allBookings
            "UPCOMING" -> allBookings.filter { 
                it.status.equals("CONFIRMED", ignoreCase = true) || 
                it.status.equals("PENDING", ignoreCase = true) 
            }
            "PENDING" -> allBookings.filter { it.status.equals("PENDING", ignoreCase = true) }
            "CANCELLED" -> allBookings.filter { it.status.equals("CANCELLED", ignoreCase = true) }
            else -> allBookings
        }

        val container = llBookingsContainer ?: return
        container.removeAllViews()

        val countText = if (filtered.size == 1) "1 appointment" else "${filtered.size} appointments"
        tvBookingsCount?.text = countText

        if (filtered.isEmpty()) {
            llEmptyState?.visibility = View.VISIBLE
            val label = when (selectedTab) {
                "CONFIRMED" -> "upcoming"
                "PENDING" -> "pending"
                "CANCELLED" -> "cancelled"
                else -> ""
            }
            tvEmptyMessage?.text = if (label.isNotEmpty()) {
                "You have no $label appointments."
            } else {
                "You haven't placed any bookings yet."
            }
            return
        }

        llEmptyState?.visibility = View.GONE

        val inflater = LayoutInflater.from(requireContext())
        for (booking in filtered) {
            val cardView = inflater.inflate(R.layout.item_booking_card, container, false)

            val tvRef = cardView.findViewById<TextView>(R.id.tv_booking_ref)
            val tvStatus = cardView.findViewById<TextView>(R.id.tv_booking_status)
            val llStatusBadge = cardView.findViewById<LinearLayout>(R.id.ll_status_badge)
            val tvBizName = cardView.findViewById<TextView>(R.id.tv_business_name)
            val tvServiceInfo = cardView.findViewById<TextView>(R.id.tv_service_info)
            val tvDateTime = cardView.findViewById<TextView>(R.id.tv_appointment_date_time)
            val btnDetails = cardView.findViewById<TextView>(R.id.btn_view_details)
            val btnAction = cardView.findViewById<TextView>(R.id.btn_action)

            tvRef?.text = booking.qrReference.ifEmpty { booking.id }
            tvBizName?.text = booking.businessNameSnapshot
            tvServiceInfo?.text = "${booking.serviceNameSnapshot} • $${booking.priceAtBooking.toInt()} • ${booking.durationMinutes} min"
            tvDateTime?.text = "${booking.appointmentDate} • ${booking.appointmentTime}"

            // Status Badge Formatting
            when (booking.status.uppercase()) {
                "CONFIRMED" -> {
                    tvStatus?.text = "Confirmed"
                    tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_confirmed_text))
                    llStatusBadge?.setBackgroundResource(R.drawable.bg_tag_confirmed)
                }
                "PENDING" -> {
                    tvStatus?.text = "Pending"
                    tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_pending_text))
                    llStatusBadge?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.badge_pending_bg))
                }
                "COMPLETED" -> {
                    tvStatus?.text = "Completed"
                    tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_completed_text))
                    llStatusBadge?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.badge_completed_bg))
                }
                "CANCELLED" -> {
                    tvStatus?.text = "Cancelled"
                    tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_text))
                    llStatusBadge?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_bg))
                }
                else -> {
                    tvStatus?.text = booking.status
                    tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    llStatusBadge?.setBackgroundResource(R.drawable.bg_chip_default)
                }
            }

            // Action Button Setup
            when (booking.status.uppercase()) {
                "COMPLETED" -> {
                    btnAction?.visibility = View.VISIBLE
                    btnAction?.text = "Write a review"
                    btnAction?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    btnAction?.setOnClickListener {
                        val frag = WriteReviewFragment.newInstance(
                            bookingId = booking.id,
                            businessId = booking.businessId,
                            businessName = booking.businessNameSnapshot
                        )
                        (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
                    }
                }
                "PENDING", "CONFIRMED" -> {
                    btnAction?.visibility = View.VISIBLE
                    btnAction?.text = "Cancel booking"
                    btnAction?.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_text))
                    btnAction?.setOnClickListener {
                        promptCancellation(booking)
                    }
                }
                else -> {
                    btnAction?.visibility = View.GONE
                }
            }

            // View Details click
            val openDetails = View.OnClickListener {
                val frag = BookingDetailsFragment.newInstance(booking.id)
                (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
            }
            btnDetails?.setOnClickListener(openDetails)
            cardView.setOnClickListener(openDetails)

            container.addView(cardView)
        }
    }

    private fun promptCancellation(booking: Booking) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel your appointment with ${booking.businessNameSnapshot} on ${booking.appointmentDate}?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                BookingRepository.cancelBooking(booking.id, "Customer requested cancellation",
                    onSuccess = {
                        if (!isAdded) return@cancelBooking
                        Toast.makeText(requireContext(), "Booking cancelled", Toast.LENGTH_SHORT).show()
                        loadCustomerBookings()
                    },
                    onFailure = { e ->
                        if (!isAdded) return@cancelBooking
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to cancel", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Keep Booking", null)
            .show()
    }
}
