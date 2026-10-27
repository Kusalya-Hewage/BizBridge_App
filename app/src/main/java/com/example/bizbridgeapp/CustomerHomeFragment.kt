package com.example.bizbridgeapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.NotificationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class CustomerHomeFragment : Fragment() {

    private var activeBusinesses: List<Business> = emptyList()
    private var upcomingBooking: Booking? = null
    private var businessesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentDeviceLocation: Location? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchRealLocation()
        } else {
            onLocationFallback()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customer_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

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

        // ─── Real Device Location Setup ────────────────────────────────
        val tvLocation = view.findViewById<TextView>(R.id.tv_location)
        val cachedLocName = UserSessionManager.getLastKnownLocationName()
        if (!cachedLocName.isNullOrBlank()) {
            tvLocation?.text = cachedLocName
        }
        checkLocationPermissionAndFetch()

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
        locationRow?.setOnClickListener { v ->
            val popup = PopupMenu(requireContext(), v)
            popup.menu.add(0, 1, 0, "📍 Update Current Location")
            popup.menu.add(0, 2, 1, "🗺️ Explore on Map")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        checkLocationPermissionAndFetch(showFeedbackToast = true)
                        true
                    }
                    2 -> {
                        (activity as? MainActivity)?.navigateTo(ExploreNearbyFragment(), addToBackStack = true)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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
            view.findViewById<TextView>(R.id.tv_nearby_trust_score)?.text = "Trust: ${b0.trustScore}/100"
            view.findViewById<TextView>(R.id.tv_nearby_price)?.text = if (!b0.price.isNullOrBlank()) b0.price else "Explore"
            
            if (currentDeviceLocation != null) {
                updateNearbyDistanceDisplay(currentDeviceLocation!)
            } else {
                view.findViewById<TextView>(R.id.tv_nearby_distance)?.text = if (b0.address.isNotBlank()) b0.address else "Nearby"
            }
            
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

    private fun checkLocationPermissionAndFetch(showFeedbackToast: Boolean = false) {
        val context = context ?: return
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            if (showFeedbackToast) {
                Toast.makeText(context, "Detecting real location...", Toast.LENGTH_SHORT).show()
            }
            fetchRealLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchRealLocation() {
        val context = context ?: return
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (!isAdded) return@addOnSuccessListener
                if (loc != null) {
                    onLocationRetrieved(loc)
                } else {
                    requestFreshLocation()
                }
            }.addOnFailureListener {
                if (isAdded) requestFreshLocation()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            onLocationFallback()
        }
    }

    private fun requestFreshLocation() {
        val context = context ?: return
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (!isAdded) return@addOnSuccessListener
                    if (loc != null) {
                        onLocationRetrieved(loc)
                    } else {
                        onLocationFallback()
                    }
                }.addOnFailureListener {
                    if (isAdded) onLocationFallback()
                }
        } catch (e: SecurityException) {
            onLocationFallback()
        }
    }

    private fun onLocationRetrieved(location: Location) {
        currentDeviceLocation = location
        resolveAddressName(location.latitude, location.longitude) { addressName ->
            if (!isAdded) return@resolveAddressName
            view?.findViewById<TextView>(R.id.tv_location)?.text = addressName
            UserSessionManager.saveLastKnownLocation(addressName, location.latitude, location.longitude)
        }
        updateNearbyDistanceDisplay(location)
    }

    private fun onLocationFallback() {
        val cached = UserSessionManager.getLastKnownLocationName()
        if (!cached.isNullOrBlank()) {
            view?.findViewById<TextView>(R.id.tv_location)?.text = cached
        } else {
            view?.findViewById<TextView>(R.id.tv_location)?.text = "Colombo, Sri Lanka"
        }
    }

    private fun resolveAddressName(latitude: Double, longitude: Double, onResult: (String) -> Unit) {
        val context = context ?: return
        Thread {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val formatted = formatAddress(addresses.firstOrNull(), latitude, longitude)
                        activity?.runOnUiThread {
                            onResult(formatted)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val formatted = formatAddress(addresses?.firstOrNull(), latitude, longitude)
                    activity?.runOnUiThread {
                        onResult(formatted)
                    }
                }
            } catch (e: Exception) {
                val fallback = formatFallbackCoordinates(latitude, longitude)
                activity?.runOnUiThread {
                    onResult(fallback)
                }
            }
        }.start()
    }

    private fun formatAddress(address: Address?, latitude: Double, longitude: Double): String {
        if (address == null) return formatFallbackCoordinates(latitude, longitude)

        val subLocality = address.subLocality ?: address.thoroughfare
        val locality = address.locality ?: address.subAdminArea ?: address.adminArea

        return when {
            !subLocality.isNullOrBlank() && !locality.isNullOrBlank() && !subLocality.equals(locality, ignoreCase = true) -> {
                "$subLocality, $locality"
            }
            !locality.isNullOrBlank() -> {
                locality
            }
            !subLocality.isNullOrBlank() -> {
                subLocality
            }
            !address.adminArea.isNullOrBlank() -> {
                address.adminArea
            }
            else -> {
                address.featureName ?: formatFallbackCoordinates(latitude, longitude)
            }
        }
    }

    private fun formatFallbackCoordinates(latitude: Double, longitude: Double): String {
        val knownCities = listOf(
            "Colombo" to (6.9271 to 79.8612),
            "Galle" to (6.0535 to 80.2210),
            "Kandy" to (7.2906 to 80.6337),
            "Negombo" to (7.2008 to 79.8736),
            "Matara" to (5.9549 to 80.5550),
            "Jaffna" to (9.6615 to 80.0255),
            "Gampaha" to (7.0840 to 79.9925),
            "Kurunegala" to (7.4863 to 80.3623)
        )
        for ((cityName, coords) in knownCities) {
            val dLat = latitude - coords.first
            val dLng = longitude - coords.second
            if (dLat * dLat + dLng * dLng < 0.05) {
                return cityName
            }
        }
        return String.format(Locale.US, "%.2f, %.2f", latitude, longitude)
    }

    private fun updateNearbyDistanceDisplay(location: Location) {
        if (activeBusinesses.isEmpty()) return
        val b0 = activeBusinesses[0]
        val tvNearbyDistance = view?.findViewById<TextView>(R.id.tv_nearby_distance) ?: return

        val lat = b0.latitude
        val lng = b0.longitude
        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            val bizLoc = Location("").apply {
                latitude = lat
                longitude = lng
            }
            val meters = location.distanceTo(bizLoc)
            val distStr = if (meters < 1000) {
                "${meters.toInt()}m away"
            } else {
                String.format(Locale.US, "%.1f km away", meters / 1000f)
            }
            tvNearbyDistance.text = if (b0.address.isNotBlank()) "${b0.address} · $distStr" else distStr
        } else if (b0.address.isNotBlank()) {
            tvNearbyDistance.text = b0.address
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        businessesListener?.remove()
    }
}
