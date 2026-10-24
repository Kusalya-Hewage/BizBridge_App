package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class BusinessDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_business_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Book Appointment button -> navigates to SelectServiceFragment (C05)
        view.findViewById<Button>(R.id.btn_book_appointment)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SelectServiceFragment(), addToBackStack = true)
        }

        // Service items click -> also opens SelectServiceFragment
        view.findViewById<LinearLayout>(R.id.item_service_1)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SelectServiceFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.item_service_2)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SelectServiceFragment(), addToBackStack = true)
        }
    }
}
