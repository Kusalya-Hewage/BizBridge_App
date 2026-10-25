package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.BusinessRepository
import com.google.firebase.firestore.ListenerRegistration

class OwnerBusinessAnalyticsFragment : Fragment() {

    private var bookingsListener: ListenerRegistration? = null
    private var currentBusinessId: String = "biz_juniper_studio"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_business_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bottom Navigation
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.INSIGHTS)

        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Timeframe selector
        view.findViewById<TextView>(R.id.tv_analytics_timeframe)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filtering live analytics for current period.", Toast.LENGTH_SHORT).show()
        }

        // Toggle pills for Weekly / Monthly
        val pillWeekly = view.findViewById<TextView>(R.id.pill_chart_weekly)
        val pillMonthly = view.findViewById<TextView>(R.id.pill_chart_monthly)

        pillWeekly?.setOnClickListener {
            pillWeekly.setBackgroundResource(R.drawable.bg_chip_selected)
            pillWeekly.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            pillMonthly?.setBackgroundResource(R.drawable.bg_chip_default)
            pillMonthly?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        pillMonthly?.setOnClickListener {
            pillMonthly.setBackgroundResource(R.drawable.bg_chip_selected)
            pillMonthly.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            pillWeekly?.setBackgroundResource(R.drawable.bg_chip_default)
            pillWeekly?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            Toast.makeText(requireContext(), "Switched to monthly booking overview.", Toast.LENGTH_SHORT).show()
        }

        // Real-time listener to compute live analytics from actual Firestore bookings
        bookingsListener = BookingRepository.listenToBookingsForBusiness(currentBusinessId) { list ->
            if (!isAdded) return@listenToBookingsForBusiness
            val total = list.size
            val completed = list.count { it.status == "COMPLETED" }
            val pending = list.count { it.status == "PENDING" }
            val confirmed = list.count { it.status == "CONFIRMED" }
            val cancelled = list.count { it.status == "CANCELLED" }

            val completionRate = if (total > 0) ((completed.toDouble() / total) * 100).toInt() else 0

            view.findViewById<TextView>(R.id.tv_analytics_total_bookings)?.text = total.toString()
            view.findViewById<TextView>(R.id.tv_analytics_completed_count)?.text = completed.toString()
            view.findViewById<TextView>(R.id.tv_analytics_completion_rate)?.text = "$completionRate% completion rate"
            view.findViewById<TextView>(R.id.tv_analytics_status_breakdown)?.text =
                "$pending Pending · $confirmed Confirmed · $cancelled Cancelled"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bookingsListener?.remove()
    }
}
