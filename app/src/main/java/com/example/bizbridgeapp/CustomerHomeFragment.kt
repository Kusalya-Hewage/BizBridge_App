package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth

class CustomerHomeFragment : Fragment() {

    private var activeBusinesses: List<Business> = emptyList()
    private var upcomingBooking: Booking? = null
    private var businessesListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customer_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid.orEmpty()

        // ─── Bind Customer Profile & Greeting ──────────────────────────
        UserSessionManager.init(requireContext())
        val profile = UserSessionManager.getProfile()
        val fullName = (profile?.get("fullName") as? String).orEmpty()
        if (fullName.isNotEmpty()) {
            val firstName = fullName.split(" ").firstOrNull() ?: fullName
            view.findViewById<TextView>(R.id.tv_greeting)?.text = "Good day, $firstName ☀️"
            val initials = if (fullName.contains(" ")) {
                val p = fullName.split(" ")
                "${p[0].take(1)}${p[1].take(1)}".uppercase()
            } else {
                fullName.take(2).uppercase()
            }
            view.findViewById<TextView>(R.id.tv_avatar_initials)?.text = initials
        }

        // ─── Notification Dot Check ────────────────────────────────────
        if (uid.isNotEmpty()) {
            NotificationRepository.getUnreadCount(uid,
                onSuccess = { count ->
                    if (!isAdded) return@getUnreadCount
                    view.findViewById<View>(R.id.notification_dot)?.visibility = if (count > 0) View.VISIBLE else View.GONE
                },
                onFailure = {}
            )
        }

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

        chipSalon?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment.newInstance("Hair salon"), addToBackStack = true)
        }
        chipBeauty?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment.newInstance("Beauty & Spa"), addToBackStack = true)
        }
        chipRepair?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment.newInstance("Repair & Maintenance"), addToBackStack = true)
        }
        chipWellness?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment.newInstance("Wellness"), addToBackStack = true)
        }

        // ─── View all appointments ─────────────────────────────────────
        val tvViewAllApts = view.findViewById<TextView>(R.id.tv_view_all_appointments)
        tvViewAllApts?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(MyBookingsFragment(), addToBackStack = true)
        }

        // ─── Upcoming Appointment ──────────────────────────────────────
        val customerUid = uid.ifEmpty { "demo_customer" }
        BookingRepository.getUpcomingBookingForCustomer(customerUid,
            onSuccess = { booking ->
                if (!isAdded) return@getUpcomingBookingForCustomer
                upcomingBooking = booking
                if (booking != null) {
                    view.findViewById<TextView>(R.id.tv_apt_business_name)?.text = booking.businessNameSnapshot
                    view.findViewById<TextView>(R.id.tv_apt_category)?.text = booking.serviceNameSnapshot
                    view.findViewById<TextView>(R.id.tv_apt_badge)?.text = booking.status
                    view.findViewById<TextView>(R.id.tv_apt_service)?.text = "${booking.serviceNameSnapshot} · ${booking.appointmentDate} · ${booking.appointmentTime}"
                }
            },
            onFailure = {}
        )

        // ─── Appointment card click ────────────────────────────────────
        val openAppointment = View.OnClickListener {
            val bId = upcomingBooking?.id ?: "bk_demo_1"
            (activity as? MainActivity)?.navigateTo(BookingDetailsFragment.newInstance(bId), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.card_appointment)?.setOnClickListener(openAppointment)
        view.findViewById<TextView>(R.id.tv_view_booking)?.setOnClickListener(openAppointment)

        // ─── Map button click ──────────────────────────────────────────
        val btnMap = view.findViewById<LinearLayout>(R.id.btn_map)
        btnMap?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(ExploreNearbyFragment(), addToBackStack = true)
        }

        // Default instant clicks for nearby & popular cards (never dead)
        view.findViewById<LinearLayout>(R.id.card_nearby_business)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance("biz_juniper_studio"), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.card_popular_1)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance("biz_juniper_studio"), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.card_popular_2)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance("biz_oliver_beauty"), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.card_popular_3)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance("biz_daily_cut"), addToBackStack = true)
        }

        // ─── Real-Time Approved Businesses Sync ────────────────────────
        businessesListener?.remove()
        businessesListener = BusinessRepository.listenToApprovedBusinesses { list ->
            if (!isAdded) return@listenToApprovedBusinesses
            activeBusinesses = list

            val b0 = list.getOrNull(0)
            val b1 = list.getOrNull(1)
            val b2 = list.getOrNull(2)

            // Nearby card
            if (b0 != null) {
                view.findViewById<TextView>(R.id.tv_nearby_name)?.text = b0.name
                view.findViewById<TextView>(R.id.tv_nearby_category)?.text = b0.category
                view.findViewById<TextView>(R.id.tv_nearby_rating)?.text = String.format("%.1f", b0.rating ?: 4.9)
                view.findViewById<TextView>(R.id.tv_nearby_reviews)?.text = "(${b0.reviewCount} reviews)"
                view.findViewById<LinearLayout>(R.id.card_nearby_business)?.setOnClickListener {
                    (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance(b0.id), addToBackStack = true)
                }
            }

            // Popular cards
            if (b0 != null) {
                view.findViewById<LinearLayout>(R.id.card_popular_1)?.setOnClickListener {
                    (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance(b0.id), addToBackStack = true)
                }
            }
            if (b1 != null) {
                view.findViewById<LinearLayout>(R.id.card_popular_2)?.setOnClickListener {
                    (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance(b1.id), addToBackStack = true)
                }
            }
            if (b2 != null) {
                view.findViewById<LinearLayout>(R.id.card_popular_3)?.setOnClickListener {
                    (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance(b2.id), addToBackStack = true)
                }
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        businessesListener?.remove()
    }
}
