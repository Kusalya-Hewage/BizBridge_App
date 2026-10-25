package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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

        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(AdminAnnouncementsFragment(), addToBackStack = true)
        }

        val card1 = view.findViewById<View>(R.id.card_willow_wellness)
        val card2 = view.findViewById<View>(R.id.card_linden_beauty)
        val card3 = view.findViewById<View>(R.id.card_fixwell_repairs)

        // Listen to live business applications from Firestore
        appsListener = AdminRepository.listenToApplications { list ->
            if (!isAdded) return@listenToApplications
            allApplications = list
            val pendingCount = list.count { it.status == "PENDING" }
            view.findViewById<TextView>(R.id.tab_pending)?.text = "Pending  $pendingCount"

            val app0 = list.getOrNull(0)
            val app1 = list.getOrNull(1)
            val app2 = list.getOrNull(2)

            if (app0 != null) {
                card1?.visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.tv_willow_name)?.text = app0.businessName
                view.findViewById<TextView>(R.id.tv_willow_owner)?.text = app0.ownerName
                view.findViewById<TextView>(R.id.tv_willow_category)?.text = "${app0.category} · ${app0.address}"
                view.findViewById<TextView>(R.id.badge_willow_status)?.text = app0.status
                val openApp0 = View.OnClickListener {
                    mainActivity?.navigateTo(AdminApplicationReviewFragment.newInstance(app0.id), addToBackStack = true)
                }
                card1?.setOnClickListener(openApp0)
                view.findViewById<View>(R.id.btn_open_app_willow)?.setOnClickListener(openApp0)
            } else {
                card1?.visibility = View.GONE
            }

            if (app1 != null) {
                card2?.visibility = View.VISIBLE
                val openApp1 = View.OnClickListener {
                    mainActivity?.navigateTo(AdminApplicationReviewFragment.newInstance(app1.id), addToBackStack = true)
                }
                card2?.setOnClickListener(openApp1)
                view.findViewById<View>(R.id.btn_open_app_linden)?.setOnClickListener(openApp1)
            } else {
                card2?.visibility = View.GONE
            }

            if (app2 != null) {
                card3?.visibility = View.VISIBLE
                val openApp2 = View.OnClickListener {
                    mainActivity?.navigateTo(AdminApplicationReviewFragment.newInstance(app2.id), addToBackStack = true)
                }
                card3?.setOnClickListener(openApp2)
                view.findViewById<View>(R.id.btn_open_app_fixwell)?.setOnClickListener(openApp2)
            } else {
                card3?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appsListener?.remove()
    }
}
