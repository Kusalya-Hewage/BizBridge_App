package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

import android.widget.ImageView
import android.widget.TextView
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.utils.QRCodeUtil

class BookingConfirmationFragment : Fragment() {

    private var bookingId: String? = null

    companion object {
        fun newInstance(bookingId: String): BookingConfirmationFragment {
            val frag = BookingConfirmationFragment()
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
        bookingId = arguments?.getString("bookingId")
        return inflater.inflate(R.layout.fragment_booking_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back to Home -> returns to CustomerHomeFragment
        view.findViewById<Button>(R.id.btn_back_to_home)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
        }

        // View My Bookings -> navigates to MyBookingsFragment
        view.findViewById<Button>(R.id.btn_view_my_bookings)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(MyBookingsFragment(), addToBackStack = true)
        }

        val id = bookingId.orEmpty()
        if (id.isNotEmpty()) {
            BookingRepository.getBookingById(id,
                onSuccess = { booking ->
                    booking?.let { b ->
                        if (!isAdded) return@let
                        view.findViewById<TextView>(R.id.tv_booking_ref)?.text = "Booking: ${b.qrReference}"
                        view.findViewById<TextView>(R.id.tv_booking_status)?.text = b.status
                        view.findViewById<TextView>(R.id.tv_business_name)?.text = b.businessNameSnapshot
                        view.findViewById<TextView>(R.id.tv_service_info)?.text = "${b.serviceNameSnapshot} • $${b.priceAtBooking.toInt()} • ${b.durationMinutes} min"
                        view.findViewById<TextView>(R.id.tv_date_time)?.text = "${b.appointmentDate} • ${b.appointmentTime}"
                        if (b.businessAddressSnapshot.isNotEmpty()) {
                            view.findViewById<TextView>(R.id.tv_address)?.text = b.businessAddressSnapshot
                        }

                        // Generate actual QR code from qrReference
                        val qrBitmap = QRCodeUtil.generateQRCode(b.qrReference, 300, 300)
                        if (qrBitmap != null) {
                            view.findViewById<ImageView>(R.id.iv_qr_code)?.setImageBitmap(qrBitmap)
                        }
                    }
                },
                onFailure = {}
            )
        }
    }
}
