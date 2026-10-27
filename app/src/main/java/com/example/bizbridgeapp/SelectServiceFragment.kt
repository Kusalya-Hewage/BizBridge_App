package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Service
import com.example.bizbridgeapp.repository.ServiceRepository

class SelectServiceFragment : Fragment() {

    private var selectedIndex = -1
    private var businessId: String = ""
    private var businessName: String = ""
    private var servicesList: List<Service> = emptyList()

    companion object {
        fun newInstance(businessId: String, businessName: String): SelectServiceFragment {
            val frag = SelectServiceFragment()
            val args = Bundle()
            args.putString("businessId", businessId)
            args.putString("businessName", businessName)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        businessId = arguments?.getString("businessId").orEmpty()
        businessName = arguments?.getString("businessName").orEmpty()
        return inflater.inflate(R.layout.fragment_select_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (businessName.isNotBlank()) {
            view.findViewById<TextView>(R.id.tv_select_service_biz_name)?.text = businessName
        }

        val btnContinue = view.findViewById<Button>(R.id.btn_continue)
        btnContinue?.isEnabled = false
        btnContinue?.text = "Select a service"

        loadServices(view)

        btnContinue?.setOnClickListener {
            if (selectedIndex in servicesList.indices) {
                val selected = servicesList[selectedIndex]
                val frag = ChooseDateTimeFragment.newInstance(
                    businessId = businessId,
                    businessName = businessName,
                    serviceId = selected.id,
                    serviceName = selected.name,
                    servicePrice = selected.price,
                    serviceDuration = selected.durationMinutes
                )
                (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
            }
        }
    }

    private fun loadServices(rootView: View) {
        ServiceRepository.getServicesForBusiness(businessId,
            onSuccess = { list ->
                if (!isAdded) return@getServicesForBusiness
                servicesList = list
                renderServices(rootView)
            },
            onFailure = {
                if (!isAdded) return@getServicesForBusiness
                servicesList = emptyList()
                renderServices(rootView)
            }
        )
    }

    private fun renderServices(rootView: View) {
        val container = rootView.findViewById<LinearLayout>(R.id.ll_select_services_container) ?: return
        val emptyView = rootView.findViewById<TextView>(R.id.tv_no_services_available)
        val tvSummary = rootView.findViewById<TextView>(R.id.tv_summary_selection)
        val btnContinue = rootView.findViewById<Button>(R.id.btn_continue)

        container.removeAllViews()

        if (servicesList.isEmpty()) {
            emptyView?.visibility = View.VISIBLE
            tvSummary?.text = "No services available for booking"
            btnContinue?.isEnabled = false
            btnContinue?.text = "No Service Available"
            return
        }

        emptyView?.visibility = View.GONE
        val density = resources.displayMetrics.density

        val cardViews = mutableListOf<LinearLayout>()
        val radioViews = mutableListOf<ImageView>()

        fun updateUISelection(index: Int) {
            selectedIndex = index
            val service = servicesList[index]
            for (i in cardViews.indices) {
                if (i == index) {
                    cardViews[i].background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_selected_purple)
                    radioViews[i].setImageResource(R.drawable.ic_radio_selected)
                } else {
                    cardViews[i].background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_white)
                    radioViews[i].setImageResource(R.drawable.ic_radio_unselected)
                }
            }
            tvSummary?.text = "1 service selected • ${service.durationMinutes} minutes"
            btnContinue?.isEnabled = true
            btnContinue?.text = "Continue - Rs. ${service.price.toInt()}"
        }

        for ((index, s) in servicesList.withIndex()) {
            val card = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (12 * density).toInt()
                }
                orientation = LinearLayout.HORIZONTAL
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_white)
                elevation = (1 * density)
                val pad = (16 * density).toInt()
                setPadding(pad, pad, pad, pad)
                isClickable = true
                isFocusable = true
            }

            val textLayout = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                orientation = LinearLayout.VERTICAL
            }

            val tvTitle = TextView(requireContext()).apply {
                text = s.name
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }

            val tvDesc = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (4 * density).toInt()
                }
                text = if (s.description.isNotBlank()) s.description else "Professional service delivered with care."
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }

            val tvPriceTime = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * density).toInt()
                }
                text = "Rs. ${s.price.toInt()} • ${s.durationMinutes} min"
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }

            textLayout.addView(tvTitle)
            textLayout.addView(tvDesc)
            textLayout.addView(tvPriceTime)

            val radio = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (24 * density).toInt(),
                    (24 * density).toInt()
                ).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    marginStart = (12 * density).toInt()
                }
                setImageResource(R.drawable.ic_radio_unselected)
            }

            card.addView(textLayout)
            card.addView(radio)

            cardViews.add(card)
            radioViews.add(radio)

            card.setOnClickListener {
                updateUISelection(index)
            }

            container.addView(card)
        }

        // Default select first item
        updateUISelection(0)
    }
}
