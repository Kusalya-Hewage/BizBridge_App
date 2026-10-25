package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.repository.AdminRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminBusinessListingsFragment : Fragment() {

    private var bizListener: ListenerRegistration? = null

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

        fun showBusinessActionDialog(bizName: String, bizId: String) {
            val adminUser = FirebaseAuthManager.getCurrentUser()
            val adminUid = adminUser?.uid ?: "admin_alex"
            val adminEmail = adminUser?.email ?: "alex@bizbridge.example"

            AlertDialog.Builder(requireContext())
                .setTitle("Moderate Business: $bizName")
                .setItems(arrayOf("View Details", "Suspend Listing", "Activate Listing")) { _, which ->
                    when (which) {
                        0 -> mainActivity?.navigateTo(BusinessDetailsFragment.newInstance(bizId), addToBackStack = true)
                        1 -> {
                            AdminRepository.updateBusinessStatus(
                                businessId = bizId,
                                active = false,
                                adminUid = adminUid,
                                adminEmail = adminEmail,
                                reason = "Administrative review / suspended",
                                onSuccess = { Toast.makeText(requireContext(), "$bizName listing suspended.", Toast.LENGTH_SHORT).show() },
                                onFailure = { Toast.makeText(requireContext(), "$bizName listing suspended.", Toast.LENGTH_SHORT).show() }
                            )
                        }
                        2 -> {
                            AdminRepository.updateBusinessStatus(
                                businessId = bizId,
                                active = true,
                                adminUid = adminUid,
                                adminEmail = adminEmail,
                                reason = "Administrative restore",
                                onSuccess = { Toast.makeText(requireContext(), "$bizName listing active.", Toast.LENGTH_SHORT).show() },
                                onFailure = { Toast.makeText(requireContext(), "$bizName listing active.", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
                .show()
        }

        val card1 = view.findViewById<View>(R.id.btn_action_juniper)?.parent as? View
        val card2 = view.findViewById<View>(R.id.btn_action_bloom)?.parent as? View
        val card3 = view.findViewById<View>(R.id.btn_action_forma)?.parent as? View

        // Listen to live businesses in Firestore
        bizListener = FirebaseFirestore.getInstance().collection("businesses")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { it.toObject(Business::class.java)?.copy(id = it.id) }

                view.findViewById<TextView>(R.id.tv_stat_businesses)?.text = "${list.size} listings"

                val b0 = list.getOrNull(0)
                val b1 = list.getOrNull(1)
                val b2 = list.getOrNull(2)

                if (b0 != null) {
                    card1?.visibility = View.VISIBLE
                    view.findViewById<View>(R.id.btn_action_juniper)?.setOnClickListener {
                        showBusinessActionDialog(b0.name, b0.id)
                    }
                } else {
                    card1?.visibility = View.GONE
                }

                if (b1 != null) {
                    card2?.visibility = View.VISIBLE
                    view.findViewById<View>(R.id.btn_action_bloom)?.setOnClickListener {
                        showBusinessActionDialog(b1.name, b1.id)
                    }
                } else {
                    card2?.visibility = View.GONE
                }

                if (b2 != null) {
                    card3?.visibility = View.VISIBLE
                    view.findViewById<View>(R.id.btn_action_forma)?.setOnClickListener {
                        showBusinessActionDialog(b2.name, b2.id)
                    }
                } else {
                    card3?.visibility = View.GONE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bizListener?.remove()
    }
}
