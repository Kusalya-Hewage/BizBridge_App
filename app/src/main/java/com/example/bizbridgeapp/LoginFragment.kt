package com.example.bizbridgeapp

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class LoginFragment : Fragment() {

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val containerEmail = view.findViewById<View>(R.id.container_email)
        val containerPassword = view.findViewById<View>(R.id.container_password)
        val tvEmailError = view.findViewById<TextView>(R.id.tv_email_error)
        val btnTogglePassword = view.findViewById<ImageView>(R.id.btn_toggle_password)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tv_forgot_password)
        val tvSignUp = view.findViewById<TextView>(R.id.tv_sign_up)
        val btnLogin = view.findViewById<Button>(R.id.btn_login)

        // Password visibility toggle
        btnTogglePassword?.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword?.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                etPassword?.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            etPassword?.text?.let { etPassword.setSelection(it.length) }
        }

        // Forgot password navigation
        tvForgotPassword?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(ForgotPasswordFragment(), addToBackStack = true)
        }

        // Sign Up navigation
        tvSignUp?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(RoleSelectionFragment(), addToBackStack = true)
        }

        // Log in action with validation
        btnLogin?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim().orEmpty()
            val password = etPassword?.text?.toString().orEmpty()

            if (!isValidEmail(email)) {
                containerEmail?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_input_field_error)
                tvEmailError?.text = "Enter a valid email address."
                tvEmailError?.visibility = View.VISIBLE
                return@setOnClickListener
            } else {
                containerEmail?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_input_field)
                tvEmailError?.visibility = View.GONE
            }

            if (password.isEmpty()) {
                containerPassword?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_input_field_error)
                Toast.makeText(requireContext(), "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                containerPassword?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_input_field)
            }

            // Successful demonstration login
            Toast.makeText(requireContext(), "Logged in as $email", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
