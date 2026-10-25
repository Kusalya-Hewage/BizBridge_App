package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.BusinessRepository
import android.widget.TextView

class BusinessDetailsFragment : Fragment() {

    private var currentBusinessId: String? = null
    private var currentBusinessName: String? = null

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

        // Service items click -> also opens SelectServiceFragment
        view.findViewById<LinearLayout>(R.id.item_service_1)?.setOnClickListener {
            val frag = SelectServiceFragment.newInstance(
                businessId = currentBusinessId.orEmpty(),
                businessName = currentBusinessName.orEmpty()
            )
            (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.item_service_2)?.setOnClickListener {
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
                    view?.findViewById<TextView>(R.id.txt_business_name)?.text = it.name
                    view?.findViewById<TextView>(R.id.txt_business_name_main)?.text = it.name
                    view?.findViewById<TextView>(R.id.txt_category)?.text = it.category
                    val ratingStr = if (it.rating != null && it.rating > 0) String.format(java.util.Locale.US, "%.1f", it.rating) else "5.0"
                    view?.findViewById<TextView>(R.id.txt_rating)?.text = "$ratingStr (${it.reviewCount} reviews)"
                    view?.findViewById<TextView>(R.id.txt_description)?.text = if (it.description.isNotBlank()) it.description else "Welcome to ${it.name}! Professional services tailored for you."
                    view?.findViewById<TextView>(R.id.txt_address)?.text = if (it.address.isNotBlank()) it.address else "Colombo, Sri Lanka"
                    view?.findViewById<TextView>(R.id.txt_trust_score)?.text = "${it.trustScore} / 100"

                    val imgView = view?.findViewById<ImageView>(R.id.img_business)
                    if (imgView != null) {
                        com.example.bizbridgeapp.utils.ImageLoader.loadImage(imgView, it.imageUrl, R.drawable.img_salon_interior)
                    }
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
}
