package com.example.bizbridgeapp

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

enum class AdminTab {
    OVERVIEW, APPLICATIONS, USERS, REPORTS, PROFILE
}

object AdminNavigationHelper {

    fun setupBottomNav(
        fragment: Fragment,
        rootView: View,
        currentTab: AdminTab
    ) {
        val navOverview = rootView.findViewById<View>(R.id.nav_tab_overview)
        val navApps = rootView.findViewById<View>(R.id.nav_tab_applications)
        val navUsers = rootView.findViewById<View>(R.id.nav_tab_users)
        val navReports = rootView.findViewById<View>(R.id.nav_tab_reports)
        val navProfile = rootView.findViewById<View>(R.id.nav_tab_profile)

        val ivOverview = rootView.findViewById<ImageView>(R.id.iv_nav_overview)
        val tvOverview = rootView.findViewById<TextView>(R.id.tv_nav_overview)
        val ivApps = rootView.findViewById<ImageView>(R.id.iv_nav_applications)
        val tvApps = rootView.findViewById<TextView>(R.id.tv_nav_applications)
        val ivUsers = rootView.findViewById<ImageView>(R.id.iv_nav_users)
        val tvUsers = rootView.findViewById<TextView>(R.id.tv_nav_users)
        val ivReports = rootView.findViewById<ImageView>(R.id.iv_nav_reports)
        val tvReports = rootView.findViewById<TextView>(R.id.tv_nav_reports)
        val ivProfile = rootView.findViewById<ImageView>(R.id.iv_nav_profile)
        val tvProfile = rootView.findViewById<TextView>(R.id.tv_nav_profile)

        val context = fragment.requireContext()
        val activeColor = ContextCompat.getColor(context, R.color.admin_primary)
        val inactiveColor = ContextCompat.getColor(context, R.color.admin_nav_inactive)

        // Reset all
        ivOverview?.setColorFilter(inactiveColor)
        tvOverview?.setTextColor(inactiveColor)
        ivApps?.setColorFilter(inactiveColor)
        tvApps?.setTextColor(inactiveColor)
        ivUsers?.setColorFilter(inactiveColor)
        tvUsers?.setTextColor(inactiveColor)
        ivReports?.setColorFilter(inactiveColor)
        tvReports?.setTextColor(inactiveColor)
        ivProfile?.setColorFilter(inactiveColor)
        tvProfile?.setTextColor(inactiveColor)

        // Highlight active tab
        when (currentTab) {
            AdminTab.OVERVIEW -> {
                ivOverview?.setColorFilter(activeColor)
                tvOverview?.setTextColor(activeColor)
            }
            AdminTab.APPLICATIONS -> {
                ivApps?.setColorFilter(activeColor)
                tvApps?.setTextColor(activeColor)
            }
            AdminTab.USERS -> {
                ivUsers?.setColorFilter(activeColor)
                tvUsers?.setTextColor(activeColor)
            }
            AdminTab.REPORTS -> {
                ivReports?.setColorFilter(activeColor)
                tvReports?.setTextColor(activeColor)
            }
            AdminTab.PROFILE -> {
                ivProfile?.setColorFilter(activeColor)
                tvProfile?.setTextColor(activeColor)
            }
        }

        val mainActivity = fragment.activity as? MainActivity

        navOverview?.setOnClickListener {
            if (currentTab != AdminTab.OVERVIEW) {
                mainActivity?.navigateTo(AdminOverviewFragment(), addToBackStack = false)
            }
        }

        navApps?.setOnClickListener {
            if (currentTab != AdminTab.APPLICATIONS) {
                mainActivity?.navigateTo(AdminApplicationsFragment(), addToBackStack = false)
            }
        }

        navUsers?.setOnClickListener {
            if (currentTab != AdminTab.USERS) {
                mainActivity?.navigateTo(AdminUserManagementFragment(), addToBackStack = false)
            }
        }

        navReports?.setOnClickListener {
            if (currentTab != AdminTab.REPORTS) {
                mainActivity?.navigateTo(AdminReportsAnalyticsFragment(), addToBackStack = false)
            }
        }

        navProfile?.setOnClickListener {
            if (currentTab != AdminTab.PROFILE) {
                mainActivity?.navigateTo(AdminProfileSecurityFragment(), addToBackStack = false)
            }
        }
    }
}
