package com.example.bizbridgeapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class SplashFragment : Fragment() {

    private val handler = Handler(Looper.getMainLooper())
    private val navigateRunnable = Runnable {
        navigateToOnboarding()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tap anywhere to skip 2.5s timer and proceed immediately
        view.setOnClickListener {
            handler.removeCallbacks(navigateRunnable)
            navigateToOnboarding()
        }

        // Keep Splash visible for 2.5 seconds so user sees branding, logo, and tagline
        handler.postDelayed(navigateRunnable, 2500)
    }

    private fun navigateToOnboarding() {
        if (!isAdded) return
        val currentUser = FirebaseAuthManager.getCurrentUser()
        if (currentUser != null) {
            UserSessionManager.init(requireContext())
            val email = currentUser.email.orEmpty().trim().lowercase()
            val localAcc = UserSessionManager.getRegisteredAccount(email)
            val savedProfile = UserSessionManager.getProfile()

            // Resolve authoritative role from registered accounts, session profile, or business ownership
            val role = (localAcc?.get("role") as? String)?.ifBlank { null }
                ?: (savedProfile?.get("role") as? String)?.ifBlank { null }
                ?: if (UserSessionManager.getAllBusinesses().any { it.ownerId == currentUser.uid || (email.isNotEmpty() && it.id.contains("aura", ignoreCase = true)) }) "BUSINESS_OWNER" else ""

            if (localAcc != null && (savedProfile == null || (savedProfile["email"] as? String)?.lowercase() != email)) {
                UserSessionManager.saveProfile(localAcc)
            }

            when {
                role.equals("ADMIN", ignoreCase = true) ||
                email.equals("alex@bizbridge.example", ignoreCase = true) ||
                email.equals("admin@bizbridge.com", ignoreCase = true) -> {
                    (activity as? MainActivity)?.navigateTo(AdminOverviewFragment(), addToBackStack = false)
                }
                role.equals("BUSINESS_OWNER", ignoreCase = true) || role.equals("OWNER", ignoreCase = true) -> {
                    (activity as? MainActivity)?.navigateTo(OwnerDashboardFragment(), addToBackStack = false)
                }
                else -> {
                    (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
                }
            }
        } else {
            (activity as? MainActivity)?.navigateTo(OnboardingDiscoverFragment(), addToBackStack = false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(navigateRunnable)
    }
}
