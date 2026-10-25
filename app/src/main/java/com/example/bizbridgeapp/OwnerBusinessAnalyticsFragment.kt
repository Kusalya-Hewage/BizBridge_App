package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class OwnerBusinessAnalyticsFragment : Fragment() {

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

        // Timeframe selector
        view.findViewById<TextView>(R.id.tv_analytics_timeframe)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filter analytics timeframe (Demonstration).", Toast.LENGTH_SHORT).show()
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
    }
}
