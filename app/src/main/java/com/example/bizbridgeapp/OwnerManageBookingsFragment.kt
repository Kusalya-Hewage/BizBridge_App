package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.BusinessRepository
import com.google.firebase.firestore.ListenerRegistration

class OwnerManageBookingsFragment : Fragment() {

    private var bookingsListener: ListenerRegistration? = null
    private var allBookings: List<Booking> = emptyList()
    private var activeFilter: String = "PENDING"
    private var currentBusinessId: String = "biz_juniper_studio"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_manage_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bottom Navigation
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.BOOKINGS)

        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Tabs
        val tabPending = view.findViewById<TextView>(R.id.tab_pending)
        val tabConfirmed = view.findViewById<TextView>(R.id.tab_confirmed)
        val tabCompleted = view.findViewById<TextView>(R.id.tab_completed)
        val tabCancelled = view.findViewById<TextView>(R.id.tab_cancelled)

        val selectTab = { active: TextView, others: List<TextView>, filter: String ->
            activeFilter = filter
            active.setBackgroundResource(R.drawable.bg_chip_selected)
            active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            others.forEach {
                it.setBackgroundResource(R.drawable.bg_chip_default)
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            renderBookings(view)
        }

        tabPending?.setOnClickListener { selectTab(tabPending, listOfNotNull(tabConfirmed, tabCompleted, tabCancelled), "PENDING") }
        tabConfirmed?.setOnClickListener { selectTab(tabConfirmed, listOfNotNull(tabPending, tabCompleted, tabCancelled), "CONFIRMED") }
        tabCompleted?.setOnClickListener { selectTab(tabCompleted, listOfNotNull(tabPending, tabConfirmed, tabCancelled), "COMPLETED") }
        tabCancelled?.setOnClickListener { selectTab(tabCancelled, listOfNotNull(tabPending, tabConfirmed, tabCompleted), "CANCELLED") }

        // Real-time listener for business bookings
        bookingsListener = BookingRepository.listenToBookingsForBusiness(currentBusinessId) { list ->
            if (!isAdded) return@listenToBookingsForBusiness
            allBookings = list

            // Update Tab Count Badges
            val pCount = list.count { it.status == "PENDING" }
            val cCount = list.count { it.status == "CONFIRMED" }
            val compCount = list.count { it.status == "COMPLETED" }
            val canCount = list.count { it.status == "CANCELLED" }

            tabPending?.text = "Pending · $pCount"
            tabConfirmed?.text = "Confirmed · $cCount"
            tabCompleted?.text = "Completed · $compCount"
            tabCancelled?.text = "Cancelled · $canCount"

            renderBookings(view)
        }
    }

    private fun renderBookings(rootView: View) {
        val filtered = allBookings.filter { it.status.equals(activeFilter, ignoreCase = true) }

        val cards = listOf(
            rootView.findViewById<LinearLayout>(R.id.card_booking_liam),
            rootView.findViewById<LinearLayout>(R.id.card_booking_maya),
            rootView.findViewById<LinearLayout>(R.id.card_booking_ava)
        )

        val confirmButtons = listOf(
            rootView.findViewById<Button>(R.id.btn_confirm_liam),
            rootView.findViewById<Button>(R.id.btn_confirm_maya),
            rootView.findViewById<Button>(R.id.btn_confirm_ava)
        )

        val rejectButtons = listOf(
            rootView.findViewById<Button>(R.id.btn_reject_liam),
            rootView.findViewById<Button>(R.id.btn_reject_maya),
            rootView.findViewById<Button>(R.id.btn_reject_ava)
        )

        for (i in 0 until 3) {
            val card = cards[i] ?: continue
            val btnConfirm = confirmButtons[i]
            val btnReject = rejectButtons[i]

            if (i < filtered.size) {
                val booking = filtered[i]
                card.visibility = View.VISIBLE

                // Find header relative layout
                val topRelLayout = card.getChildAt(0) as? RelativeLayout
                val tvRef = topRelLayout?.getChildAt(0) as? TextView
                val tvStatus = topRelLayout?.getChildAt(1) as? TextView

                val tvCustomer = card.getChildAt(1) as? TextView
                val tvService = card.getChildAt(2) as? TextView
                val tvDateTime = card.getChildAt(3) as? TextView

                tvRef?.text = booking.qrReference.ifEmpty { booking.id }
                tvStatus?.text = booking.status

                when (booking.status) {
                    "PENDING" -> {
                        tvStatus?.setBackgroundResource(R.drawable.bg_badge_pending)
                        tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_pending_text))
                        btnConfirm?.visibility = View.VISIBLE
                        btnConfirm?.isEnabled = true
                        btnConfirm?.text = "Confirm request"
                        btnReject?.visibility = View.VISIBLE
                    }
                    "CONFIRMED" -> {
                        tvStatus?.setBackgroundResource(R.drawable.bg_badge_confirmed)
                        tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_confirmed_text))
                        btnConfirm?.visibility = View.GONE
                        btnReject?.visibility = View.GONE
                    }
                    "COMPLETED" -> {
                        tvStatus?.setBackgroundResource(R.drawable.bg_badge_completed)
                        tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_completed_text))
                        btnConfirm?.visibility = View.GONE
                        btnReject?.visibility = View.GONE
                    }
                    else -> {
                        tvStatus?.setBackgroundResource(R.drawable.bg_badge_cancelled)
                        tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_text))
                        btnConfirm?.visibility = View.GONE
                        btnReject?.visibility = View.GONE
                    }
                }

                val displayName = if (booking.customerId.isNotBlank()) "Customer (${booking.customerId.take(6)})" else "Appointment #${booking.qrReference}"
                tvCustomer?.text = displayName
                tvService?.text = "${booking.serviceNameSnapshot} · $${booking.priceAtBooking.toInt()} · ${booking.durationMinutes} min"
                tvDateTime?.text = "${booking.appointmentDate} · ${booking.appointmentTime}"

                // Card Click -> Booking Details
                card.setOnClickListener {
                    (activity as? MainActivity)?.navigateTo(
                        OwnerBookingDetailsFragment.newInstance(
                            bookingId = booking.id,
                            customerName = displayName,
                            customerEmail = "customer@bizbridge.example",
                            customerPhone = "+1 (555) 019-2834",
                            serviceName = "${booking.serviceNameSnapshot} · $${booking.priceAtBooking.toInt()} · ${booking.durationMinutes} min",
                            dateTime = "${booking.appointmentDate} · ${booking.appointmentTime}",
                            customerNote = booking.customerNotes.ifEmpty { "Customer notes: None provided." }
                        ),
                        addToBackStack = true
                    )
                }

                // Confirm Action
                btnConfirm?.setOnClickListener {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Confirming…"
                    BookingRepository.updateBookingStatus(
                        bookingId = booking.id,
                        newStatus = "CONFIRMED",
                        onSuccess = {
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Booking ${booking.qrReference} confirmed.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailure = { e ->
                            if (isAdded) {
                                btnConfirm.isEnabled = true
                                btnConfirm.text = "Confirm request"
                                Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to confirm.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Reject Action with confirmation dialog
                btnReject?.setOnClickListener {
                    OwnerDialogHelper.showConfirmationDialog(
                        context = requireContext(),
                        title = "Reject booking ${booking.qrReference}?",
                        message = "Reject this appointment? The customer will receive an immediate update.",
                        confirmButtonText = "Confirm Rejection",
                        cancelButtonText = "Keep Reviewing",
                        isDestructive = true,
                        onConfirm = {
                            BookingRepository.updateBookingStatus(
                                bookingId = booking.id,
                                newStatus = "REJECTED",
                                reason = "Schedule conflict",
                                onSuccess = {
                                    if (isAdded) {
                                        Toast.makeText(requireContext(), "Booking ${booking.qrReference} rejected.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onFailure = { e ->
                                    if (isAdded) {
                                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to reject.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    )
                }
            } else {
                card.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bookingsListener?.remove()
    }
}
