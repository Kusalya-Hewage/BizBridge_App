package com.example.bizbridgeapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

import com.example.bizbridgeapp.model.Review
import com.example.bizbridgeapp.repository.ReviewRepository
import com.example.bizbridgeapp.UserSessionManager
import com.google.firebase.auth.FirebaseAuth

class WriteReviewFragment : Fragment() {

    private var currentRating = 4
    private var bookingId: String = ""
    private var businessId: String = ""
    private var businessName: String = ""

    companion object {
        fun newInstance(bookingId: String, businessId: String, businessName: String): WriteReviewFragment {
            val frag = WriteReviewFragment()
            val args = Bundle()
            args.putString("bookingId", bookingId)
            args.putString("businessId", businessId)
            args.putString("businessName", businessName)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bookingId = arguments?.getString("bookingId").orEmpty()
        businessId = arguments?.getString("businessId").orEmpty()
        businessName = arguments?.getString("businessName").orEmpty()
        return inflater.inflate(R.layout.fragment_write_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val star1 = view.findViewById<ImageView>(R.id.star_1)
        val star2 = view.findViewById<ImageView>(R.id.star_2)
        val star3 = view.findViewById<ImageView>(R.id.star_3)
        val star4 = view.findViewById<ImageView>(R.id.star_4)
        val star5 = view.findViewById<ImageView>(R.id.star_5)
        val stars = listOf(star1, star2, star3, star4, star5)

        val tvRatingLabel = view.findViewById<TextView>(R.id.tv_rating_score_label)
        val etReview = view.findViewById<EditText>(R.id.et_review_text)
        val tvCharCount = view.findViewById<TextView>(R.id.tv_char_count)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit_review)

        fun updateRating(rating: Int) {
            currentRating = rating
            stars.forEachIndexed { index, star ->
                if (index < rating) {
                    star?.setImageResource(R.drawable.ic_star_filled)
                } else {
                    star?.setImageResource(R.drawable.ic_star_outline)
                }
            }
            val labels = listOf("1 out of 5 · Poor", "2 out of 5 · Fair", "3 out of 5 · Good", "4 out of 5 · Excellent", "5 out of 5 · Outstanding")
            tvRatingLabel?.text = labels.getOrElse(rating - 1) { "$rating out of 5" }
        }

        stars.forEachIndexed { index, star ->
            star?.setOnClickListener { updateRating(index + 1) }
        }

        etReview?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvCharCount?.text = "${s?.length ?: 0} / 500 characters"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Submit Review button
        btnSubmit?.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Please log in to submit a review.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val comment = etReview?.text?.toString()?.trim().orEmpty()
            if (comment.isEmpty()) {
                Toast.makeText(requireContext(), "Please write a comment for your review.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val profile = UserSessionManager.getProfile()
            val customerName = (profile?.get("fullName") as? String) ?: "Customer"

            btnSubmit.isEnabled = false

            val review = Review(
                bookingId = if (bookingId.isNotEmpty()) bookingId else "booking_${System.currentTimeMillis()}",
                businessId = if (businessId.isNotEmpty()) businessId else "business_juniper",
                customerId = user.uid,
                customerName = customerName,
                rating = currentRating,
                comment = comment
            )

            ReviewRepository.submitReview(
                review = review,
                onSuccess = {
                    if (isAdded) {
                        btnSubmit.isEnabled = true
                        Toast.makeText(requireContext(), "Thank you! Your review has been submitted.", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                },
                onFailure = { e ->
                    if (isAdded) {
                        btnSubmit.isEnabled = true
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to submit review.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}
