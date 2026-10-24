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
    private var businessName: String = "Juniper Studio"
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

        val etNote = view.findViewById<EditText>(R.id.et_booking_note)
        val btnConfirm = view.findViewById<Button>(R.id.btn_confirm_booking)

        // Confirm Booking button -> calls BookingRepository.createBooking
        btnConfirm?.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val currentUid = user?.uid ?: "demo_customer"

            btnConfirm.isEnabled = false
            val note = etNote?.text?.toString()?.trim().orEmpty()

            val booking = Booking(
                customerId = currentUid,
                businessId = if (businessId.isNotEmpty()) businessId else "biz_juniper_studio",
                businessNameSnapshot = if (businessName.isNotEmpty()) businessName else "Juniper Studio",
                businessAddressSnapshot = "42 Flower Road, Colombo 07",
                serviceId = if (serviceId.isNotEmpty()) serviceId else "srv_haircut",
                serviceNameSnapshot = if (serviceName.isNotEmpty()) serviceName else "Signature haircut",
                appointmentDate = if (appointmentDate.isNotEmpty()) appointmentDate else "Saturday, October 24, 2026",
                appointmentTime = if (appointmentTime.isNotEmpty()) appointmentTime else "10:00 AM",
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
