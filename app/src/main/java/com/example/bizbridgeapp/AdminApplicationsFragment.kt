package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.BusinessApplication
import com.example.bizbridgeapp.repository.AdminRepository
import com.google.firebase.firestore.ListenerRegistration

class AdminApplicationsFragment : Fragment() {

    private var appsListener: ListenerRegistration? = null
    private var allApplications: List<BusinessApplication> = emptyList()

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

        // Open application detail fallback for existing demo cards
        val openWillow = View.OnClickListener {
            val appId = allApplications.firstOrNull()?.id.orEmpty()
            mainActivity?.navigateTo(AdminApplicationReviewFragment.newInstance(appId), addToBackStack = true)
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

        // Listen to live business applications
        appsListener = AdminRepository.listenToApplications { list ->
            if (!isAdded) return@listenToApplications
            allApplications = list
            val pendingCount = list.count { it.status == "PENDING" }
            view.findViewById<TextView>(R.id.tab_pending)?.text = "Pending  $pendingCount"

            // Update first card with real application if available
            val first = list.firstOrNull()
            if (first != null) {
                view.findViewById<TextView>(R.id.tv_willow_name)?.text = first.businessName
                view.findViewById<TextView>(R.id.tv_willow_owner)?.text = first.ownerName
                view.findViewById<TextView>(R.id.tv_willow_category)?.text = "${first.category} · ${first.address}"
                view.findViewById<TextView>(R.id.badge_willow_status)?.text = first.status
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appsListener?.remove()
    }
}
