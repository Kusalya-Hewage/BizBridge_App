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

class WriteReviewFragment : Fragment() {

    private var currentRating = 4

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        view.findViewById<Button>(R.id.btn_submit_review)?.setOnClickListener {
            Toast.makeText(requireContext(), "Thank you! Your review has been submitted.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
    }
}
