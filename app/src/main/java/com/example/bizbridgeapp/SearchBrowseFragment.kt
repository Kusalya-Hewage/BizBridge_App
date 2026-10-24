package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class SearchBrowseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Bell
        view.findViewById<ImageView>(R.id.btn_bell)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }

        // Business card 1: Juniper Studio
        view.findViewById<LinearLayout>(R.id.card_business_1)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment(), addToBackStack = true)
        }

        // Other cards
        view.findViewById<LinearLayout>(R.id.card_business_2)?.setOnClickListener {
            Toast.makeText(requireContext(), "Serene Day Spa details", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<LinearLayout>(R.id.card_business_3)?.setOnClickListener {
            Toast.makeText(requireContext(), "Bloom Beauty Bar details", Toast.LENGTH_SHORT).show()
        }

        // Filter chips
        val chipAll = view.findViewById<LinearLayout>(R.id.chip_all)
        val chipSalon = view.findViewById<LinearLayout>(R.id.chip_salon)
        val chipBeauty = view.findViewById<LinearLayout>(R.id.chip_beauty)
        val chipWellness = view.findViewById<LinearLayout>(R.id.chip_wellness)

        val chips = listOf(chipAll, chipSalon, chipBeauty, chipWellness)
        chips.forEach { chip ->
            chip?.setOnClickListener {
                Toast.makeText(requireContext(), "Filter applied", Toast.LENGTH_SHORT).show()
            }
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
    }
}
