package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.Service
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.ServiceRepository
import com.example.bizbridgeapp.repository.TrustScoreRepository
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OwnerDashboardFragment : Fragment() {

    private var currentBusiness: Business? = null
    private var businessListener: ListenerRegistration? = null
    private var bookingsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Logout
        view.findViewById<ImageView>(R.id.btn_owner_logout)?.setOnClickListener {
            FirebaseAuthManager.signOut()
            UserSessionManager.clear()
            (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = false)
        }

        // QR Check-in
        view.findViewById<Button>(R.id.btn_qr_checkin)?.setOnClickListener {
            showQrVerificationDialog()
        }

        // Add Service
        view.findViewById<Button>(R.id.btn_add_service)?.setOnClickListener {
            showAddServiceDialog()
        }

        // Load authenticated business
        loadOwnerBusiness()
    }

    private fun loadOwnerBusiness() {
        val uid = FirebaseAuthManager.getCurrentUser()?.uid ?: "demo_owner"

        BusinessRepository.getBusinessByOwnerId(
            ownerId = uid,
            onSuccess = { biz ->
                val targetBiz = biz ?: BusinessRepository.defaultDemoBusinesses.first()
                currentBusiness = targetBiz
                bindBusinessInfo(targetBiz)
                listenToBusinessUpdates(targetBiz.id)
                listenToBookings(targetBiz.id)
                loadServices(targetBiz.id)
            },
            onFailure = {
                val fallback = BusinessRepository.defaultDemoBusinesses.first()
                currentBusiness = fallback
                bindBusinessInfo(fallback)
                listenToBookings(fallback.id)
                loadServices(fallback.id)
            }
        )
    }

    private fun bindBusinessInfo(biz: Business) {
        val v = view ?: return
        v.findViewById<TextView>(R.id.tv_owner_business_name)?.text = biz.name
        v.findViewById<TextView>(R.id.tv_owner_category_info)?.text = "${biz.category} • ${biz.address}"
        v.findViewById<TextView>(R.id.tv_trust_score)?.text = "Trust Score: ${biz.trustScore}"
        v.findViewById<TextView>(R.id.tv_rating_summary)?.text = "${biz.rating ?: 5.0} (${biz.reviewCount} reviews)"

        val badgeApproval = v.findViewById<TextView>(R.id.badge_approval_status)
        val bannerStatus = v.findViewById<View>(R.id.banner_application_status)
        val tvNotice = v.findViewById<TextView>(R.id.tv_status_notice)

        if (biz.approved || biz.approvalStatus == "APPROVED") {
            badgeApproval?.text = "APPROVED & ACTIVE"
            badgeApproval?.setBackgroundResource(R.drawable.bg_tag_verified)
            badgeApproval?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.secondary_dark))
            bannerStatus?.visibility = View.GONE
        } else if (biz.approvalStatus == "REJECTED") {
            badgeApproval?.text = "APPLICATION REJECTED"
            badgeApproval?.setBackgroundResource(R.drawable.bg_input_field_error)
            badgeApproval?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.error_text))
            bannerStatus?.visibility = View.VISIBLE
            tvNotice?.text = "Your business listing application was rejected. Please contact support or update your application."
        } else {
            badgeApproval?.text = "PENDING APPROVAL"
            badgeApproval?.setBackgroundResource(R.drawable.bg_tag_confirmed)
            badgeApproval?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary))
            bannerStatus?.visibility = View.VISIBLE
            tvNotice?.text = "Your business is currently under review by administrators. Approved listings will be discoverable publicly to customers."
        }
    }

    private fun listenToBusinessUpdates(businessId: String) {
        businessListener?.remove()
        businessListener = BusinessRepository.listenToBusiness(businessId) { updatedBiz ->
            if (updatedBiz != null && isAdded) {
                currentBusiness = updatedBiz
                bindBusinessInfo(updatedBiz)
            }
        }
    }

    private fun listenToBookings(businessId: String) {
        bookingsListener?.remove()
        bookingsListener = BookingRepository.listenToBookingsForBusiness(businessId) { bookings ->
            if (!isAdded) return@listenToBookingsForBusiness
            renderBookings(bookings)
        }
    }

    private fun renderBookings(bookings: List<Booking>) {
        val v = view ?: return
        val container = v.findViewById<LinearLayout>(R.id.container_owner_bookings) ?: return
        container.removeAllViews()

        val tvCountToday = v.findViewById<TextView>(R.id.tv_count_today)
        val tvCountPending = v.findViewById<TextView>(R.id.tv_count_pending)
        val tvCountConfirmed = v.findViewById<TextView>(R.id.tv_count_confirmed)

        val todayDateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())

        val pendingList = bookings.filter { it.status == "PENDING" }
        val confirmedList = bookings.filter { it.status == "CONFIRMED" }
        val todayCount = bookings.count { it.appointmentDate.contains(todayDateStr, ignoreCase = true) }

        tvCountToday?.text = todayCount.toString()
        tvCountPending?.text = pendingList.size.toString()
        tvCountConfirmed?.text = confirmedList.size.toString()

        if (bookings.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "No appointment bookings yet. Customer requests will appear here in real-time."
                setPadding(32, 48, 32, 48)
                gravity = android.view.Gravity.CENTER
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary))
                setBackgroundResource(R.drawable.bg_card_white)
            }
            container.addView(emptyTv)
            return
        }

        val sorted = bookings.sortedWith(compareByDescending<Booking> { it.status == "PENDING" }.thenByDescending { it.appointmentDate })

        for (bk in sorted) {
            val card = LayoutInflater.from(requireContext()).inflate(R.layout.item_booking_card, container, false)
            card.findViewById<TextView>(R.id.tv_booking_ref)?.text = "REF: ${bk.qrReference.take(8).uppercase()}"
            card.findViewById<TextView>(R.id.tv_business_name)?.text = bk.serviceNameSnapshot
            val notes = if (bk.customerNotes.isNotEmpty()) "Notes: ${bk.customerNotes} • " else ""
            card.findViewById<TextView>(R.id.tv_service_info)?.text = "${notes}\$${String.format(Locale.US, "%.2f", bk.priceAtBooking)}"
            card.findViewById<TextView>(R.id.tv_appointment_date_time)?.text = "${bk.appointmentDate} • ${bk.appointmentTime}"
            card.findViewById<TextView>(R.id.tv_booking_status)?.text = bk.status

            val btnViewDetails = card.findViewById<TextView>(R.id.btn_view_details)
            val btnAction = card.findViewById<TextView>(R.id.btn_action)

            when (bk.status) {
                "PENDING" -> {
                    btnViewDetails?.visibility = View.VISIBLE
                    btnViewDetails?.text = "Confirm Booking"
                    btnViewDetails?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary))
                    btnViewDetails?.setOnClickListener {
                        BookingRepository.updateBookingStatus(bk.id, "CONFIRMED", onSuccess = {}, onFailure = {})
                    }
                    btnAction?.visibility = View.VISIBLE
                    btnAction?.text = "Reject"
                    btnAction?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.error_text))
                    btnAction?.setOnClickListener {
                        showRejectDialog(bk.id)
                    }
                }
                "CONFIRMED" -> {
                    btnViewDetails?.visibility = View.VISIBLE
                    btnViewDetails?.text = "Mark Completed"
                    btnViewDetails?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.secondary_dark))
                    btnViewDetails?.setOnClickListener {
                        BookingRepository.updateBookingStatus(bk.id, "COMPLETED", onSuccess = {}, onFailure = {})
                    }
                    btnAction?.visibility = View.GONE
                }
                else -> {
                    btnViewDetails?.visibility = View.GONE
                    btnAction?.visibility = View.GONE
                }
            }

            container.addView(card)
        }
    }

    private fun showRejectDialog(bookingId: String) {
        val input = EditText(requireContext()).apply {
            hint = "Enter reason for rejection"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Appointment")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim().ifEmpty { "Unavailable at this time" }
                BookingRepository.updateBookingStatus(bookingId, "REJECTED", reason, onSuccess = {}, onFailure = {})
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQrVerificationDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Enter QR Code (e.g. BB-24108)"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Verify Customer QR Check-in")
            .setMessage("Verify booking reference to validate arrival and mark appointment as completed.")
            .setView(input)
            .setPositiveButton("Verify & Check-in") { _, _ ->
                val code = input.text.toString().trim()
                if (code.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a booking reference code.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val bizId = currentBusiness?.id.orEmpty()
                BookingRepository.verifyBookingQr(
                    businessId = bizId,
                    qrRef = code,
                    onSuccess = { completedBooking ->
                        activity?.runOnUiThread {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Check-in Confirmed! ✓")
                                .setMessage("Appointment for '${completedBooking.serviceNameSnapshot}' on ${completedBooking.appointmentDate} has been verified and marked as COMPLETED.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    },
                    onFailure = { error ->
                        activity?.runOnUiThread {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Check-in Failed")
                                .setMessage(error.localizedMessage ?: "Invalid booking reference.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadServices(businessId: String) {
        ServiceRepository.getServicesForBusiness(
            businessId = businessId,
            onSuccess = { services ->
                if (!isAdded) return@getServicesForBusiness
                val v = view ?: return@getServicesForBusiness
                val container = v.findViewById<LinearLayout>(R.id.container_owner_services) ?: return@getServicesForBusiness
                container.removeAllViews()

                if (services.isEmpty()) {
                    val emptyTv = TextView(requireContext()).apply {
                        text = "No services added yet. Click '+ Add Service' above to add your business services."
                        setPadding(24, 32, 24, 32)
                        gravity = android.view.Gravity.CENTER
                        setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary))
                        setBackgroundResource(R.drawable.bg_card_white)
                    }
                    container.addView(emptyTv)
                    return@getServicesForBusiness
                }

                for (srv in services) {
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setBackgroundResource(R.drawable.bg_card_white)
                        setPadding(32, 24, 32, 24)
                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 16 }
                        this.layoutParams = layoutParams
                    }

                    val infoCol = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val titleTv = TextView(requireContext()).apply {
                        text = srv.name
                        textSize = 15f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary))
                    }

                    val descTv = TextView(requireContext()).apply {
                        text = "${srv.durationMinutes} mins • ${srv.description}"
                        textSize = 12f
                        setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary))
                    }

                    infoCol.addView(titleTv)
                    infoCol.addView(descTv)

                    val priceTv = TextView(requireContext()).apply {
                        text = String.format(Locale.US, "$%.2f", srv.price)
                        textSize = 15f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.primary))
                    }

                    row.addView(infoCol)
                    row.addView(priceTv)
                    container.addView(row)
                }
            },
            onFailure = {}
        )
    }

    private fun showAddServiceDialog() {
        val bizId = currentBusiness?.id ?: return
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etName = EditText(requireContext()).apply { hint = "Service Name (e.g. Deluxe Haircut)" }
        val etPrice = EditText(requireContext()).apply {
            hint = "Price in USD (e.g. 45.0)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etDuration = EditText(requireContext()).apply {
            hint = "Duration in minutes (e.g. 45)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val etDesc = EditText(requireContext()).apply { hint = "Brief Description" }

        layout.addView(etName)
        layout.addView(etPrice)
        layout.addView(etDuration)
        layout.addView(etDesc)

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Service")
            .setView(layout)
            .setPositiveButton("Save Service") { _, _ ->
                val name = etName.text.toString().trim()
                val price = etPrice.text.toString().toDoubleOrNull() ?: 35.0
                val duration = etDuration.text.toString().toIntOrNull() ?: 30
                val desc = etDesc.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Service name is required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newService = Service(
                    businessId = bizId,
                    name = name,
                    description = desc,
                    price = price,
                    durationMinutes = duration,
                    isActive = true
                )

                ServiceRepository.createOrUpdateService(
                    businessId = bizId,
                    service = newService,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Service '$name' added successfully!", Toast.LENGTH_SHORT).show()
                        loadServices(bizId)
                    },
                    onFailure = {
                        Toast.makeText(requireContext(), "Failed to add service: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        businessListener?.remove()
        bookingsListener?.remove()
    }
}
