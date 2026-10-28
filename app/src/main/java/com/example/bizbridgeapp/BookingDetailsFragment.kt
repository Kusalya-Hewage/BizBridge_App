package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

import android.widget.TextView
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.repository.BookingRepository

class BookingDetailsFragment : Fragment() {

    private var currentBookingId: String? = null
    private var currentBooking: Booking? = null

    companion object {
        fun newInstance(bookingId: String): BookingDetailsFragment {
            val frag = BookingDetailsFragment()
            val args = Bundle()
            args.putString("bookingId", bookingId)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        currentBookingId = arguments?.getString("bookingId")
        return inflater.inflate(R.layout.fragment_booking_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // View Demonstration QR -> navigates to BookingConfirmationFragment
        view.findViewById<Button>(R.id.btn_view_demo_qr)?.setOnClickListener {
            val bId = currentBookingId
            if (!bId.isNullOrBlank()) {
                val frag = BookingConfirmationFragment.newInstance(bId)
                (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
            } else {
                (activity as? MainActivity)?.navigateTo(BookingConfirmationFragment(), addToBackStack = true)
            }
        }

        // Cancel Booking
        view.findViewById<Button>(R.id.btn_cancel_booking)?.setOnClickListener {
            val bId = currentBookingId
            if (!bId.isNullOrBlank()) {
                BookingRepository.cancelBooking(bId, "Customer cancellation",
                    onSuccess = {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Booking cancelled", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    },
                    onFailure = { e ->
                        if (isAdded) {
                            Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to cancel booking", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Booking cancelled", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        // Load real booking data
        val id = currentBookingId.orEmpty()
        if (id.isNotEmpty()) {
            BookingRepository.getBookingById(id,
                onSuccess = { booking ->
                    booking?.let { b ->
                        currentBooking = b
                        if (!isAdded) return@let
                        view.findViewById<TextView>(R.id.tv_booking_id)?.text = b.qrReference.ifEmpty { b.id }
                        view.findViewById<TextView>(R.id.tv_booking_status)?.text = b.status
                        view.findViewById<TextView>(R.id.tv_business_name)?.text = b.businessNameSnapshot
                        view.findViewById<TextView>(R.id.tv_service_info)?.text = "${b.serviceNameSnapshot} â€¢ $${b.priceAtBooking.toInt()}"
                        view.findViewById<TextView>(R.id.tv_appointment_date)?.text = b.appointmentDate
                        view.findViewById<TextView>(R.id.tv_appointment_time)?.text = "${b.appointmentTime} â€¢ ${b.durationMinutes} min"
                        if (b.businessAddressSnapshot.isNotEmpty()) {
                            view.findViewById<TextView>(R.id.tv_address)?.text = b.businessAddressSnapshot
                        }

                        val btnCancel = view.findViewById<Button>(R.id.btn_cancel_booking)
                        if (b.status.equals("COMPLETED", ignoreCase = true)) {
                            btnCancel?.text = "Write a review"
                            btnCancel?.isEnabled = true
                            btnCancel?.setOnClickListener {
                                val frag = WriteReviewFragment.newInstance(
                                    bookingId = b.id,
                                    businessId = b.businessId,
                                    businessName = b.businessNameSnapshot
                                )
                                (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
                            }
                        } else if (b.status.equals("CANCELLED", ignoreCase = true)) {
                            btnCancel?.isEnabled = false
                            btnCancel?.text = "Booking Cancelled"
                        }
                    }
                },
                onFailure = {}
            )
        }
    }
}
