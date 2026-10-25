package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Promotion
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.PromotionRepository

class OwnerCreatePromotionFragment : Fragment() {

    private var isEditMode: Boolean = false
    private var currentBusinessId: String = "biz_juniper_studio"

    companion object {
        fun newInstance(isEditMode: Boolean = false): OwnerCreatePromotionFragment {
            val fragment = OwnerCreatePromotionFragment()
            val args = Bundle()
            args.putBoolean("isEditMode", isEditMode)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isEditMode = arguments?.getBoolean("isEditMode", false) ?: false
        currentBusinessId = BusinessRepository.getOwnerBusinessId()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_create_promotion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tv_create_promo_title)?.text =
            if (isEditMode) "Edit promotion" else "Create promotion"

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_create_promo)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Image picker
        view.findViewById<View>(R.id.box_upload_promo_image)?.setOnClickListener {
            Toast.makeText(requireContext(), "Banner image picker ready via Storage.", Toast.LENGTH_SHORT).show()
        }

        val etTitle = view.findViewById<EditText>(R.id.et_promo_title)
        val etDesc = view.findViewById<EditText>(R.id.et_promo_description)
        val etOffer = view.findViewById<EditText>(R.id.et_promo_offer_details)
        val btnPublish = view.findViewById<Button>(R.id.btn_publish_promotion)
        val btnDraft = view.findViewById<Button>(R.id.btn_save_promo_draft)

        if (!isEditMode) {
            etTitle?.setText("")
            etDesc?.setText("")
            etOffer?.setText("")
        }

        fun savePromotion(status: String) {
            val title = etTitle?.text?.toString()?.trim().orEmpty()
            val desc = etDesc?.text?.toString()?.trim().orEmpty()
            val offer = etOffer?.text?.toString()?.trim().orEmpty()

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Promotion title is required.", Toast.LENGTH_SHORT).show()
                return
            }

            btnPublish?.isEnabled = false
            btnPublish?.text = if (status == "ACTIVE") "Publishing…" else "Saving Draft…"

            val promotion = Promotion(
                id = if (isEditMode) "promo_season_finish" else "",
                businessId = currentBusinessId,
                businessName = "Juniper Studio",
                title = title,
                description = desc,
                discountText = offer.ifEmpty { "Limited offer" },
                status = status
            )

            PromotionRepository.createOrUpdatePromotion(
                promotion = promotion,
                onSuccess = {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Promotion $status in Firestore.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                },
                onFailure = { e ->
                    if (isAdded) {
                        btnPublish?.isEnabled = true
                        btnPublish?.text = "Publish to Listing"
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to save promotion.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        btnDraft?.setOnClickListener { savePromotion("DRAFT") }
        btnPublish?.setOnClickListener { savePromotion("ACTIVE") }
    }
}
