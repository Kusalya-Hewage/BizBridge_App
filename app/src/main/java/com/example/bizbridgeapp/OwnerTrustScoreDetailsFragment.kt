package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.BusinessRepository
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

        val profile = UserSessionManager.getProfile()
        val score = (profile?.get("trustScore") as? Int)
            ?: UserSessionManager.getAllBusinesses().firstOrNull { it.id == currentBusinessId }?.trustScore
            ?: 80
        view.findViewById<TextView>(R.id.tv_big_trust_score)?.text = "$score / 100"

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

        // Real-time listener for Trust Score
        businessListener = BusinessRepository.listenToOwnerBusiness(currentBusinessId) { biz ->
            if (!isAdded || biz == null) return@listenToOwnerBusiness
            view.findViewById<TextView>(R.id.tv_big_trust_score)?.text = "${biz.trustScore} / 100"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        businessListener?.remove()
    }
}
