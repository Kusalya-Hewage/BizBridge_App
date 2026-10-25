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
import com.example.bizbridgeapp.model.Promotion
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.PromotionRepository
import com.google.firebase.firestore.ListenerRegistration

class OwnerPromotionsFragment : Fragment() {

    private var promoListener: ListenerRegistration? = null
    private var allPromotions: List<Promotion> = emptyList()
    private var currentBusinessId: String = "biz_juniper_studio"
    private var activeFilter: String = "ACTIVE"

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
        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_promotions)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Tabs
        val tabActive = view.findViewById<TextView>(R.id.tab_promo_active)
        val tabScheduled = view.findViewById<TextView>(R.id.tab_promo_scheduled)
        val tabExpired = view.findViewById<TextView>(R.id.tab_promo_expired)

        val selectTab = { active: TextView, others: List<TextView>, filter: String ->
            activeFilter = filter
            active.setBackgroundResource(R.drawable.bg_chip_selected)
            active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            others.forEach {
                it.setBackgroundResource(R.drawable.bg_chip_default)
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            renderPromotions(view)
        }

        tabActive?.setOnClickListener { selectTab(tabActive, listOfNotNull(tabScheduled, tabExpired), "ACTIVE") }
        tabScheduled?.setOnClickListener { selectTab(tabScheduled, listOfNotNull(tabActive, tabExpired), "SCHEDULED") }
        tabExpired?.setOnClickListener { selectTab(tabExpired, listOfNotNull(tabActive, tabScheduled), "EXPIRED") }

        // Create Promotion -> B09
        view.findViewById<Button>(R.id.btn_create_promotion)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerCreatePromotionFragment.newInstance(isEditMode = false), addToBackStack = true)
        }

        // Real-time listener for Promotions
        promoListener = PromotionRepository.listenToPromotionsForBusiness(currentBusinessId) { list ->
            if (!isAdded) return@listenToPromotionsForBusiness
            allPromotions = list
            renderPromotions(view)
        }
    }

    private fun renderPromotions(rootView: View) {
        val promoCard = rootView.findViewById<View>(R.id.card_promotion_sample) ?: return
        val activePromo = allPromotions.firstOrNull {
            if (activeFilter == "ACTIVE") it.status == "ACTIVE"
            else if (activeFilter == "SCHEDULED") it.status == "SCHEDULED" || it.status == "DRAFT"
            else it.status == "EXPIRED" || it.status == "PAUSED"
        } ?: allPromotions.firstOrNull()

        val mainActivity = activity as? MainActivity

        if (activePromo != null) {
            promoCard.visibility = View.VISIBLE
            val badgeStatus = rootView.findViewById<TextView>(R.id.badge_promo_active)

            if (activePromo.status == "ACTIVE") {
                badgeStatus?.text = "Active"
                badgeStatus?.setBackgroundResource(R.drawable.bg_tag_verified)
                badgeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
            } else {
                badgeStatus?.text = activePromo.status
                badgeStatus?.setBackgroundResource(R.drawable.bg_badge_cancelled)
                badgeStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_text))
            }

            // Edit Promotion
            rootView.findViewById<View>(R.id.btn_edit_promotion)?.setOnClickListener {
                mainActivity?.navigateTo(OwnerCreatePromotionFragment.newInstance(isEditMode = true), addToBackStack = true)
            }

            // Deactivate promotion with confirmation dialog
            rootView.findViewById<View>(R.id.btn_deactivate_promotion)?.setOnClickListener {
                OwnerDialogHelper.showConfirmationDialog(
                    context = requireContext(),
                    title = "Deactivate promotion?",
                    message = "Deactivate '${activePromo.title}'? It will no longer be visible on your public listing.",
                    confirmButtonText = "Deactivate Promotion",
                    cancelButtonText = "Cancel",
                    isDestructive = true,
                    onConfirm = {
                        PromotionRepository.togglePromotionStatus(
                            promotionId = activePromo.id,
                            status = "PAUSED",
                            onSuccess = {
                                if (isAdded) {
                                    Toast.makeText(requireContext(), "Promotion deactivated.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onFailure = { e ->
                                if (isAdded) {
                                    Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to deactivate.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                )
            }
        } else {
            promoCard.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        promoListener?.remove()
    }
}
