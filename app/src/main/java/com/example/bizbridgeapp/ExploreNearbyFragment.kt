package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class ExploreNearbyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore_nearby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            Toast.makeText(requireContext(), "Distance radius: 3 miles", Toast.LENGTH_SHORT).show()
        }

        // View Details button
        view.findViewById<Button>(R.id.btn_view_details)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance("biz_juniper_studio"), addToBackStack = true)
        }

        // Card click
        view.findViewById<LinearLayout>(R.id.card_selected_business)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance("biz_juniper_studio"), addToBackStack = true)
        }

        // Pin click
        view.findViewById<LinearLayout>(R.id.pin_juniper)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance("biz_juniper_studio"), addToBackStack = true)
        }
    }
}
