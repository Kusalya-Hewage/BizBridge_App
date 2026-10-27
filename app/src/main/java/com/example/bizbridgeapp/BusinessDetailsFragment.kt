package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Service
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.ServiceRepository
import com.example.bizbridgeapp.repository.TrustScoreRepository

class BusinessDetailsFragment : Fragment() {

    private var currentBusinessId: String? = null
    private var currentBusinessName: String? = null
    private var currentBusinessAddress: String? = null

    companion object {
        fun newInstance(businessId: String): BusinessDetailsFragment {
            val fragment = BusinessDetailsFragment()
            val args = Bundle()
            args.putString("businessId", businessId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_business_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Book Appointment button -> navigates to SelectServiceFragment
        view.findViewById<Button>(R.id.btn_book_appointment)?.setOnClickListener {
            val frag = SelectServiceFragment.newInstance(
                businessId = currentBusinessId.orEmpty(),
                businessName = currentBusinessName.orEmpty()
            )
            (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
        }

        // Load business details
        val bId = arguments?.getString("businessId")
        if (!bId.isNullOrBlank()) {
            loadBusiness(bId)
        } else {
            // Load first available approved active business as fallback
            BusinessRepository.getApprovedActiveBusinesses(
                onSuccess = { list ->
                    list.firstOrNull()?.let { b ->
                        loadBusiness(b.id)
                    }
                },
                onFailure = {}
            )
        }
    }

    private fun loadBusiness(businessId: String) {
        currentBusinessId = businessId
        BusinessRepository.getBusinessById(businessId,
            onSuccess = { business ->
                business?.let {
                    if (!isAdded) return@let
                    currentBusinessName = it.name
                    currentBusinessAddress = it.address
                    view?.findViewById<TextView>(R.id.txt_business_name)?.text = it.name
                    view?.findViewById<TextView>(R.id.txt_business_name_main)?.text = it.name
                    view?.findViewById<TextView>(R.id.txt_category)?.text = it.category
                    val ratingStr = if (it.rating != null && it.rating > 0) String.format(java.util.Locale.US, "%.1f", it.rating) else "5.0"
                    view?.findViewById<TextView>(R.id.txt_rating)?.text = "$ratingStr (${it.reviewCount} reviews)"
                    view?.findViewById<TextView>(R.id.txt_description)?.text = if (it.description.isNotBlank()) it.description else "Welcome to ${it.name}! Professional services tailored for you."
                    view?.findViewById<TextView>(R.id.txt_address)?.text = if (it.address.isNotBlank()) it.address else "Colombo, Sri Lanka"

                    // Dynamically load Trust Score and all 5 breakdown metrics
                    TrustScoreRepository.getTrustScoreBreakdown(businessId) { breakdown ->
                        if (!isAdded) return@getTrustScoreBreakdown
                        view?.findViewById<TextView>(R.id.txt_trust_score)?.text = "${breakdown.totalScore} / 100"
                        view?.findViewById<TextView>(R.id.txt_trust_score_caption)?.text = "${breakdown.trustBadge} · ${breakdown.trustDescription}"
                        view?.findViewById<TextView>(R.id.txt_metric_qualifications)?.text = "${breakdown.qualificationScore}/${breakdown.maxQualificationScore}"
                        view?.findViewById<TextView>(R.id.txt_metric_experience)?.text = "${breakdown.experienceScore}/${breakdown.maxExperienceScore}"
                        view?.findViewById<TextView>(R.id.txt_metric_profile)?.text = "${breakdown.profileScore}/${breakdown.maxProfileScore}"
                        view?.findViewById<TextView>(R.id.txt_metric_documents)?.text = "${breakdown.docScore}/${breakdown.maxDocScore}"
                        view?.findViewById<TextView>(R.id.txt_metric_reviews)?.text = "${breakdown.reviewScore}/${breakdown.maxReviewScore}"
                    }

                    val imgView = view?.findViewById<ImageView>(R.id.img_business)
                    if (imgView != null) {
                        com.example.bizbridgeapp.utils.ImageLoader.loadImage(imgView, it.imageUrl, R.drawable.img_salon_interior)
                    }

                    // Dynamically load services for this business
                    loadServices(businessId, it.name)
                } ?: run {
                    if (isAdded) {
                        android.widget.Toast.makeText(requireContext(), "Business not found or not accessible", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onFailure = { e ->
                if (isAdded) {
                    android.widget.Toast.makeText(requireContext(), "Error loading business: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun loadServices(businessId: String, businessName: String) {
        ServiceRepository.getServicesForBusiness(businessId,
            onSuccess = { services ->
                if (!isAdded) return@getServicesForBusiness
                val container = view?.findViewById<LinearLayout>(R.id.ll_business_services_container) ?: return@getServicesForBusiness
                val tvNoServices = view?.findViewById<TextView>(R.id.tv_no_services_found)

                container.removeAllViews()

                if (services.isEmpty()) {
                    tvNoServices?.visibility = View.VISIBLE
                    return@getServicesForBusiness
                }
                tvNoServices?.visibility = View.GONE

                val density = resources.displayMetrics.density

                for (service in services) {
                    val card = LinearLayout(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = (10 * density).toInt()
                        }
                        orientation = LinearLayout.VERTICAL
                        background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_white)
                        elevation = (1 * density)
                        val pad = (16 * density).toInt()
                        setPadding(pad, pad, pad, pad)
                        isClickable = true
                        isFocusable = true
                    }

                    val rowTop = RelativeLayout(requireContext()).apply {
                        layoutParams = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val tvTitle = TextView(requireContext()).apply {
                        layoutParams = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_START)
                        }
                        text = service.name
                        textSize = 15f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    }

                    val tvPrice = TextView(requireContext()).apply {
                        layoutParams = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_END)
                        }
                        text = "$${service.price.toInt()}"
                        textSize = 15f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    }

                    rowTop.addView(tvTitle)
                    rowTop.addView(tvPrice)

                    val tvDesc = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = (4 * density).toInt()
                        }
                        text = if (service.description.isNotBlank()) service.description else "Professional service delivered with care."
                        textSize = 13f
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }

                    val tvDuration = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = (6 * density).toInt()
                        }
                        text = "${service.durationMinutes} min"
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                    }

                    card.addView(rowTop)
                    card.addView(tvDesc)
                    card.addView(tvDuration)

                    card.setOnClickListener {
                        val frag = ChooseDateTimeFragment.newInstance(
                            businessId = businessId,
                            businessName = businessName,
                            serviceId = service.id,
                            serviceName = service.name,
                            servicePrice = service.price,
                            serviceDuration = service.durationMinutes
                        )
                        (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
                    }

                    container.addView(card)
                }
            },
            onFailure = {}
        )
    }
}
