package com.example.bizbridgeapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.repository.BusinessRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class ExploreNearbyFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null
    private var allBusinesses: List<Business> = emptyList()
    private val markerList = mutableListOf<Marker>()
    private var selectedBusiness: Business? = null

    // Default Colombo, Sri Lanka coordinates
    private val colomboLatLng = LatLng(6.9271, 79.8612)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            enableMyLocation()
        } else {
            zoomToColombo()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore_nearby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize SupportMapFragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Toggle Grid
        view.findViewById<LinearLayout>(R.id.btn_toggle_grid)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = false)
        }

        // Distance filter
        view.findViewById<LinearLayout>(R.id.btn_filter_distance)?.setOnClickListener {
            Toast.makeText(requireContext(), "Showing businesses within Colombo area", Toast.LENGTH_SHORT).show()
        }

        // My Location Button
        view.findViewById<LinearLayout>(R.id.btn_my_location)?.setOnClickListener {
            if (userLocation != null) {
                val latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                checkLocationPermissionAndEnable()
            }
        }

        // View Details button & card click
        val openDetails = View.OnClickListener {
            val bizId = selectedBusiness?.id
            if (!bizId.isNullOrBlank()) {
                (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance(bizId), addToBackStack = true)
            }
        }
        view.findViewById<Button>(R.id.btn_view_details)?.setOnClickListener(openDetails)
        view.findViewById<LinearLayout>(R.id.card_selected_business)?.setOnClickListener(openDetails)

        // Search input text watcher
        val etSearch = view.findViewById<EditText>(R.id.et_search_map)
        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty().lowercase()
                val filtered = if (query.isEmpty()) {
                    allBusinesses
                } else {
                    allBusinesses.filter {
                        it.name.lowercase().contains(query) ||
                        it.category.lowercase().contains(query) ||
                        it.address.lowercase().contains(query)
                    }
                }
                updateMarkers(filtered)
                filtered.firstOrNull()?.let { match ->
                    val pos = getEffectiveCoordinates(match)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            pos,
                            14f
                        )
                    )
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Fetch real approved and registered businesses from repository
        loadBusinesses()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isCompassEnabled = true

        map.setOnMarkerClickListener { marker ->
            val biz = marker.tag as? Business
            if (biz != null) {
                displayBusinessCard(biz)
                map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                true
            } else {
                false
            }
        }

        checkLocationPermissionAndEnable()
        updateMarkers(allBusinesses)
    }

    private fun checkLocationPermissionAndEnable() {
        val context = context ?: return
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            enableMyLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            zoomToColombo()
        }
    }

    private fun enableMyLocation() {
        val map = googleMap ?: return
        val context = context ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                map.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (!isAdded) return@addOnSuccessListener
                if (loc != null) {
                    userLocation = loc
                    val userLatLng = LatLng(loc.latitude, loc.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                    if (allBusinesses.isNotEmpty()) {
                        updateMarkers(allBusinesses)
                    }
                } else {
                    zoomToColombo()
                }
            }.addOnFailureListener {
                zoomToColombo()
            }
        }
    }

    private fun zoomToColombo() {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(colomboLatLng, 13f))
    }

    private fun loadBusinesses() {
        BusinessRepository.getBusinessesForMap(
            onSuccess = { list ->
                if (!isAdded) return@getBusinessesForMap
                allBusinesses = list
                updateMarkers(list)
            },
            onFailure = {
                if (!isAdded) return@getBusinessesForMap
                allBusinesses = emptyList()
                updateMarkers(allBusinesses)
            }
        )
    }

    private fun getEffectiveCoordinates(biz: Business): LatLng {
        val lat = biz.latitude
        val lng = biz.longitude
        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            return LatLng(lat, lng)
        }
        val addr = biz.address.lowercase(Locale.ROOT)
        val baseCoords = when {
            addr.contains("kandy") -> LatLng(7.2906, 80.6337)
            addr.contains("galle") -> LatLng(6.0535, 80.2210)
            addr.contains("negombo") -> LatLng(7.2008, 79.8736)
            addr.contains("matara") -> LatLng(5.9549, 80.5550)
            addr.contains("jaffna") -> LatLng(9.6615, 80.0255)
            addr.contains("kurunegala") -> LatLng(7.4863, 80.3623)
            addr.contains("gampaha") -> LatLng(7.0840, 79.9925)
            else -> LatLng(6.9271, 79.8612) // Colombo default
        }
        val hash = Math.abs(biz.id.ifBlank { biz.name }.hashCode())
        val offsetLat = ((hash % 100) - 50) * 0.0003
        val offsetLng = (((hash / 100) % 100) - 50) * 0.0003
        return LatLng(baseCoords.latitude + offsetLat, baseCoords.longitude + offsetLng)
    }

    private fun updateMarkers(businesses: List<Business>) {
        val map = googleMap ?: return
        map.clear()
        markerList.clear()

        for (biz in businesses) {
            val position = getEffectiveCoordinates(biz)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(biz.name)
                    .snippet(biz.category)
            )
            marker?.tag = biz
            if (marker != null) {
                markerList.add(marker)
            }
        }

        val card = view?.findViewById<LinearLayout>(R.id.card_selected_business)
        if (businesses.isEmpty()) {
            card?.visibility = View.GONE
            selectedBusiness = null
        } else {
            val toSelect = selectedBusiness?.takeIf { b -> businesses.any { it.id == b.id } } ?: businesses.first()
            displayBusinessCard(toSelect)
            if (userLocation == null) {
                val pos = getEffectiveCoordinates(toSelect)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
            }
        }
    }

    private fun displayBusinessCard(biz: Business) {
        selectedBusiness = biz
        val card = view?.findViewById<LinearLayout>(R.id.card_selected_business) ?: return
        card.visibility = View.VISIBLE

        val tvName = view?.findViewById<TextView>(R.id.tv_business_name)
        val tvCategoryDist = view?.findViewById<TextView>(R.id.tv_business_category_dist)
        val tvRating = view?.findViewById<TextView>(R.id.tv_business_rating)
        val tvPrice = view?.findViewById<TextView>(R.id.tv_business_price)
        val badgeVerified = view?.findViewById<View>(R.id.badge_verified)

        tvName?.text = biz.name

        val coords = getEffectiveCoordinates(biz)
        val distanceText = userLocation?.let { loc ->
            val results = FloatArray(1)
            Location.distanceBetween(loc.latitude, loc.longitude, coords.latitude, coords.longitude, results)
            val km = results[0] / 1000f
            String.format(Locale.US, "%.1f km away", km)
        } ?: "Colombo"

        tvCategoryDist?.text = "${biz.category} • $distanceText"
        val ratingVal = if (biz.rating != null && biz.rating > 0.0) String.format(Locale.US, "%.1f", biz.rating) else "5.0"
        tvRating?.text = "$ratingVal (${biz.reviewCount} reviews)"
        tvPrice?.text = biz.price ?: "From $25"
        badgeVerified?.visibility = if (biz.approved || biz.verificationStatus.equals("VERIFIED", ignoreCase = true)) View.VISIBLE else View.GONE
    }
}
