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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class RegisterOwnerFragment : Fragment() {

    private var isPasswordVisible = false
    private val categories = arrayOf(
        "Hair salon",
        "Beauty & Spa",
        "Fitness",
        "Repair & Maintenance",
        "Wellness",
        "Education",
        "Other"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_owner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val containerCategory = view.findViewById<View>(R.id.container_category)
        val tvCategory = view.findViewById<TextView>(R.id.tv_category)
        val etOwnerName = view.findViewById<EditText>(R.id.et_owner_name)
        val etBusinessName = view.findViewById<EditText>(R.id.et_business_name)
        val etBusinessEmail = view.findViewById<EditText>(R.id.et_business_email)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val btnTogglePassword = view.findViewById<ImageView>(R.id.btn_toggle_password)
        val btnCreateOwnerAccount = view.findViewById<Button>(R.id.btn_create_owner_account)

        // Back button
        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Category dropdown dialog
        containerCategory?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Business Category")
                .setItems(categories) { _, which ->
                    tvCategory?.text = categories[which]
                }
                .show()
        }

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

        // Create Owner Account
        btnCreateOwnerAccount?.setOnClickListener {
            val ownerName = etOwnerName?.text?.toString()?.trim().orEmpty()
            val businessName = etBusinessName?.text?.toString()?.trim().orEmpty()
            val email = etBusinessEmail?.text?.toString()?.trim().orEmpty()
            val password = etPassword?.text?.toString().orEmpty()

            if (ownerName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the owner name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (businessName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the business name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid business email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(requireContext(), "Password must contain at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = tvCategory?.text?.toString()?.ifBlank { "Hair salon" } ?: "Hair salon"

            btnCreateOwnerAccount.isEnabled = false
            Toast.makeText(requireContext(), "Submitting business application...", Toast.LENGTH_SHORT).show()

            FirebaseAuthManager.registerOwner(
                ownerName = ownerName,
                businessName = businessName,
                category = category,
                email = email,
                password = password,
                onSuccess = { _, _ ->
                    activity?.runOnUiThread {
                        btnCreateOwnerAccount.isEnabled = true
                        AlertDialog.Builder(requireContext())
                            .setTitle("Application Submitted")
                            .setMessage("Your business registration for $businessName has been submitted for administrator approval. You can log in and manage your services and profile.")
                            .setPositiveButton("Proceed to Login") { _, _ ->
                                (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = false)
                            }
                            .setCancelable(false)
                            .show()
                    }
                },
                onFailure = { e ->
                    activity?.runOnUiThread {
                        btnCreateOwnerAccount.isEnabled = true
                        Toast.makeText(requireContext(), "Registration failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}
