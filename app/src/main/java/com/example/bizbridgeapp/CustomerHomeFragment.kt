package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class CustomerHomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customer_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ─── Search bar click ──────────────────────────────────────────
        val searchBar = view.findViewById<LinearLayout>(R.id.search_bar)
        searchBar?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = true)
        }

        // ─── Notification bell click ───────────────────────────────────
        val btnNotifications = view.findViewById<View>(R.id.btn_notifications)
        btnNotifications?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }

        // ─── Location row click ────────────────────────────────────────
        val locationRow = view.findViewById<LinearLayout>(R.id.location_row)
        locationRow?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(ExploreNearbyFragment(), addToBackStack = true)
        }

        // ─── Category chips ────────────────────────────────────────────
        val chipSalon = view.findViewById<LinearLayout>(R.id.chip_salon)
        val chipBeauty = view.findViewById<LinearLayout>(R.id.chip_beauty)
        val chipRepair = view.findViewById<LinearLayout>(R.id.chip_repair)
        val chipWellness = view.findViewById<LinearLayout>(R.id.chip_wellness)

        val allChips = listOf(chipSalon, chipBeauty, chipRepair, chipWellness)
        allChips.forEach { chip ->
            chip?.setOnClickListener {
                (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = true)
            }
        }

        // ─── View all appointments ─────────────────────────────────────
        val tvViewAllApts = view.findViewById<TextView>(R.id.tv_view_all_appointments)
        tvViewAllApts?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(MyBookingsFragment(), addToBackStack = true)
        }

        // ─── Appointment card click ────────────────────────────────────
        val cardAppointment = view.findViewById<LinearLayout>(R.id.card_appointment)
        cardAppointment?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment(), addToBackStack = true)
        }

        // ─── "View booking →" click ────────────────────────────────────
        val tvViewBooking = view.findViewById<TextView>(R.id.tv_view_booking)
        tvViewBooking?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment(), addToBackStack = true)
        }

        // ─── Map button click ──────────────────────────────────────────
        val btnMap = view.findViewById<LinearLayout>(R.id.btn_map)
        btnMap?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(ExploreNearbyFragment(), addToBackStack = true)
        }

        // ─── Nearby business card click ────────────────────────────────
        val cardNearby = view.findViewById<LinearLayout>(R.id.card_nearby_business)
        cardNearby?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment(), addToBackStack = true)
        }

        // ─── Popular card clicks ───────────────────────────────────────
        val cardPopular1 = view.findViewById<LinearLayout>(R.id.card_popular_1)
        val cardPopular2 = view.findViewById<LinearLayout>(R.id.card_popular_2)
        val cardPopular3 = view.findViewById<LinearLayout>(R.id.card_popular_3)

        cardPopular1?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment(), addToBackStack = true)
        }
        cardPopular2?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment(), addToBackStack = true)
        }
        cardPopular3?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment(), addToBackStack = true)
        }

        // ─── See all popular ───────────────────────────────────────────
        val tvSeeAllPopular = view.findViewById<TextView>(R.id.tv_see_all_popular)
        tvSeeAllPopular?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = true)
        }

        // ─── Bottom navigation ─────────────────────────────────────────
        val navHome = view.findViewById<LinearLayout>(R.id.nav_home)
        val navSearch = view.findViewById<LinearLayout>(R.id.nav_search)
        val navBookings = view.findViewById<LinearLayout>(R.id.nav_bookings)
        val navNotifications = view.findViewById<LinearLayout>(R.id.nav_notifications)
        val navProfile = view.findViewById<LinearLayout>(R.id.nav_profile)

        navHome?.setOnClickListener {
            // Already on Home
        }
        navSearch?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = true)
        }
        navBookings?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(MyBookingsFragment(), addToBackStack = true)
        }
        navNotifications?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }
        navProfile?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerProfileFragment(), addToBackStack = true)
        }
    }
}
