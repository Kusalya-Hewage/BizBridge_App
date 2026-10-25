package com.example.bizbridgeapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.BusinessApplication
import com.example.bizbridgeapp.repository.AdminRepository
import com.google.firebase.firestore.ListenerRegistration

class AdminApplicationsFragment : Fragment() {

    private var appsListener: ListenerRegistration? = null
    private var allApplications: List<BusinessApplication> = emptyList()
    private var currentFilterStatus: String = "PENDING" // "PENDING", "APPROVED", "REJECTED", "ALL"
    private var selectedCategory: String = "All"
    private var searchQuery: String = ""

    private val categories = arrayOf(
        "All",
        "Hair salon",
        "Beauty & Spa",
        "Fitness",
        "Repair & Maintenance",
        "Wellness",
        "Education",
        "Other"
    )

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

        val tabPending = view.findViewById<TextView>(R.id.tab_pending)
        val tabApproved = view.findViewById<TextView>(R.id.tab_approved)
        val tabRejected = view.findViewById<TextView>(R.id.tab_rejected)
        val btnCategory = view.findViewById<View>(R.id.btn_category_dropdown)
        val etSearch = view.findViewById<EditText>(R.id.et_search_apps)

        fun updateTabStyles() {
            val activeBg = R.drawable.bg_admin_tab_active
            val inactiveBg = R.drawable.bg_admin_tab_inactive
            val activeColor = ContextCompat.getColor(requireContext(), R.color.white)
            val inactiveColor = ContextCompat.getColor(requireContext(), R.color.admin_text_body)

            tabPending?.setBackgroundResource(if (currentFilterStatus == "PENDING") activeBg else inactiveBg)
            tabPending?.setTextColor(if (currentFilterStatus == "PENDING") activeColor else inactiveColor)

            tabApproved?.setBackgroundResource(if (currentFilterStatus == "APPROVED") activeBg else inactiveBg)
            tabApproved?.setTextColor(if (currentFilterStatus == "APPROVED") activeColor else inactiveColor)

            tabRejected?.setBackgroundResource(if (currentFilterStatus == "REJECTED") activeBg else inactiveBg)
            tabRejected?.setTextColor(if (currentFilterStatus == "REJECTED") activeColor else inactiveColor)
        }

        tabPending?.setOnClickListener {
            currentFilterStatus = "PENDING"
            updateTabStyles()
            renderList(view)
        }

        tabApproved?.setOnClickListener {
            currentFilterStatus = "APPROVED"
            updateTabStyles()
            renderList(view)
        }

        tabRejected?.setOnClickListener {
            currentFilterStatus = "REJECTED"
            updateTabStyles()
            renderList(view)
        }

        btnCategory?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Filter by Category")
                .setItems(categories) { _, which ->
                    selectedCategory = categories[which]
                    renderList(view)
                }
                .show()
        }

        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                renderList(view)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Listen to live business applications
        appsListener = AdminRepository.listenToApplications { list ->
            if (!isAdded) return@listenToApplications
            allApplications = list
            val pendingCount = list.count { it.status.equals("PENDING", ignoreCase = true) }
            tabPending?.text = "Pending  $pendingCount"

            renderList(view)
        }
    }

    private fun renderList(rootView: View) {
        val container = rootView.findViewById<LinearLayout>(R.id.ll_applications_container) ?: return
        val emptyView = rootView.findViewById<View>(R.id.ll_empty_applications)

        val filtered = allApplications.filter { app ->
            val matchStatus = when (currentFilterStatus) {
                "PENDING" -> app.status.equals("PENDING", ignoreCase = true)
                "APPROVED" -> app.status.equals("APPROVED", ignoreCase = true)
                "REJECTED" -> app.status.equals("REJECTED", ignoreCase = true)
                else -> true
            }
            val matchCategory = if (selectedCategory.equals("All", ignoreCase = true)) {
                true
            } else {
                app.category.contains(selectedCategory, ignoreCase = true)
            }
            val matchSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                app.businessName.contains(searchQuery, ignoreCase = true) ||
                app.ownerName.contains(searchQuery, ignoreCase = true)
            }
            matchStatus && matchCategory && matchSearch
        }

        container.removeAllViews()

        if (filtered.isEmpty()) {
            emptyView?.visibility = View.VISIBLE
            return
        }
        emptyView?.visibility = View.GONE

        val inflater = LayoutInflater.from(requireContext())
        val mainActivity = activity as? MainActivity

        for (app in filtered) {
            val itemView = inflater.inflate(R.layout.item_admin_application_card, container, false)
            val tvName = itemView.findViewById<TextView>(R.id.tv_app_business_name)
            val badgeStatus = itemView.findViewById<TextView>(R.id.badge_app_status)
            val tvOwner = itemView.findViewById<TextView>(R.id.tv_app_owner_name)
            val tvCategory = itemView.findViewById<TextView>(R.id.tv_app_category_location)
            val btnOpen = itemView.findViewById<View>(R.id.btn_open_application)

            tvName?.text = app.businessName
            tvOwner?.text = app.ownerName.ifEmpty { "Business Owner" }
            tvCategory?.text = "${app.category} · ${app.address}"

            when (app.status.uppercase()) {
                "APPROVED" -> {
                    badgeStatus?.text = "Approved"
                    badgeStatus?.setBackgroundResource(R.drawable.bg_admin_badge_active)
                    badgeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_badge_active_text))
                }
                "REJECTED" -> {
                    badgeStatus?.text = "Rejected"
                    badgeStatus?.setBackgroundResource(R.drawable.bg_badge_cancelled)
                    badgeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_text))
                }
                else -> {
                    badgeStatus?.text = "Pending"
                    badgeStatus?.setBackgroundResource(R.drawable.bg_admin_badge_pending)
                    badgeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_badge_pending_text))
                }
            }

            val openAction = View.OnClickListener {
                mainActivity?.navigateTo(AdminApplicationReviewFragment.newInstance(app.id), addToBackStack = true)
            }
            itemView.setOnClickListener(openAction)
            btnOpen?.setOnClickListener(openAction)

            container.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appsListener?.remove()
    }
}
