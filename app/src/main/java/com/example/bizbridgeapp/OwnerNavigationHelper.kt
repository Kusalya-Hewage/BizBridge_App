package com.example.bizbridgeapp

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

enum class OwnerTab {
    HOME, BOOKINGS, SERVICES, INSIGHTS, PROFILE
}

object OwnerNavigationHelper {

    fun setupBottomNav(
        fragment: Fragment,
        rootView: View,
        currentTab: OwnerTab
    ) {
        val navHome = rootView.findViewById<View>(R.id.nav_owner_home)
        val navBookings = rootView.findViewById<View>(R.id.nav_owner_bookings)
        val navServices = rootView.findViewById<View>(R.id.nav_owner_services)
        val navInsights = rootView.findViewById<View>(R.id.nav_owner_insights)
        val navProfile = rootView.findViewById<View>(R.id.nav_owner_profile)

        val ivHome = rootView.findViewById<ImageView>(R.id.iv_owner_home)
        val tvHome = rootView.findViewById<TextView>(R.id.tv_owner_home)
        val ivBookings = rootView.findViewById<ImageView>(R.id.iv_owner_bookings)
        val tvBookings = rootView.findViewById<TextView>(R.id.tv_owner_bookings)
        val ivServices = rootView.findViewById<ImageView>(R.id.iv_owner_services)
        val tvServices = rootView.findViewById<TextView>(R.id.tv_owner_services)
        val ivInsights = rootView.findViewById<ImageView>(R.id.iv_owner_insights)
        val tvInsights = rootView.findViewById<TextView>(R.id.tv_owner_insights)
        val ivProfile = rootView.findViewById<ImageView>(R.id.iv_owner_profile)
        val tvProfile = rootView.findViewById<TextView>(R.id.tv_owner_profile)

        val context = fragment.requireContext()
        val activeColor = ContextCompat.getColor(context, R.color.primary)
        val inactiveColor = ContextCompat.getColor(context, R.color.text_muted)

        // Reset all
        ivHome?.setColorFilter(inactiveColor)
        tvHome?.setTextColor(inactiveColor)
        ivBookings?.setColorFilter(inactiveColor)
        tvBookings?.setTextColor(inactiveColor)
        ivServices?.setColorFilter(inactiveColor)
        tvServices?.setTextColor(inactiveColor)
        ivInsights?.setColorFilter(inactiveColor)
        tvInsights?.setTextColor(inactiveColor)
        ivProfile?.setColorFilter(inactiveColor)
        tvProfile?.setTextColor(inactiveColor)

        // Highlight active tab
        when (currentTab) {
            OwnerTab.HOME -> {
                ivHome?.setColorFilter(activeColor)
                tvHome?.setTextColor(activeColor)
            }
            OwnerTab.BOOKINGS -> {
                ivBookings?.setColorFilter(activeColor)
                tvBookings?.setTextColor(activeColor)
            }
            OwnerTab.SERVICES -> {
                ivServices?.setColorFilter(activeColor)
                tvServices?.setTextColor(activeColor)
            }
            OwnerTab.INSIGHTS -> {
                ivInsights?.setColorFilter(activeColor)
                tvInsights?.setTextColor(activeColor)
            }
            OwnerTab.PROFILE -> {
                ivProfile?.setColorFilter(activeColor)
                tvProfile?.setTextColor(activeColor)
            }
        }

        val mainActivity = fragment.activity as? MainActivity

        navHome?.setOnClickListener {
            if (currentTab != OwnerTab.HOME) {
                mainActivity?.navigateTo(OwnerDashboardFragment(), addToBackStack = false)
            }
        }

        navBookings?.setOnClickListener {
            if (currentTab != OwnerTab.BOOKINGS) {
                mainActivity?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = false)
            }
        }

        navServices?.setOnClickListener {
            if (currentTab != OwnerTab.SERVICES) {
                mainActivity?.navigateTo(OwnerManageServicesFragment(), addToBackStack = false)
            }
        }

        navInsights?.setOnClickListener {
            if (currentTab != OwnerTab.INSIGHTS) {
                mainActivity?.navigateTo(OwnerBusinessAnalyticsFragment(), addToBackStack = false)
            }
        }

        navProfile?.setOnClickListener {
            if (currentTab != OwnerTab.PROFILE) {
                mainActivity?.navigateTo(OwnerProfileFragment(), addToBackStack = false)
            }
        }
    }
}
