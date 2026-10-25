package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class OwnerManageBookingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_manage_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bottom Navigation
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.BOOKINGS)

        val mainActivity = activity as? MainActivity

        // Tabs
        val tabPending = view.findViewById<TextView>(R.id.tab_pending)
        val tabConfirmed = view.findViewById<TextView>(R.id.tab_confirmed)
        val tabCompleted = view.findViewById<TextView>(R.id.tab_completed)
        val tabCancelled = view.findViewById<TextView>(R.id.tab_cancelled)

        val selectTab = { active: TextView, others: List<TextView> ->
            active.setBackgroundResource(R.drawable.bg_chip_selected)
            active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            others.forEach {
                it.setBackgroundResource(R.drawable.bg_chip_default)
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }

        tabPending?.setOnClickListener { selectTab(tabPending, listOfNotNull(tabConfirmed, tabCompleted, tabCancelled)) }
        tabConfirmed?.setOnClickListener {
            selectTab(tabConfirmed, listOfNotNull(tabPending, tabCompleted, tabCancelled))
            Toast.makeText(requireContext(), "Showing confirmed bookings.", Toast.LENGTH_SHORT).show()
        }
        tabCompleted?.setOnClickListener {
            selectTab(tabCompleted, listOfNotNull(tabPending, tabConfirmed, tabCancelled))
            Toast.makeText(requireContext(), "Showing completed bookings.", Toast.LENGTH_SHORT).show()
        }
        tabCancelled?.setOnClickListener {
            selectTab(tabCancelled, listOfNotNull(tabPending, tabConfirmed, tabCompleted))
            Toast.makeText(requireContext(), "Showing cancelled bookings.", Toast.LENGTH_SHORT).show()
        }

        // Open Booking Details B06 on card clicks
        view.findViewById<View>(R.id.card_booking_liam)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerBookingDetailsFragment.newInstance(
                bookingId = "BB-24109",
                customerName = "Liam Brooks",
                customerEmail = "liam.brooks@example.com",
                customerPhone = "+1 (415) 555-0199",
                serviceName = "Signature haircut · $45 · 45 min · Elena Rivera",
                dateTime = "Sat, October 24, 2026, 11:00–11:45 AM · Pacific Time",
                customerNote = "Looking forward to refreshing my hairstyle before the weekend."
            ), addToBackStack = true)
        }

        view.findViewById<View>(R.id.card_booking_maya)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerBookingDetailsFragment.newInstance(
                bookingId = "BB-24108",
                customerName = "Maya Chen",
                customerEmail = "maya.chen@example.com",
                customerPhone = "+1 (415) 555-0182",
                serviceName = "Color refresh · $65 · 50 min · Elena Rivera",
                dateTime = "Sat, October 24, 2026, 2:00–2:50 PM · Pacific Time",
                customerNote = "A shoulder-length cut with soft layers, please."
            ), addToBackStack = true)
        }

        view.findViewById<View>(R.id.card_booking_ava)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerBookingDetailsFragment.newInstance(
                bookingId = "BB-24110",
                customerName = "Ava Patel",
                customerEmail = "ava.patel@example.com",
                customerPhone = "+1 (415) 555-0144",
                serviceName = "Signature haircut · $45 · 45 min · Elena Rivera",
                dateTime = "Mon, October 26, 2026, 10:00–10:45 AM · Pacific Time",
                customerNote = "Standard trim and styling please."
            ), addToBackStack = true)
        }

        // Confirm buttons
        view.findViewById<Button>(R.id.btn_confirm_liam)?.setOnClickListener {
            Toast.makeText(requireContext(), "BB-24109 confirmed.", Toast.LENGTH_SHORT).show()
            view.findViewById<Button>(R.id.btn_confirm_liam)?.isEnabled = false
            view.findViewById<Button>(R.id.btn_confirm_liam)?.text = "Confirmed"
            view.findViewById<Button>(R.id.btn_reject_liam)?.visibility = View.GONE
        }

        view.findViewById<Button>(R.id.btn_confirm_maya)?.setOnClickListener {
            Toast.makeText(requireContext(), "BB-24108 confirmed.", Toast.LENGTH_SHORT).show()
            view.findViewById<Button>(R.id.btn_confirm_maya)?.isEnabled = false
            view.findViewById<Button>(R.id.btn_confirm_maya)?.text = "Confirmed"
            view.findViewById<Button>(R.id.btn_reject_maya)?.visibility = View.GONE
        }

        view.findViewById<Button>(R.id.btn_confirm_ava)?.setOnClickListener {
            Toast.makeText(requireContext(), "BB-24110 confirmed.", Toast.LENGTH_SHORT).show()
            view.findViewById<Button>(R.id.btn_confirm_ava)?.isEnabled = false
            view.findViewById<Button>(R.id.btn_confirm_ava)?.text = "Confirmed"
            view.findViewById<Button>(R.id.btn_reject_ava)?.visibility = View.GONE
        }

        // Reject buttons with Specimen 04 Confirmation Dialog
        view.findViewById<Button>(R.id.btn_reject_liam)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Reject request BB-24109?",
                message = "Reject this request? The sample reason is shown to the account owner.",
                confirmButtonText = "Confirm Rejection",
                isDestructive = true,
                onConfirm = {
                    view.findViewById<View>(R.id.card_booking_liam)?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Request BB-24109 rejected.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        view.findViewById<Button>(R.id.btn_reject_maya)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Reject request BB-24108?",
                message = "Reject this request? The sample reason is shown to the account owner.",
                confirmButtonText = "Confirm Rejection",
                isDestructive = true,
                onConfirm = {
                    view.findViewById<View>(R.id.card_booking_maya)?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Request BB-24108 rejected.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        view.findViewById<Button>(R.id.btn_reject_ava)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Reject request BB-24110?",
                message = "Reject this request? The sample reason is shown to the account owner.",
                confirmButtonText = "Confirm Rejection",
                isDestructive = true,
                onConfirm = {
                    view.findViewById<View>(R.id.card_booking_ava)?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Request BB-24110 rejected.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
