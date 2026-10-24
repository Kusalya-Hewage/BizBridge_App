package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class ChooseDateTimeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_choose_date_time, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Top back arrow
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Action back button
        view.findViewById<Button>(R.id.btn_action_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val slot9am = view.findViewById<TextView>(R.id.slot_9am)
        val slot10am = view.findViewById<TextView>(R.id.slot_10am)
        val slot11am = view.findViewById<TextView>(R.id.slot_11am)
        val slot130pm = view.findViewById<TextView>(R.id.slot_130pm)
        val slot230pm = view.findViewById<TextView>(R.id.slot_230pm)
        val slot330pm = view.findViewById<TextView>(R.id.slot_330pm)

        val tvSelectedPill = view.findViewById<TextView>(R.id.tv_selected_pill)

        val slots = listOf(slot9am, slot10am, slot11am, slot130pm, slot230pm, slot330pm)
        val slotTexts = listOf("9:00–9:45 AM", "10:00–10:45 AM", "11:00–11:45 AM", "1:30–2:15 PM", "2:30–3:15 PM", "3:30–4:15 PM")

        fun selectSlot(index: Int) {
            slots.forEachIndexed { i, slot ->
                if (i == index) {
                    slot?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_selected)
                    slot?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    slot?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_default)
                    slot?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                }
            }
            tvSelectedPill?.text = "Selected: Sat, Oct 24 • ${slotTexts[index]}"
        }

        slots.forEachIndexed { index, slot ->
            slot?.setOnClickListener { selectSlot(index) }
        }

        // Continue button navigates to C07 ReviewBookingFragment
        view.findViewById<Button>(R.id.btn_continue)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(ReviewBookingFragment(), addToBackStack = true)
        }
    }
}
