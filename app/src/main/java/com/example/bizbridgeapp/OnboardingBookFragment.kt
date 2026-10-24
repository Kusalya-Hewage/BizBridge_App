package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingBookFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_book, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBackNav = view.findViewById<ImageView>(R.id.btn_back_nav)
        val btnBack = view.findViewById<Button>(R.id.btn_back)
        val btnNext = view.findViewById<Button>(R.id.btn_next)
        val chipDiscover = view.findViewById<TextView>(R.id.chip_discover)
        val chipEnjoy = view.findViewById<TextView>(R.id.chip_enjoy)

        // Top back arrow navigation
        btnBackNav?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingDiscoverFragment(), addToBackStack = false)
        }

        // Bottom Back button
        btnBack?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingDiscoverFragment(), addToBackStack = false)
        }

        // Bottom Next button
        btnNext?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingRemindersFragment(), addToBackStack = false)
        }

        // Step chips direct navigation
        chipDiscover?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingDiscoverFragment(), addToBackStack = false)
        }

        chipEnjoy?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingRemindersFragment(), addToBackStack = false)
        }
    }
}
