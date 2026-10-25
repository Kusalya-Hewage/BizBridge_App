package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class OwnerPromotionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_promotions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as? MainActivity

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_promotions)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Tabs
        val tabActive = view.findViewById<TextView>(R.id.tab_promo_active)
        val tabScheduled = view.findViewById<TextView>(R.id.tab_promo_scheduled)
        val tabExpired = view.findViewById<TextView>(R.id.tab_promo_expired)

        val selectTab = { active: TextView, others: List<TextView> ->
            active.setBackgroundResource(R.drawable.bg_chip_selected)
            active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            others.forEach {
                it.setBackgroundResource(R.drawable.bg_chip_default)
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }

        tabActive?.setOnClickListener { selectTab(tabActive, listOfNotNull(tabScheduled, tabExpired)) }
        tabScheduled?.setOnClickListener { selectTab(tabScheduled, listOfNotNull(tabActive, tabExpired)) }
        tabExpired?.setOnClickListener { selectTab(tabExpired, listOfNotNull(tabActive, tabScheduled)) }

        // Create / Edit Promotion -> B09
        view.findViewById<Button>(R.id.btn_create_promotion)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerCreatePromotionFragment.newInstance(isEditMode = false), addToBackStack = true)
        }
        view.findViewById<View>(R.id.btn_edit_promotion)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerCreatePromotionFragment.newInstance(isEditMode = true), addToBackStack = true)
        }

        // Deactivate promotion with Specimen 04 Confirmation Dialog
        view.findViewById<View>(R.id.btn_deactivate_promotion)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Deactivate promotion?",
                message = "Deactivate A fresh season finish? It will no longer be available on the listing.",
                confirmButtonText = "Deactivate Promotion",
                cancelButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    view.findViewById<TextView>(R.id.badge_promo_active)?.text = "Inactive"
                    view.findViewById<TextView>(R.id.badge_promo_active)?.setBackgroundResource(R.drawable.bg_badge_cancelled)
                    view.findViewById<TextView>(R.id.badge_promo_active)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_text))
                    Toast.makeText(requireContext(), "Promotion deactivated.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
