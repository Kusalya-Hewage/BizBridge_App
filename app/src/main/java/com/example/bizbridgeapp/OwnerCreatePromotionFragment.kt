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

class OwnerCreatePromotionFragment : Fragment() {

    private var isEditMode: Boolean = false

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
            Toast.makeText(requireContext(), "Select promotion banner image (Demonstration).", Toast.LENGTH_SHORT).show()
        }

        val etTitle = view.findViewById<EditText>(R.id.et_promo_title)
        val etDesc = view.findViewById<EditText>(R.id.et_promo_description)
        val etOffer = view.findViewById<EditText>(R.id.et_promo_offer_details)
        val etStart = view.findViewById<EditText>(R.id.et_promo_start_date)
        val etEnd = view.findViewById<EditText>(R.id.et_promo_end_date)
        val tvEndDateError = view.findViewById<TextView>(R.id.tv_promo_end_date_error)

        if (!isEditMode) {
            etTitle?.setText("")
            etDesc?.setText("")
            etOffer?.setText("")
        }

        // Save Draft
        view.findViewById<Button>(R.id.btn_save_promo_draft)?.setOnClickListener {
            Toast.makeText(requireContext(), "Draft saved successfully.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        // Publish Promotion
        val btnPublish = view.findViewById<Button>(R.id.btn_publish_promotion)
        btnPublish?.setOnClickListener {
            val title = etTitle?.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Promotion title is required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnPublish.isEnabled = false
            btnPublish.text = "Publishing…"

            btnPublish.postDelayed({
                if (isAdded) {
                    Toast.makeText(requireContext(), "Promotion published to listing successfully.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }, 600)
        }
    }
}
