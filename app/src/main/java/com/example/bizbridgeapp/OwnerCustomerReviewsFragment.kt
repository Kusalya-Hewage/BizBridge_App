package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

class OwnerCustomerReviewsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_customer_reviews, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_reviews)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Bottom Navigation (can be viewed from Home or Insights)
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.HOME)

        // Reply actions
        view.findViewById<View>(R.id.btn_reply_maya)?.setOnClickListener {
            Toast.makeText(requireContext(), "Write a reply to Maya Chen (Demonstration).", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btn_reply_jordan)?.setOnClickListener {
            Toast.makeText(requireContext(), "Write a reply to Jordan Lee (Demonstration).", Toast.LENGTH_SHORT).show()
        }

        // Report action with Specimen 04 Confirmation Dialog
        view.findViewById<View>(R.id.btn_report_maya)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Remove review confirmation",
                message = "Permanently remove the reported Maya Chen review? Use Hide for a reversible moderation decision.",
                confirmButtonText = "Remove Review",
                cancelButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    view.findViewById<View>(R.id.card_review_maya)?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Review reported for moderation.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        view.findViewById<View>(R.id.btn_report_jordan)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Remove review confirmation",
                message = "Permanently remove the reported Jordan Lee review? Use Hide for a reversible moderation decision.",
                confirmButtonText = "Remove Review",
                cancelButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    view.findViewById<View>(R.id.card_review_jordan)?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Review reported for moderation.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
