package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class AdminOverviewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup bottom navigation for Admin Overview
        AdminNavigationHelper.setupBottomNav(this, view, AdminTab.OVERVIEW)

        val mainActivity = activity as? MainActivity

        // Connect quick access cards and buttons
        view.findViewById<View>(R.id.card_users)?.setOnClickListener {
            mainActivity?.navigateTo(AdminUserManagementFragment(), addToBackStack = true)
        }

        view.findViewById<View>(R.id.card_business_listings)?.setOnClickListener {
            mainActivity?.navigateTo(AdminBusinessListingsFragment(), addToBackStack = true)
        }

        view.findViewById<View>(R.id.card_pending_approvals)?.setOnClickListener {
            mainActivity?.navigateTo(AdminApplicationsFragment(), addToBackStack = true)
        }

        view.findViewById<View>(R.id.card_verified_businesses)?.setOnClickListener {
            mainActivity?.navigateTo(AdminBusinessListingsFragment(), addToBackStack = true)
        }

        // Action queue buttons
        view.findViewById<TextView>(R.id.btn_action_review_docs)?.setOnClickListener {
            mainActivity?.navigateTo(AdminApplicationsFragment(), addToBackStack = true)
        }

        view.findViewById<TextView>(R.id.btn_action_open_reports)?.setOnClickListener {
            mainActivity?.navigateTo(AdminReviewReportModerationFragment(), addToBackStack = true)
        }

        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(AdminAnnouncementsFragment(), addToBackStack = true)
        }
    }
}
