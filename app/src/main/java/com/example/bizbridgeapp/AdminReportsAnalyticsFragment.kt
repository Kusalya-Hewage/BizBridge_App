package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class AdminReportsAnalyticsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_reports_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup bottom nav for Reports tab
        AdminNavigationHelper.setupBottomNav(this, view, AdminTab.REPORTS)

        val mainActivity = activity as? MainActivity

        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(AdminAnnouncementsFragment(), addToBackStack = true)
        }

        view.findViewById<View>(R.id.btn_export_sample)?.setOnClickListener {
            Toast.makeText(requireContext(), "Sample report exported to CSV (Demonstration).", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_date_range)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filtering date range: Oct 1–23, 2026", Toast.LENGTH_SHORT).show()
        }
    }
}
