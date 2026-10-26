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
        val ivHomeAvatar = view.findViewById<ImageView>(R.id.iv_home_avatar)
        val tvHomeInitials = view.findViewById<TextView>(R.id.tv_avatar_initials)

        if (fullName.isNotEmpty()) {
            val firstName = fullName.split(" ").firstOrNull() ?: fullName
            view.findViewById<TextView>(R.id.tv_greeting)?.text = "Good day, $firstName ☀️"
            val initials = if (fullName.contains(" ")) {
                val p = fullName.split(" ")
                "${p[0].take(1)}${p[1].take(1)}".uppercase()
            } else {
                fullName.take(2).uppercase()
            }
            tvHomeInitials?.text = initials
        }

        val homeImageUrl = (profile?.get("profileImageUrl") as? String)?.ifBlank { null }
        if (!homeImageUrl.isNullOrBlank()) {
            ivHomeAvatar?.visibility = View.VISIBLE
            tvHomeInitials?.visibility = View.GONE
            if (ivHomeAvatar != null) {
                com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivHomeAvatar, homeImageUrl, R.drawable.bg_avatar_circle)
            }
        } else {
            ivHomeAvatar?.visibility = View.GONE
            tvHomeInitials?.visibility = View.VISIBLE
        }

        view.findViewById<View>(R.id.btn_home_profile_avatar)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerProfileFragment(), addToBackStack = true)
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
        val tvNoApt = view.findViewById<TextView>(R.id.tv_no_upcoming_appointment)
        val cardApt = view.findViewById<LinearLayout>(R.id.card_appointment)
        val openAppointment = View.OnClickListener {
            val bId = upcomingBooking?.id
            if (!bId.isNullOrBlank()) {
                (activity as? MainActivity)?.navigateTo(BookingDetailsFragment.newInstance(bId), addToBackStack = true)
            }
        }
        cardApt?.setOnClickListener(openAppointment)
        view.findViewById<TextView>(R.id.tv_view_booking)?.setOnClickListener(openAppointment)

        if (uid.isNotEmpty()) {
            BookingRepository.getUpcomingBookingForCustomer(uid,
                onSuccess = { booking ->
                    if (!isAdded) return@getUpcomingBookingForCustomer
                    upcomingBooking = booking
                    if (booking != null) {
                        tvNoApt?.visibility = View.GONE
                        cardApt?.visibility = View.VISIBLE
                        view.findViewById<TextView>(R.id.tv_apt_business_name)?.text = booking.businessNameSnapshot
                        view.findViewById<TextView>(R.id.tv_apt_category)?.text = booking.serviceNameSnapshot
                        view.findViewById<TextView>(R.id.tv_apt_badge)?.text = booking.status
                        view.findViewById<TextView>(R.id.tv_apt_service)?.text = "${booking.serviceNameSnapshot} · ${booking.appointmentDate} · ${booking.appointmentTime}"
                    } else {
                        cardApt?.visibility = View.GONE
                        tvNoApt?.visibility = View.VISIBLE
                    }
                },
                onFailure = {
                    if (isAdded) {
                        cardApt?.visibility = View.GONE
                        tvNoApt?.visibility = View.VISIBLE
                    }
                }
            )
        } else {
            cardApt?.visibility = View.GONE
            tvNoApt?.visibility = View.VISIBLE
        }

        // ─── Map button click ──────────────────────────────────────────
        val btnMap = view.findViewById<LinearLayout>(R.id.btn_map)
        btnMap?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(ExploreNearbyFragment(), addToBackStack = true)
        }

        // ─── Real-Time Approved Businesses Sync ────────────────────────
        val tvNoNearby = view.findViewById<TextView>(R.id.tv_no_nearby_businesses)
        val cardNearby = view.findViewById<LinearLayout>(R.id.card_nearby_business)
        val tvNoPopular = view.findViewById<TextView>(R.id.tv_no_popular_businesses)
        val scrollPopular = view.findViewById<View>(R.id.scroll_popular_businesses)
        val cardP1 = view.findViewById<LinearLayout>(R.id.card_popular_1)
        val cardP2 = view.findViewById<LinearLayout>(R.id.card_popular_2)
        val cardP3 = view.findViewById<LinearLayout>(R.id.card_popular_3)

        businessesListener?.remove()
        businessesListener = BusinessRepository.listenToApprovedBusinesses { list ->
            if (!isAdded) return@listenToApprovedBusinesses
            activeBusinesses = list

            if (list.isEmpty()) {
                cardNearby?.visibility = View.GONE
                tvNoNearby?.visibility = View.VISIBLE
                scrollPopular?.visibility = View.GONE
                tvNoPopular?.visibility = View.VISIBLE
                return@listenToApprovedBusinesses
            }

            // Bind Nearby card
            val b0 = list[0]
            tvNoNearby?.visibility = View.GONE
            cardNearby?.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tv_nearby_name)?.text = b0.name
            view.findViewById<TextView>(R.id.tv_nearby_category)?.text = b0.category
            view.findViewById<TextView>(R.id.tv_nearby_rating)?.text = String.format(java.util.Locale.US, "%.1f", b0.rating ?: 5.0)
            view.findViewById<TextView>(R.id.tv_nearby_reviews)?.text = "(${b0.reviewCount} reviews)"
            view.findViewById<TextView>(R.id.tv_nearby_distance)?.text = if (b0.address.isNotBlank()) b0.address else "Nearby"
            view.findViewById<TextView>(R.id.tv_nearby_price)?.text = if (!b0.price.isNullOrBlank()) b0.price else "Explore"
            
            val ivNearby = view.findViewById<android.widget.ImageView>(R.id.img_nearby_business)
            if (ivNearby != null) {
                com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivNearby, b0.imageUrl, R.drawable.img_salon_interior)
            }

            cardNearby?.setOnClickListener {
                (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance(b0.id), addToBackStack = true)
            }

            // Bind Popular cards dynamically
            tvNoPopular?.visibility = View.GONE
            scrollPopular?.visibility = View.VISIBLE

            fun bindPopularCard(cardView: View?, biz: Business?) {
                if (cardView == null) return
                if (biz == null) {
                    cardView.visibility = View.GONE
                    return
                }
                cardView.visibility = View.VISIBLE
                val ivCard = (cardView as? LinearLayout)?.getChildAt(0) as? android.widget.ImageView
                if (ivCard != null) {
                    com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivCard, biz.imageUrl, R.drawable.img_salon_interior)
                }

                val contentLayout = (cardView as? LinearLayout)?.getChildAt(1) as? LinearLayout
                val tvName = contentLayout?.getChildAt(0) as? TextView
                val tvCat = contentLayout?.getChildAt(1) as? TextView
                val ratingRow = contentLayout?.getChildAt(2) as? LinearLayout
                val tvRating = ratingRow?.getChildAt(1) as? TextView
                val tvReviews = ratingRow?.getChildAt(2) as? TextView

                tvName?.text = biz.name
                tvCat?.text = biz.category
                tvRating?.text = String.format(java.util.Locale.US, "%.1f", biz.rating ?: 5.0)
                tvReviews?.text = "(${biz.reviewCount})"

                cardView.setOnClickListener {
                    (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance(biz.id), addToBackStack = true)
                }
            }

            bindPopularCard(cardP1, list.getOrNull(0))
            bindPopularCard(cardP2, list.getOrNull(1))
            bindPopularCard(cardP3, list.getOrNull(2))
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
