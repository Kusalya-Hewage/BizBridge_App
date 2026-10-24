package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.AdminRepository
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.util.Locale

class AdminOverviewFragment : Fragment() {

    private var statsListener: ListenerRegistration? = null

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

        // Real-time Overview Statistics
        statsListener = AdminRepository.listenToOverviewStats { stats ->
            if (!isAdded) return@listenToOverviewStats
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            view.findViewById<TextView>(R.id.tv_stat_users)?.text = formatter.format(stats.totalUsers)
            view.findViewById<TextView>(R.id.tv_stat_businesses)?.text = formatter.format(stats.totalBusinesses)
            view.findViewById<TextView>(R.id.tv_stat_pending)?.text = formatter.format(stats.pendingApplications)
            view.findViewById<TextView>(R.id.tv_stat_verified)?.text = formatter.format(stats.verifiedBusinesses)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        statsListener?.remove()
    }
}
