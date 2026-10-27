package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.TrustScoreBreakdown
import com.example.bizbridgeapp.repository.TrustScoreRepository
import com.google.firebase.firestore.ListenerRegistration

class OwnerTrustScoreDetailsFragment : Fragment() {

    private var businessListener: ListenerRegistration? = null
    private var currentBusinessId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_trust_score_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as? MainActivity
        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // 1. Immediately bind from local session/cache (zero delay, no mock score)
        val initialBreakdown = TrustScoreRepository.getLocalTrustScoreBreakdown(currentBusinessId)
        bindBreakdown(view, initialBreakdown)

        // 2. Refresh dynamically from cloud database
        TrustScoreRepository.getTrustScoreBreakdown(currentBusinessId) { breakdown ->
            if (isAdded) {
                bindBreakdown(view, breakdown)
            }
        }

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_trust_score)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Navigate to B10 on recommendations
        view.findViewById<View>(R.id.card_strengthen_1)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerQualificationsDocumentsFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.card_strengthen_2)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerQualificationsDocumentsFragment(), addToBackStack = true)
        }
        view.findViewById<View>(R.id.card_strengthen_3)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerCustomerReviewsFragment(), addToBackStack = true)
        }

        // Real-time listener for business changes (recalculates Trust Score live)
        businessListener = BusinessRepository.listenToOwnerBusiness(currentBusinessId) { biz ->
            if (!isAdded || biz == null) return@listenToOwnerBusiness
            TrustScoreRepository.getTrustScoreBreakdown(currentBusinessId) { breakdown ->
                if (isAdded) {
                    bindBreakdown(view, breakdown)
                }
            }
        }
    }

    private fun bindBreakdown(view: View, breakdown: TrustScoreBreakdown) {
        // Overall score & descriptive trust badge
        view.findViewById<TextView>(R.id.tv_big_trust_score)?.text = "${breakdown.totalScore} / 100"
        view.findViewById<TextView>(R.id.tv_trust_score_caption)?.text = "${breakdown.trustBadge} · ${breakdown.trustDescription}"

        // 1. Professional qualifications (max 30)
        view.findViewById<TextView>(R.id.tv_score_qualifications)?.text = "${breakdown.qualificationScore}/${breakdown.maxQualificationScore}"
        view.findViewById<ProgressBar>(R.id.pb_qualifications)?.apply {
            max = breakdown.maxQualificationScore
            progress = breakdown.qualificationScore
        }

        // 2. Industry experience / Approval base (max 25)
        view.findViewById<TextView>(R.id.tv_score_experience)?.text = "${breakdown.experienceScore}/${breakdown.maxExperienceScore}"
        view.findViewById<ProgressBar>(R.id.pb_experience)?.apply {
            max = breakdown.maxExperienceScore
            progress = breakdown.experienceScore
        }

        // 3. Profile completeness (max 15)
        view.findViewById<TextView>(R.id.tv_score_profile)?.text = "${breakdown.profileScore}/${breakdown.maxProfileScore}"
        view.findViewById<ProgressBar>(R.id.pb_profile)?.apply {
            max = breakdown.maxProfileScore
            progress = breakdown.profileScore
        }

        // 4. Verified documents (max 15)
        view.findViewById<TextView>(R.id.tv_score_documents)?.text = "${breakdown.docScore}/${breakdown.maxDocScore}"
        view.findViewById<ProgressBar>(R.id.pb_documents)?.apply {
            max = breakdown.maxDocScore
            progress = breakdown.docScore
        }

        // 5. Customer reviews (max 15)
        view.findViewById<TextView>(R.id.tv_score_reviews)?.text = "${breakdown.reviewScore}/${breakdown.maxReviewScore}"
        view.findViewById<ProgressBar>(R.id.pb_reviews)?.apply {
            max = breakdown.maxReviewScore
            progress = breakdown.reviewScore
        }

        // Action recommendations hints
        view.findViewById<TextView>(R.id.tv_qualifications_hint)?.text =
            "Qualification points: ${breakdown.qualificationScore} of ${breakdown.maxQualificationScore}"
        view.findViewById<TextView>(R.id.tv_reviews_hint)?.text =
            "Customer reviews contribute ${breakdown.reviewScore} of ${breakdown.maxReviewScore} points."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        businessListener?.remove()
    }
}
