package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingRemindersFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_reminders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBackNav = view.findViewById<ImageView>(R.id.btn_back_nav)
        val btnGetStarted = view.findViewById<Button>(R.id.btn_get_started)
        val chipDiscover = view.findViewById<TextView>(R.id.chip_discover)
        val chipBook = view.findViewById<TextView>(R.id.chip_book)

        // Top back arrow navigation
        btnBackNav?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingBookFragment(), addToBackStack = false)
        }

        // Get Started button
        btnGetStarted?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = false)
        }

        // Step chips direct navigation
        chipDiscover?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingDiscoverFragment(), addToBackStack = false)
        }

        chipBook?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingBookFragment(), addToBackStack = false)
        }
    }
}
