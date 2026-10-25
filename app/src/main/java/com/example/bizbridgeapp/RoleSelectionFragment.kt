package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment

class RoleSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_role_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val btnContinueCustomer = view.findViewById<Button>(R.id.btn_continue_customer)
        val btnContinueOwner = view.findViewById<Button>(R.id.btn_continue_owner)

        // Back button
        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Continue as Customer
        btnContinueCustomer?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(RegisterCustomerFragment(), addToBackStack = true)
        }

        // Continue as Business Owner
        btnContinueOwner?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(RegisterOwnerFragment(), addToBackStack = true)
        }
    }
}
