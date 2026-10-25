package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class OwnerDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Bottom Navigation
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.HOME)

        val mainActivity = activity as? MainActivity

        // Notifications
        view.findViewById<View>(R.id.iv_owner_notifications)?.setOnClickListener {
            Toast.makeText(requireContext(), "You're all caught up with notifications.", Toast.LENGTH_SHORT).show()
        }

        // Manage all requests link -> B05 Manage Bookings
        view.findViewById<TextView>(R.id.tv_manage_all_requests)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = true)
        }

        // Click on Metric cards -> B05 Manage Bookings
        view.findViewById<View>(R.id.card_metric_today)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.card_metric_pending)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.card_metric_pending)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = true)
        }

        // Rating Card -> B13 Customer Reviews
        view.findViewById<View>(R.id.card_owner_reviews)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerCustomerReviewsFragment(), addToBackStack = true)
        }

        // Trust Score Card -> B11 Trust Score Details
        view.findViewById<View>(R.id.card_owner_trust_score)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerTrustScoreDetailsFragment(), addToBackStack = true)
        }

        // Request Card Click -> B06 Booking Details
        view.findViewById<View>(R.id.card_recent_request_1)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerBookingDetailsFragment(), addToBackStack = true)
        }

        // Confirm Request
        view.findViewById<Button>(R.id.btn_confirm_request)?.setOnClickListener {
            Toast.makeText(requireContext(), "Appointment BB-24109 confirmed for Liam Brooks.", Toast.LENGTH_SHORT).show()
            view.findViewById<Button>(R.id.btn_confirm_request)?.isEnabled = false
            view.findViewById<Button>(R.id.btn_confirm_request)?.text = "Confirmed"
            view.findViewById<Button>(R.id.btn_reject_request)?.visibility = View.GONE
        }

        // Reject Request with Specimen Confirmation Dialog
        view.findViewById<Button>(R.id.btn_reject_request)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Reject request BB-24109?",
                message = "Reject this request? The sample reason is shown to the account owner.",
                confirmButtonText = "Confirm Rejection",
                cancelButtonText = "Keep Reviewing",
                isDestructive = true,
                onConfirm = {
                    Toast.makeText(requireContext(), "Request rejected.", Toast.LENGTH_SHORT).show()
                    view.findViewById<View>(R.id.card_recent_request_1)?.visibility = View.GONE
                }
            )
        }
    }
}
