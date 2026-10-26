package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment

import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth

class ReviewBookingFragment : Fragment() {

    private var businessId: String = ""
    private var businessName: String = ""
    private var serviceId: String = ""
    private var serviceName: String = "Signature haircut"
    private var servicePrice: Double = 45.0
    private var serviceDuration: Int = 45
    private var appointmentDate: String = "Saturday, October 24, 2026"
    private var appointmentTime: String = "10:00–10:45 AM"

    companion object {
        fun newInstance(
            businessId: String,
            businessName: String,
            serviceId: String,
            serviceName: String,
            servicePrice: Double,
            serviceDuration: Int,
            appointmentDate: String,
            appointmentTime: String
        ): ReviewBookingFragment {
            val frag = ReviewBookingFragment()
            val args = Bundle()
            args.putString("businessId", businessId)
            args.putString("businessName", businessName)
            args.putString("serviceId", serviceId)
            args.putString("serviceName", serviceName)
            args.putDouble("servicePrice", servicePrice)
            args.putInt("serviceDuration", serviceDuration)
            args.putString("appointmentDate", appointmentDate)
            args.putString("appointmentTime", appointmentTime)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        businessId = arguments?.getString("businessId").orEmpty()
        val bName = arguments?.getString("businessName")
        if (!bName.isNullOrBlank()) businessName = bName
        val sId = arguments?.getString("serviceId")
        if (!sId.isNullOrBlank()) serviceId = sId
        val sName = arguments?.getString("serviceName")
        if (!sName.isNullOrBlank()) serviceName = sName
        servicePrice = arguments?.getDouble("servicePrice") ?: 45.0
        serviceDuration = arguments?.getInt("serviceDuration") ?: 45
        val aDate = arguments?.getString("appointmentDate")
        if (!aDate.isNullOrBlank()) appointmentDate = aDate
        val aTime = arguments?.getString("appointmentTime")
        if (!aTime.isNullOrBlank()) appointmentTime = aTime

        return inflater.inflate(R.layout.fragment_review_booking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Bind dynamic values
        view.findViewById<TextView>(R.id.tv_business_name)?.text = businessName
        view.findViewById<TextView>(R.id.tv_service_header)?.text = "$serviceName • $${servicePrice.toInt()}"
        view.findViewById<TextView>(R.id.tv_appointment_date)?.text = appointmentDate
        view.findViewById<TextView>(R.id.tv_appointment_time_duration)?.text = "$appointmentTime • $serviceDuration minutes"
        view.findViewById<TextView>(R.id.tv_service_breakdown_name)?.text = serviceName
        view.findViewById<TextView>(R.id.tv_service_breakdown_price)?.text = "$${String.format("%.2f", servicePrice)}"
        view.findViewById<TextView>(R.id.tv_service_total_price)?.text = "$${String.format("%.2f", servicePrice)}"

        val tvAddress1 = view.findViewById<TextView>(R.id.tv_review_address_line1)
        val tvAddress2 = view.findViewById<TextView>(R.id.tv_review_address_line2)
        val tvClientName = view.findViewById<TextView>(R.id.tv_review_client_name)
        val tvClientEmail = view.findViewById<TextView>(R.id.tv_review_client_email)

        // Dynamic Client Details
        val currentUser = FirebaseAuth.getInstance().currentUser
        val profile = UserSessionManager.getProfile()
        val clientName = currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: (profile?.get("fullName") as? String)?.takeIf { it.isNotBlank() }
            ?: (profile?.get("name") as? String)?.takeIf { it.isNotBlank() }
            ?: "Customer"
        val clientEmail = currentUser?.email?.takeIf { it.isNotBlank() }
            ?: (profile?.get("email") as? String)?.takeIf { it.isNotBlank() }
            ?: "customer@example.com"

        tvClientName?.text = clientName
        tvClientEmail?.text = clientEmail

        var fullAddress = ""
        // Dynamic Business Address
        if (businessId.isNotBlank()) {
            com.example.bizbridgeapp.repository.BusinessRepository.getBusinessById(
                businessId = businessId,
                onSuccess = { b ->
                    if (b != null && isAdded) {
                        fullAddress = b.address
                        val addressParts = b.address.split(",", limit = 2)
                        if (addressParts.size > 1) {
                            tvAddress1?.text = addressParts[0].trim()
                            tvAddress2?.text = addressParts[1].trim()
                        } else if (b.address.isNotBlank()) {
                            tvAddress1?.text = b.address
                            tvAddress2?.text = b.category
                        }
                    }
                },
                onFailure = {}
            )
        }

        val etNote = view.findViewById<EditText>(R.id.et_booking_note)
        val btnConfirm = view.findViewById<Button>(R.id.btn_confirm_booking)

        // Confirm Booking button -> calls BookingRepository.createBooking
        btnConfirm?.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val currentUid = user?.uid ?: (profile?.get("uid") as? String).orEmpty()
            if (currentUid.isEmpty()) {
                Toast.makeText(requireContext(), "Please sign in to confirm booking.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnConfirm.isEnabled = false
            val note = etNote?.text?.toString()?.trim().orEmpty()

            val booking = Booking(
                customerId = currentUid,
                businessId = businessId,
                businessNameSnapshot = businessName,
                businessAddressSnapshot = fullAddress.ifBlank { tvAddress1?.text?.toString().orEmpty() },
                serviceId = serviceId,
                serviceNameSnapshot = serviceName,
                appointmentDate = appointmentDate,
                appointmentTime = appointmentTime,
                priceAtBooking = servicePrice,
                durationMinutes = serviceDuration,
                status = "PENDING",
                customerNotes = note
            )

            BookingRepository.createBooking(
                booking = booking,
                onSuccess = { bookingId, qrRef ->
                    if (!isAdded) return@createBooking
                    activity?.runOnUiThread {
                        btnConfirm.isEnabled = true
                        val frag = BookingConfirmationFragment.newInstance(bookingId)
                        (activity as? MainActivity)?.navigateTo(frag, addToBackStack = false)
                    }
                },
                onFailure = { e ->
                    if (!isAdded) return@createBooking
                    activity?.runOnUiThread {
                        btnConfirm.isEnabled = true
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to create booking.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}
