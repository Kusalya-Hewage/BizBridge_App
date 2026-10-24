package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class SelectServiceFragment : Fragment() {

    private var selectedIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val card1 = view.findViewById<LinearLayout>(R.id.card_service_1)
        val card2 = view.findViewById<LinearLayout>(R.id.card_service_2)
        val card3 = view.findViewById<LinearLayout>(R.id.card_service_3)

        val radio1 = view.findViewById<ImageView>(R.id.radio_service_1)
        val radio2 = view.findViewById<ImageView>(R.id.radio_service_2)
        val radio3 = view.findViewById<ImageView>(R.id.radio_service_3)

        val tvSummary = view.findViewById<TextView>(R.id.tv_summary_selection)
        val btnContinue = view.findViewById<Button>(R.id.btn_continue)

        val cards = listOf(card1, card2, card3)
        val radios = listOf(radio1, radio2, radio3)
        val prices = listOf(45, 65, 35)
        val durations = listOf(45, 60, 30)

        fun updateSelection(index: Int) {
            selectedIndex = index
            for (i in cards.indices) {
                if (i == index) {
                    cards[i]?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_selected_purple)
                    radios[i]?.setImageResource(R.drawable.ic_radio_selected)
                } else {
                    cards[i]?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_white)
                    radios[i]?.setImageResource(R.drawable.ic_radio_unselected)
                }
            }
            tvSummary?.text = "1 service selected • ${durations[index]} minutes"
            btnContinue?.text = "Continue - $${prices[index]}"
        }

        card1?.setOnClickListener { updateSelection(0) }
        card2?.setOnClickListener { updateSelection(1) }
        card3?.setOnClickListener { updateSelection(2) }

        // Continue button navigates to C06 ChooseDateTimeFragment
        btnContinue?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(ChooseDateTimeFragment(), addToBackStack = true)
        }
    }
}
