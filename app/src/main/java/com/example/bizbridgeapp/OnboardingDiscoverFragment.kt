package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingDiscoverFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_discover, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSkip = view.findViewById<Button>(R.id.btn_skip)
        val btnNext = view.findViewById<Button>(R.id.btn_next)
        val chipBook = view.findViewById<TextView>(R.id.chip_book)
        val chipEnjoy = view.findViewById<TextView>(R.id.chip_enjoy)

        // Bottom Skip button
        btnSkip?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = false)
        }

        // Bottom Next button
        btnNext?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingBookFragment(), addToBackStack = false)
        }

        // Step chips direct navigation
        chipBook?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingBookFragment(), addToBackStack = false)
        }

        chipEnjoy?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(OnboardingRemindersFragment(), addToBackStack = false)
        }
    }
}
