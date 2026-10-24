package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class AdminApplicationsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_applications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup bottom nav for Applications tab
        AdminNavigationHelper.setupBottomNav(this, view, AdminTab.APPLICATIONS)

        val mainActivity = activity as? MainActivity

        // Open application detail (Willow Wellness)
        val openWillow = View.OnClickListener {
            mainActivity?.navigateTo(AdminApplicationReviewFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.card_willow_wellness)?.setOnClickListener(openWillow)
        view.findViewById<View>(R.id.btn_open_app_willow)?.setOnClickListener(openWillow)

        view.findViewById<View>(R.id.card_linden_beauty)?.setOnClickListener(openWillow)
        view.findViewById<View>(R.id.btn_open_app_linden)?.setOnClickListener(openWillow)

        view.findViewById<View>(R.id.card_fixwell_repairs)?.setOnClickListener(openWillow)
        view.findViewById<View>(R.id.btn_open_app_fixwell)?.setOnClickListener(openWillow)

        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(AdminAnnouncementsFragment(), addToBackStack = true)
        }
    }
}
