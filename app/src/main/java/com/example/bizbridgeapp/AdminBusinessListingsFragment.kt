package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class AdminBusinessListingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_business_listings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup bottom nav
        AdminNavigationHelper.setupBottomNav(this, view, AdminTab.OVERVIEW)

        val mainActivity = activity as? MainActivity

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(AdminAnnouncementsFragment(), addToBackStack = true)
        }

        val onActionClick = View.OnClickListener {
            Toast.makeText(requireContext(), "Moderation action recorded in audit log.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_action_juniper)?.setOnClickListener(onActionClick)
        view.findViewById<View>(R.id.btn_action_bloom)?.setOnClickListener(onActionClick)
        view.findViewById<View>(R.id.btn_action_forma)?.setOnClickListener(onActionClick)
    }
}
