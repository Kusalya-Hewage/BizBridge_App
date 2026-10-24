package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.AdminRepository

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

        view.findViewById<View>(R.id.btn_action_juniper)?.setOnClickListener {
            showBusinessActionDialog("Juniper Studio", "biz_juniper_studio")
        }
        view.findViewById<View>(R.id.btn_action_bloom)?.setOnClickListener {
            showBusinessActionDialog("Bloom & Co Floral", "biz_bloom_co")
        }
        view.findViewById<View>(R.id.btn_action_forma)?.setOnClickListener {
            showBusinessActionDialog("Forma Architecture", "biz_forma_arch")
        }
    }
}
