package com.example.bizbridgeapp
 
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.BusinessRepository
import com.google.firebase.firestore.ListenerRegistration
 
class OwnerDashboardFragment : Fragment() {
 
    private var businessListener: ListenerRegistration? = null
    private var bookingsListener: ListenerRegistration? = null
    private var currentBusinessId: String = ""
    private var mostRecentPendingBooking: Booking? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Bottom Navigation
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.HOME)

        val mainActivity = activity as? MainActivity
        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Pre-populate business name & category immediately from session
        val profile = UserSessionManager.getProfile()
        val sessionBizName = (profile?.get("businessName") as? String)?.ifBlank { null }
        val sessionCategory = (profile?.get("category") as? String)?.ifBlank { null } ?: "Hair salon"
        if (!sessionBizName.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tv_owner_business_name)?.text = sessionBizName
            view.findViewById<TextView>(R.id.tv_owner_subtitle)?.text = "$sessionBizName · $sessionCategory"
        }

        // Initialize zero metric counts and hide recent request card until real data loads
        view.findViewById<TextView>(R.id.tv_count_pending)?.text = "0"
        view.findViewById<TextView>(R.id.tv_count_today)?.text = "0"
        view.findViewById<TextView>(R.id.tv_count_confirmed_month)?.text = "0"
        view.findViewById<TextView>(R.id.tv_count_completed_total)?.text = "0"
        view.findViewById<View>(R.id.card_recent_request_1)?.visibility = View.GONE
 
        // Notifications
        view.findViewById<View>(R.id.iv_owner_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }
 
        // Manage all requests link -> Manage Bookings
        view.findViewById<TextView>(R.id.tv_manage_all_requests)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = true)
        }
 
        // Metric cards -> Manage Bookings
        view.findViewById<View>(R.id.card_metric_today)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.card_metric_pending)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = true)
        }
 
        // Rating Card -> B13 Customer Reviews
        view.findViewById<View>(R.id.card_owner_reviews)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerCustomerReviewsFragment(), addToBackStack = true)
        }
 
        // Trust Score Card -> B11 Trust Score Details
        view.findViewById<View>(R.id.card_owner_trust_score)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerTrustScoreDetailsFragment(), addToBackStack = true)
        }
 
        // Request Card Click -> Booking Details
        view.findViewById<View>(R.id.card_recent_request_1)?.setOnClickListener {
            val booking = mostRecentPendingBooking
            if (booking != null) {
                mainActivity?.navigateTo(
                    OwnerBookingDetailsFragment.newInstance(
                        bookingId = booking.id,
                        customerName = "Customer (${booking.customerId.take(6)})",
                        serviceName = "${booking.serviceNameSnapshot} · $${booking.priceAtBooking.toInt()} · ${booking.durationMinutes} min",
                        dateTime = "${booking.appointmentDate} · ${booking.appointmentTime}",
                        customerNote = booking.customerNotes.ifEmpty { "No special notes provided." }
                    ),
                    addToBackStack = true
                )
            } else {
                mainActivity?.navigateTo(OwnerBookingDetailsFragment(), addToBackStack = true)
            }
        }
 
        // Real-time listener for Owner Business document
        businessListener = BusinessRepository.listenToOwnerBusiness(currentBusinessId) { biz ->
            if (!isAdded || biz == null) return@listenToOwnerBusiness
            currentBusinessId = biz.id
 
            // Update Business Name & Subtitle
            view.findViewById<TextView>(R.id.tv_owner_business_name)?.text = biz.name
            view.findViewById<TextView>(R.id.tv_owner_subtitle)?.text = "${biz.name} · ${biz.category}"
 
            // Approval & Verification badge
            val badge = view.findViewById<TextView>(R.id.badge_approval_status)
            if (biz.approved && biz.active) {
                badge?.text = "Approved · Listing live · Verified business"
                badge?.setBackgroundResource(R.drawable.bg_tag_verified)
                badge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
            } else if (biz.approvalStatus == "PENDING") {
                badge?.text = "Application Pending Admin Review"
                badge?.setBackgroundResource(R.drawable.bg_badge_pending)
                badge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_pending_text))
            } else if (biz.approvalStatus == "REJECTED") {
                badge?.text = "Application Rejected · Tap to Review"
                badge?.setBackgroundResource(R.drawable.bg_badge_cancelled)
                badge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_text))
            }
 
            // Rating and Review Count
            val ratingText = if (biz.rating != null && biz.rating > 0 && biz.reviewCount > 0) {
                "★ ${String.format("%.1f", biz.rating)} · ${biz.reviewCount} customer reviews"
            } else if (biz.reviewCount > 0) {
                "★ 5.0 · ${biz.reviewCount} customer reviews"
            } else {
                "★ 0.0 · No customer reviews yet"
            }
            view.findViewById<TextView>(R.id.tv_rating_stars)?.text = ratingText
 
            // Trust Score
            view.findViewById<TextView>(R.id.tv_trust_score_value)?.text = "${biz.trustScore} / 100"
        }
 
        // Real-time listener for Bookings
        bookingsListener = BookingRepository.listenToBookingsForBusiness(currentBusinessId) { bookings ->
            if (!isAdded) return@listenToBookingsForBusiness
 
            val pendingList = bookings.filter { it.status == "PENDING" }
            val confirmedList = bookings.filter { it.status == "CONFIRMED" }
            val completedList = bookings.filter { it.status == "COMPLETED" }
 
            // Metric counts
            view.findViewById<TextView>(R.id.tv_count_pending)?.text = pendingList.size.toString()
            view.findViewById<TextView>(R.id.tv_count_today)?.text = (pendingList.size + confirmedList.size).toString()
            view.findViewById<TextView>(R.id.tv_count_confirmed_month)?.text = confirmedList.size.toString()
            view.findViewById<TextView>(R.id.tv_count_completed_total)?.text = completedList.size.toString()
 
            // Update Recent Request Card with the first pending booking
            val targetBooking = pendingList.firstOrNull() ?: bookings.firstOrNull { it.status != "CANCELLED" }
            mostRecentPendingBooking = targetBooking
 
            val requestCard = view.findViewById<View>(R.id.card_recent_request_1)
            val btnConfirm = view.findViewById<Button>(R.id.btn_confirm_request)
            val btnReject = view.findViewById<Button>(R.id.btn_reject_request)
 
            if (targetBooking != null) {
                requestCard?.visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.tv_recent_booking_ref)?.text = targetBooking.qrReference.ifEmpty { targetBooking.id }
                val statusView = view.findViewById<TextView>(R.id.tv_recent_booking_status)
                statusView?.text = targetBooking.status
                if (targetBooking.status == "CONFIRMED") {
                    statusView?.setBackgroundResource(R.drawable.bg_badge_confirmed)
                    statusView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_confirmed_text))
                    btnConfirm?.visibility = View.GONE
                    btnReject?.visibility = View.GONE
                } else if (targetBooking.status == "PENDING") {
                    statusView?.setBackgroundResource(R.drawable.bg_badge_pending)
                    statusView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_pending_text))
                    btnConfirm?.visibility = View.VISIBLE
                    btnReject?.visibility = View.VISIBLE
                    btnConfirm?.isEnabled = true
                    btnConfirm?.text = "Confirm request"
                }
 
                view.findViewById<TextView>(R.id.tv_recent_customer_name)?.text =
                    if (targetBooking.customerId.isNotBlank()) "Customer (${targetBooking.customerId.take(6)})" else "Liam Brooks"
                view.findViewById<TextView>(R.id.tv_recent_service_details)?.text =
                    "${targetBooking.serviceNameSnapshot} · $${targetBooking.priceAtBooking.toInt()} · ${targetBooking.durationMinutes} min"
                view.findViewById<TextView>(R.id.tv_recent_date_time)?.text =
                    "${targetBooking.appointmentDate} · ${targetBooking.appointmentTime}"
 
                // Confirm action
                btnConfirm?.setOnClickListener {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Confirming…"
                    BookingRepository.updateBookingStatus(
                        bookingId = targetBooking.id,
                        newStatus = "CONFIRMED",
                        onSuccess = {
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Appointment ${targetBooking.qrReference} confirmed.", Toast.LENGTH_SHORT).show()
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
 
                // Reject action with confirmation dialog
                btnReject?.setOnClickListener {
                    OwnerDialogHelper.showConfirmationDialog(
                        context = requireContext(),
                        title = "Reject request ${targetBooking.qrReference}?",
                        message = "Reject this appointment request? The customer will be notified.",
                        confirmButtonText = "Confirm Rejection",
                        cancelButtonText = "Keep Reviewing",
                        isDestructive = true,
                        onConfirm = {
                            BookingRepository.updateBookingStatus(
                                bookingId = targetBooking.id,
                                newStatus = "REJECTED",
                                reason = "Unavailable at requested time",
                                onSuccess = {
                                    if (isAdded) {
                                        Toast.makeText(requireContext(), "Request rejected.", Toast.LENGTH_SHORT).show()
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
                requestCard?.visibility = View.GONE
            }
        }
    }
 
    override fun onDestroyView() {
        super.onDestroyView()
        businessListener?.remove()
        bookingsListener?.remove()
    }
}
