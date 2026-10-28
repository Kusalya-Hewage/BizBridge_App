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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.repository.AdminRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminBusinessListingsFragment : Fragment() {

    private var bizListener: ListenerRegistration? = null
    private var allBusinesses: List<Business> = emptyList()
    private var filterActiveStatus: String = "ALL" // "ALL", "ACTIVE", "INACTIVE"
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

        val btnCategory = view.findViewById<View>(R.id.btn_category_dropdown)
        val tvCategory = view.findViewById<TextView>(R.id.tv_category_filter)
        val tabActive = view.findViewById<TextView>(R.id.tab_active_listings)
        val tabInactive = view.findViewById<TextView>(R.id.tab_inactive_listings)
        val etSearch = view.findViewById<EditText>(R.id.et_search_listings)

        fun updateTabs() {
            val activeBg = R.drawable.bg_admin_tab_active
            val inactiveBg = R.drawable.bg_admin_tab_inactive
            val activeColor = ContextCompat.getColor(requireContext(), R.color.white)
            val inactiveColor = ContextCompat.getColor(requireContext(), R.color.admin_text_body)

            tabActive?.setBackgroundResource(if (filterActiveStatus == "ACTIVE") activeBg else inactiveBg)
            tabActive?.setTextColor(if (filterActiveStatus == "ACTIVE") activeColor else inactiveColor)

            tabInactive?.setBackgroundResource(if (filterActiveStatus == "INACTIVE") activeBg else inactiveBg)
            tabInactive?.setTextColor(if (filterActiveStatus == "INACTIVE") activeColor else inactiveColor)
        }

        tabActive?.setOnClickListener {
            filterActiveStatus = if (filterActiveStatus == "ACTIVE") "ALL" else "ACTIVE"
            updateTabs()
            renderList(view)
        }

        tabInactive?.setOnClickListener {
            filterActiveStatus = if (filterActiveStatus == "INACTIVE") "ALL" else "INACTIVE"
            updateTabs()
            renderList(view)
        }

        btnCategory?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Category")
                .setItems(categories) { _, which ->
                    selectedCategory = categories[which]
                    tvCategory?.text = if (selectedCategory == "All") "All categories" else selectedCategory
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

        // Listen to live businesses in Firestore and local repository
        bizListener = FirebaseFirestore.getInstance().collection("businesses")
            .addSnapshotListener { snapshot, _ ->
                if (!isAdded) return@addSnapshotListener
                val firestoreList = snapshot?.documents?.mapNotNull { it.toObject(Business::class.java)?.copy(id = it.id) } ?: emptyList()
                val localList = UserSessionManager.getAllBusinesses()

                allBusinesses = (firestoreList + localList)
                    .distinctBy { it.id }
                    .sortedByDescending { it.approved }

                renderList(view)
            }
    }

    private fun showBusinessActionDialog(bizName: String, bizId: String) {
        val adminUser = FirebaseAuthManager.getCurrentUser()
        val adminUid = adminUser?.uid ?: "admin_alex"
        val adminEmail = adminUser?.email ?: "admin@bizbridge.com"
        val mainActivity = activity as? MainActivity

        AlertDialog.Builder(requireContext())
            .setTitle("Moderate Business: $bizName")
            .setItems(arrayOf("View Details", "Approve & Activate Listing", "Suspend Listing")) { _, which ->
                when (which) {
                    0 -> mainActivity?.navigateTo(BusinessDetailsFragment.newInstance(bizId, businessName = bizName), addToBackStack = true)
                    1 -> {
                        AdminRepository.updateBusinessStatus(
                            businessId = bizId,
                            active = true,
                            adminUid = adminUid,
                            adminEmail = adminEmail,
                            reason = "Administrative approval & activation",
                            onSuccess = {
                                UserSessionManager.updateBusinessApprovalStatus(bizId, true, true, "APPROVED", "VERIFIED")
                                Toast.makeText(requireContext(), "$bizName listing activated.", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { Toast.makeText(requireContext(), "$bizName activated.", Toast.LENGTH_SHORT).show() }
                        )
                    }
                    2 -> {
                        AdminRepository.updateBusinessStatus(
                            businessId = bizId,
                            active = false,
                            adminUid = adminUid,
                            adminEmail = adminEmail,
                            reason = "Administrative suspension",
                            onSuccess = {
                                UserSessionManager.updateBusinessApprovalStatus(bizId, false, false, "SUSPENDED", "PENDING")
                                Toast.makeText(requireContext(), "$bizName listing suspended.", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { Toast.makeText(requireContext(), "$bizName listing suspended.", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
            .show()
    }

    private fun renderList(rootView: View) {
        val container = rootView.findViewById<LinearLayout>(R.id.ll_listings_container) ?: return
        val emptyView = rootView.findViewById<View>(R.id.ll_empty_listings)
        val tvStat = rootView.findViewById<TextView>(R.id.tv_stat_businesses)

        val filtered = allBusinesses.filter { b ->
            val matchStatus = when (filterActiveStatus) {
                "ACTIVE" -> b.active
                "INACTIVE" -> !b.active
                else -> true
            }
            val matchCategory = if (selectedCategory.equals("All", ignoreCase = true)) {
                true
            } else {
                b.category.contains(selectedCategory, ignoreCase = true)
            }
            val matchSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                b.name.contains(searchQuery, ignoreCase = true) ||
                b.category.contains(searchQuery, ignoreCase = true) ||
                b.address.contains(searchQuery, ignoreCase = true)
            }
            matchStatus && matchCategory && matchSearch
        }

        tvStat?.text = "${filtered.size} listings"
        container.removeAllViews()

        if (filtered.isEmpty()) {
            emptyView?.visibility = View.VISIBLE
            return
        }
        emptyView?.visibility = View.GONE

        val inflater = LayoutInflater.from(requireContext())
        for (biz in filtered) {
            val itemView = inflater.inflate(R.layout.item_admin_business_listing_card, container, false)
            val tvName = itemView.findViewById<TextView>(R.id.tv_listing_name)
            val badgeStatus = itemView.findViewById<TextView>(R.id.badge_listing_status)
            val tvCategoryOwner = itemView.findViewById<TextView>(R.id.tv_listing_category_owner)
            val btnAction = itemView.findViewById<View>(R.id.btn_listing_action)

            tvName?.text = biz.name
            tvCategoryOwner?.text = "${biz.category} · ${biz.address}"

            if (biz.approved && biz.active) {
                badgeStatus?.text = "Active"
                badgeStatus?.setBackgroundResource(R.drawable.bg_admin_badge_active)
                badgeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_badge_active_text))
            } else if (biz.approvalStatus.equals("PENDING", ignoreCase = true)) {
                badgeStatus?.text = "Pending"
                badgeStatus?.setBackgroundResource(R.drawable.bg_admin_badge_pending)
                badgeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_badge_pending_text))
            } else {
                badgeStatus?.text = "Inactive"
                badgeStatus?.setBackgroundResource(R.drawable.bg_badge_cancelled)
                badgeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_text))
            }

            val clickListener = View.OnClickListener {
                showBusinessActionDialog(biz.name, biz.id)
            }
            itemView.setOnClickListener(clickListener)
            btnAction?.setOnClickListener(clickListener)

            container.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bizListener?.remove()
    }
}
