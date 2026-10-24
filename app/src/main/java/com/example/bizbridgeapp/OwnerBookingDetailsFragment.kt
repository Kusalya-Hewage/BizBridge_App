package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class OwnerBookingDetailsFragment : Fragment() {

    private var bookingId: String = "BB-24108"
    private var customerName: String = "Maya Chen"
    private var customerEmail: String = "maya.chen@example.com"
    private var customerPhone: String = "+1 (415) 555-0182"
    private var serviceName: String = "Signature haircut · $45 · 45 min · Elena Rivera"
    private var dateTime: String = "Sat, October 24, 2026, 10:00–10:45 AM · Pacific Time"
    private var customerNote: String = "A shoulder-length cut with soft layers, please."

    companion object {
        fun newInstance(
            bookingId: String = "BB-24108",
            customerName: String = "Maya Chen",
            customerEmail: String = "maya.chen@example.com",
            customerPhone: String = "+1 (415) 555-0182",
            serviceName: String = "Signature haircut · $45 · 45 min · Elena Rivera",
            dateTime: String = "Sat, October 24, 2026, 10:00–10:45 AM · Pacific Time",
            customerNote: String = "A shoulder-length cut with soft layers, please."
        ): OwnerBookingDetailsFragment {
            val fragment = OwnerBookingDetailsFragment()
            val args = Bundle()
            args.putString("bookingId", bookingId)
            args.putString("customerName", customerName)
            args.putString("customerEmail", customerEmail)
            args.putString("customerPhone", customerPhone)
            args.putString("serviceName", serviceName)
            args.putString("dateTime", dateTime)
            args.putString("customerNote", customerNote)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookingId = it.getString("bookingId", bookingId)
            customerName = it.getString("customerName", customerName)
            customerEmail = it.getString("customerEmail", customerEmail)
            customerPhone = it.getString("customerPhone", customerPhone)
            serviceName = it.getString("serviceName", serviceName)
            dateTime = it.getString("dateTime", dateTime)
            customerNote = it.getString("customerNote", customerNote)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_booking_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_booking_details)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Bind data
        view.findViewById<TextView>(R.id.tv_booking_tag_badge)?.text = "$bookingId Confirmed"
        view.findViewById<TextView>(R.id.tv_detail_customer_name)?.text = customerName
        view.findViewById<TextView>(R.id.tv_detail_customer_contact)?.text = "$customerEmail · $customerPhone"
        view.findViewById<TextView>(R.id.tv_detail_service)?.text = serviceName
        view.findViewById<TextView>(R.id.tv_detail_date_time)?.text = dateTime
        view.findViewById<TextView>(R.id.tv_detail_customer_note)?.text = customerNote

        val initials = customerName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
        view.findViewById<TextView>(R.id.tv_customer_avatar_initials)?.text = if (initials.isNotEmpty()) initials else "MC"

        // QR Verification Button -> B12
        view.findViewById<Button>(R.id.btn_booking_qr_verification)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OwnerQrVerificationFragment.newInstance(bookingId), addToBackStack = true)
        }

        // Mark as Completed Button
        val btnComplete = view.findViewById<Button>(R.id.btn_booking_mark_completed)
        btnComplete?.setOnClickListener {
            btnComplete.isEnabled = false
            btnComplete.text = "Completed"
            btnComplete.setBackgroundResource(R.drawable.bg_button_disabled)
            view.findViewById<TextView>(R.id.tv_booking_tag_badge)?.text = "$bookingId Completed"
            view.findViewById<TextView>(R.id.tv_booking_tag_badge)?.setBackgroundResource(R.drawable.bg_badge_completed)
            view.findViewById<TextView>(R.id.tv_booking_tag_badge)?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.badge_completed_text))
            Toast.makeText(requireContext(), "$bookingId marked as Completed.", Toast.LENGTH_SHORT).show()
        }
    }
}
