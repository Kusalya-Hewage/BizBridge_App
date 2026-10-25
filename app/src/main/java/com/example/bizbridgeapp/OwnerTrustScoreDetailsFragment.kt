package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class OwnerTrustScoreDetailsFragment : Fragment() {

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
    }
}
