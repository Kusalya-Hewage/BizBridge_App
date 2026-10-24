package com.example.bizbridgeapp

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class RegisterCustomerFragment : Fragment() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_customer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val tvAvatarInitials = view.findViewById<TextView>(R.id.tv_avatar_initials)
        val etFullName = view.findViewById<EditText>(R.id.et_full_name)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etPhone = view.findViewById<EditText>(R.id.et_phone)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = view.findViewById<EditText>(R.id.et_confirm_password)
        val btnTogglePassword = view.findViewById<ImageView>(R.id.btn_toggle_password)
        val btnToggleConfirmPassword = view.findViewById<ImageView>(R.id.btn_toggle_confirm_password)
        val cbTerms = view.findViewById<CheckBox>(R.id.cb_terms)
        val btnCreateAccount = view.findViewById<Button>(R.id.btn_create_account)

        // Back button
        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Dynamic Avatar Initials
        etFullName?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val words = s?.toString()?.trim()?.split(" ")?.filter { it.isNotEmpty() }
                if (!words.isNullOrEmpty()) {
                    val initials = if (words.size == 1) {
                        words[0].take(2).uppercase()
                    } else {
                        "${words[0].first()}${words[1].first()}".uppercase()
                    }
                    tvAvatarInitials?.text = initials
                } else {
                    tvAvatarInitials?.text = "MC"
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Toggle Password
        btnTogglePassword?.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword?.inputType = if (isPasswordVisible) {
                btnTogglePassword.setImageResource(R.drawable.ic_eye)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword?.text?.let { etPassword.setSelection(it.length) }
        }

        // Toggle Confirm Password
        btnToggleConfirmPassword?.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            etConfirmPassword?.inputType = if (isConfirmPasswordVisible) {
                btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etConfirmPassword?.text?.let { etConfirmPassword.setSelection(it.length) }
        }

        // Create Customer Account
        btnCreateAccount?.setOnClickListener {
            val name = etFullName?.text?.toString()?.trim().orEmpty()
            val email = etEmail?.text?.toString()?.trim().orEmpty()
            val password = etPassword?.text?.toString().orEmpty()
            val confirm = etConfirmPassword?.text?.toString().orEmpty()
            val phone = etPhone?.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your full name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(requireContext(), "Password must contain at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cbTerms?.isChecked != true) {
                Toast.makeText(requireContext(), "Please accept the Terms of Service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double clicks
            btnCreateAccount.isEnabled = false

            // Call Firebase to register and store profile
            com.example.bizbridgeapp.FirebaseAuthManager.registerCustomer(
                fullName = name,
                email = email,
                phone = phone,
                password = password,
                onSuccess = { firebaseUser ->
                    activity?.runOnUiThread {
                        btnCreateAccount.isEnabled = true
                        AlertDialog.Builder(requireContext())
                            .setTitle("Account Created")
                            .setMessage("Welcome to BizBridge, $name! Your customer account is ready.")
                            .setPositiveButton("Get Started") { _, _ ->
                                (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
                            }
                            .show()
                    }
                },
                onFailure = { e ->
                    activity?.runOnUiThread {
                        btnCreateAccount.isEnabled = true
                        val msg = when (e) {
                            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "An account with this email already exists."
                            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Password is too weak."
                            else -> e.localizedMessage ?: "Registration failed."
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}
