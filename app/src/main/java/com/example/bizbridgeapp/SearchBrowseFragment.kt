package com.example.bizbridgeapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.repository.BusinessRepository

class SearchBrowseFragment : Fragment() {

    private var selectedCategory: String = "All"
    private var loadedBusinesses: List<Business> = emptyList()

    private var businessListContainer: LinearLayout? = null
    private var pbLoading: ProgressBar? = null
    private var llEmptyState: LinearLayout? = null
    private var tvEmptyMessage: TextView? = null
    private var tvResultsCount: TextView? = null
    private var etSearchInput: EditText? = null
    private var btnClearSearch: ImageView? = null

    private var chipAll: LinearLayout? = null
    private var chipSalon: LinearLayout? = null
    private var chipBeauty: LinearLayout? = null
    private var chipWellness: LinearLayout? = null

    companion object {
        fun newInstance(category: String = "All"): SearchBrowseFragment {
            val frag = SearchBrowseFragment()
            val args = Bundle()
            args.putString("category", category)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        selectedCategory = arguments?.getString("category") ?: "All"
        return inflater.inflate(R.layout.fragment_search_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        businessListContainer = view.findViewById(R.id.business_list_container)
        pbLoading = view.findViewById(R.id.pb_loading)
        llEmptyState = view.findViewById(R.id.ll_empty_state)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)
        tvResultsCount = view.findViewById(R.id.tv_results_count)
        etSearchInput = view.findViewById(R.id.et_search_input)
        btnClearSearch = view.findViewById(R.id.btn_clear_search)

        chipAll = view.findViewById(R.id.chip_all)
        chipSalon = view.findViewById(R.id.chip_salon)
        chipBeauty = view.findViewById(R.id.chip_beauty)
        chipWellness = view.findViewById(R.id.chip_wellness)

        // Top navigation
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<ImageView>(R.id.btn_bell)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }

        // Chip selection listeners
        chipAll?.setOnClickListener { updateSelectedCategory("All") }
        chipSalon?.setOnClickListener { updateSelectedCategory("Hair salon") }
        chipBeauty?.setOnClickListener { updateSelectedCategory("Beauty & Spa") }
        chipWellness?.setOnClickListener { updateSelectedCategory("Wellness") }

        // Search text watcher
        etSearchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty().trim()
                btnClearSearch?.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                if (query.isNotEmpty()) {
                    performSearch(query)
                } else {
                    loadBusinesses(selectedCategory)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch?.setOnClickListener {
            etSearchInput?.setText("")
            loadBusinesses(selectedCategory)
        }

        // Bottom Navigation
        view.findViewById<LinearLayout>(R.id.nav_home)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
        }
        view.findViewById<LinearLayout>(R.id.nav_bookings)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(MyBookingsFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_notifications)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_profile)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerProfileFragment(), addToBackStack = true)
        }

        updateChipStyles(selectedCategory)
        loadBusinesses(selectedCategory)
    }

    private fun updateSelectedCategory(category: String) {
        selectedCategory = category
        updateChipStyles(category)
        etSearchInput?.setText("")
        loadBusinesses(category)
    }

    private fun updateChipStyles(activeCategory: String) {
        val chips = listOf(
            Triple(chipAll, "All", "All"),
            Triple(chipSalon, "Hair salon", "Salon"),
            Triple(chipBeauty, "Beauty & Spa", "Beauty"),
            Triple(chipWellness, "Wellness", "Wellness")
        )

        for ((chipLayout, catValue, _) in chips) {
            val isSelected = activeCategory.equals(catValue, ignoreCase = true)
            chipLayout?.setBackgroundResource(
                if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_default
            )
            val tv = chipLayout?.getChildAt(0) as? TextView
            if (isSelected) {
                tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.surface_white))
            } else {
                tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }

    private fun loadBusinesses(category: String) {
        showLoading(true)
        if (category == "All") {
            BusinessRepository.getApprovedActiveBusinesses(
                onSuccess = { list ->
                    if (!isAdded) return@getApprovedActiveBusinesses
                    showLoading(false)
                    renderBusinesses(list)
                },
                onFailure = { e ->
                    if (!isAdded) return@getApprovedActiveBusinesses
                    showLoading(false)
                    showError(e)
                }
            )
        } else {
            BusinessRepository.getBusinessesByCategory(category,
                onSuccess = { list ->
                    if (!isAdded) return@getBusinessesByCategory
                    showLoading(false)
                    renderBusinesses(list)
                },
                onFailure = { e ->
                    if (!isAdded) return@getBusinessesByCategory
                    showLoading(false)
                    showError(e)
                }
            )
        }
    }

    private fun performSearch(query: String) {
        showLoading(true)
        BusinessRepository.searchBusinesses(query,
            onSuccess = { list ->
                if (!isAdded) return@searchBusinesses
                showLoading(false)
                renderBusinesses(list)
            },
            onFailure = { e ->
                if (!isAdded) return@searchBusinesses
                showLoading(false)
                showError(e)
            }
        )
    }

    private fun renderBusinesses(list: List<Business>) {
        val container = businessListContainer ?: return
        container.removeAllViews()
        loadedBusinesses = list

        val countText = if (list.size == 1) "1 business found" else "${list.size} businesses found"
        tvResultsCount?.text = countText

        if (list.isEmpty()) {
            llEmptyState?.visibility = View.VISIBLE
            tvEmptyMessage?.text = "No businesses found in \"$selectedCategory\"."
            return
        }

        llEmptyState?.visibility = View.GONE

        val inflater = LayoutInflater.from(requireContext())
        for (biz in list) {
            val cardView = inflater.inflate(R.layout.item_business_card, container, false)

            val tvName = cardView.findViewById<TextView>(R.id.tv_business_name)
            val tvCategoryAddress = cardView.findViewById<TextView>(R.id.tv_business_category_address)
            val tvRating = cardView.findViewById<TextView>(R.id.tv_business_rating)
            val tvPrice = cardView.findViewById<TextView>(R.id.tv_business_price)
            val ivImage = cardView.findViewById<ImageView>(R.id.iv_business_image)

            tvName?.text = biz.name
            val locationPart = if (biz.address.isNotEmpty()) biz.address else "Colombo"
            tvCategoryAddress?.text = "${biz.category} · $locationPart"

            val ratingVal = biz.rating ?: 4.8
            tvRating?.text = String.format("%.1f (%d)", ratingVal, biz.reviewCount)
            cardView.findViewById<TextView>(R.id.tv_business_trust_score)?.text = "Trust: ${biz.trustScore}/100"
            tvPrice?.text = biz.price ?: "From $25"

            // Set business image (real imageUrl or category fallback)
            if (!biz.imageUrl.isNullOrBlank()) {
                if (ivImage != null) {
                    val fallback = if (biz.category.contains("beauty", ignoreCase = true) || biz.category.contains("spa", ignoreCase = true)) {
                        R.drawable.img_spa_treatment
                    } else {
                        R.drawable.img_salon_interior
                    }
                    com.example.bizbridgeapp.utils.ImageLoader.loadImage(ivImage, biz.imageUrl, fallback)
                }
            } else {
                when {
                    biz.category.contains("salon", ignoreCase = true) || biz.category.contains("hair", ignoreCase = true) -> {
                        ivImage?.setImageResource(R.drawable.img_salon_interior)
                    }
                    biz.category.contains("beauty", ignoreCase = true) || biz.category.contains("spa", ignoreCase = true) -> {
                        ivImage?.setImageResource(R.drawable.img_spa_treatment)
                    }
                    else -> {
                        ivImage?.setImageResource(R.drawable.img_salon_interior)
                    }
                }
            }

            // Click navigate to BusinessDetailsFragment with actual document ID
            cardView.setOnClickListener {
                (activity as? MainActivity)?.navigateTo(
                    BusinessDetailsFragment.newInstance(
                        businessId = biz.id,
                        businessName = biz.name,
                        category = biz.category,
                        address = biz.address,
                        imageUrl = biz.imageUrl,
                        rating = biz.rating,
                        reviewCount = biz.reviewCount,
                        trustScore = biz.trustScore,
                        description = biz.description
                    ),
                    addToBackStack = true
                )
            }

            container.addView(cardView)
        }
    }

    private fun showLoading(loading: Boolean) {
        pbLoading?.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            llEmptyState?.visibility = View.GONE
            tvResultsCount?.text = "Searching businesses..."
        }
    }

    private fun showError(e: Exception) {
        val msg = e.localizedMessage ?: "Permission denied or network error"
        if (loadedBusinesses.isEmpty()) {
            llEmptyState?.visibility = View.VISIBLE
            tvEmptyMessage?.text = "Error connecting to Firestore: $msg"
            tvResultsCount?.text = "Offline Mode"
        }
    }
}
