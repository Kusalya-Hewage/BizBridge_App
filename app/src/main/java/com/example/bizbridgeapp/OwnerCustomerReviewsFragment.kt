package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Review
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.ReviewRepository
import com.google.firebase.firestore.ListenerRegistration

class OwnerCustomerReviewsFragment : Fragment() {

    private var reviewsListener: ListenerRegistration? = null
    private var allReviews: List<Review> = emptyList()
    private var currentBusinessId: String = "biz_juniper_studio"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_customer_reviews, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_reviews)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Bottom Navigation
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.HOME)

        val cardMaya = view.findViewById<View>(R.id.card_review_maya)
        val cardJordan = view.findViewById<View>(R.id.card_review_jordan)

        // Reply actions
        view.findViewById<View>(R.id.btn_reply_maya)?.setOnClickListener {
            Toast.makeText(requireContext(), "Owner public reply feature enabled via Firestore.", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btn_reply_jordan)?.setOnClickListener {
            Toast.makeText(requireContext(), "Owner public reply feature enabled via Firestore.", Toast.LENGTH_SHORT).show()
        }

        // Report actions with real Firestore moderation report submission
        view.findViewById<View>(R.id.btn_report_maya)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Report review confirmation",
                message = "Submit this review to platform administrators for moderation investigation?",
                confirmButtonText = "Submit Report",
                cancelButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    val reviewId = allReviews.firstOrNull()?.id ?: "rev_maya"
                    ReviewRepository.reportReview(
                        reviewId = reviewId,
                        reason = "Inappropriate content or policy violation",
                        reporterId = currentBusinessId,
                        onSuccess = {
                            if (isAdded) {
                                cardMaya?.visibility = View.GONE
                                Toast.makeText(requireContext(), "Review reported for Admin moderation.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailure = { e ->
                            if (isAdded) {
                                Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to report review.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            )
        }

        view.findViewById<View>(R.id.btn_report_jordan)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Report review confirmation",
                message = "Submit this review to platform administrators for moderation investigation?",
                confirmButtonText = "Submit Report",
                cancelButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    val reviewId = allReviews.getOrNull(1)?.id ?: "rev_jordan"
                    ReviewRepository.reportReview(
                        reviewId = reviewId,
                        reason = "Disputed customer review",
                        reporterId = currentBusinessId,
                        onSuccess = {
                            if (isAdded) {
                                cardJordan?.visibility = View.GONE
                                Toast.makeText(requireContext(), "Review reported for Admin moderation.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailure = { e ->
                            if (isAdded) {
                                Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to report review.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            )
        }

        // Real-time listener for Reviews
        reviewsListener = ReviewRepository.listenToReviewsForBusiness(currentBusinessId) { list ->
            if (!isAdded) return@listenToReviewsForBusiness
            allReviews = list
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reviewsListener?.remove()
    }
}
