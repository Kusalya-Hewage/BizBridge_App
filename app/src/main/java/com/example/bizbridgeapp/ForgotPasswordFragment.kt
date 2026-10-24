package com.example.bizbridgeapp

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class ForgotPasswordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val svRequestForm = view.findViewById<View>(R.id.sv_request_form)
        val etResetEmail = view.findViewById<EditText>(R.id.et_reset_email)
        val btnSendReset = view.findViewById<Button>(R.id.btn_send_reset)
        val tvBackToLogin = view.findViewById<TextView>(R.id.tv_back_to_login)
        val llSuccessState = view.findViewById<View>(R.id.ll_success_state)
        val tvSuccessDesc = view.findViewById<TextView>(R.id.tv_success_desc)
        val btnSuccessLogin = view.findViewById<Button>(R.id.btn_success_login)
        val btnResend = view.findViewById<Button>(R.id.btn_resend)

        // Top back button
        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Back to Login text link
        tvBackToLogin?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Send Reset Link action
        btnSendReset?.setOnClickListener {
            val email = etResetEmail?.text?.toString()?.trim().orEmpty()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Transition to Supporting State: "Reset instructions requested"
            svRequestForm?.visibility = View.GONE
            llSuccessState?.visibility = View.VISIBLE
            tvSuccessDesc?.text = getString(R.string.reset_requested_desc, email)
        }

        // Success state: Back to Log In button
        btnSuccessLogin?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Success state: Resend Link button
        btnResend?.setOnClickListener {
            val email = etResetEmail?.text?.toString()?.trim().orEmpty()
            Toast.makeText(
                requireContext(),
                "Reset instructions resent to $email",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
