package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar

import com.example.bizbridgeapp.repository.UserRepository
import com.example.bizbridgeapp.UserSessionManager
import com.example.bizbridgeapp.FirebaseAuthManager
import com.example.bizbridgeapp.LoginFragment
import com.example.bizbridgeapp.RoleSelectionFragment

import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.Fragment

class CustomerProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customer_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bell
        view.findViewById<ImageView>(R.id.btn_bell)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }

        // Edit Profile
        view.findViewById<Button>(R.id.btn_edit_profile)?.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
            val context = requireContext()

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }
            val inputName = android.widget.EditText(context).apply {
                hint = "Full Name"
                setText(view.findViewById<TextView>(R.id.tv_profile_name)?.text)
            }
            val inputPhone = android.widget.EditText(context).apply {
                hint = "Phone Number"
                setText(view.findViewById<TextView>(R.id.tv_profile_phone)?.text)
            }
            layout.addView(inputName)
            layout.addView(inputPhone)

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val newName = inputName.text.toString().trim()
                    val newPhone = inputPhone.text.toString().trim()
                    if (newName.isEmpty()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val updated = com.example.bizbridgeapp.model.UserProfile(
                        uid = currentUser.uid,
                        fullName = newName,
                        email = currentUser.email.orEmpty(),
                        phone = newPhone,
                        role = "CUSTOMER",
                        accountStatus = "ACTIVE"
                    )
                    UserRepository.createOrUpdate(updated,
                        onSuccess = {
                            view.findViewById<TextView>(R.id.tv_profile_name)?.text = newName
                            view.findViewById<TextView>(R.id.tv_profile_phone)?.text = newPhone
                            UserSessionManager.saveProfile(updated.toMap())
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Settings items
        view.findViewById<LinearLayout>(R.id.row_notifications_pref)?.setOnClickListener {
            val options = arrayOf("Booking updates & reminders", "Promotions & local deals")
            val checked = booleanArrayOf(true, true)
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Notification Preferences")
                .setMultiChoiceItems(options, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("Save") { _, _ ->
                    Toast.makeText(requireContext(), "Preferences saved", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        view.findViewById<LinearLayout>(R.id.row_password_security)?.setOnClickListener {
            val email = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
            if (email.isNotEmpty()) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Password & Security")
                    .setMessage("Send a password reset email to $email?")
                    .setPositiveButton("Send Reset Email") { _, _ ->
                        FirebaseAuthManager.sendPasswordReset(email,
                            onSuccess = {
                                Toast.makeText(requireContext(), "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                            },
                            onFailure = { e ->
                                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        view.findViewById<LinearLayout>(R.id.row_admin_workspace)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(AdminOverviewFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.row_privacy_policy)?.setOnClickListener {
            Toast.makeText(requireContext(), "BizBridge connects customers to local verified businesses. Your data is protected.", Toast.LENGTH_LONG).show()
        }
        view.findViewById<LinearLayout>(R.id.row_terms_service)?.setOnClickListener {
            Toast.makeText(requireContext(), "BizBridge Terms of Service: Safe, reliable local booking.", Toast.LENGTH_LONG).show()
        }
        view.findViewById<LinearLayout>(R.id.row_help_support)?.setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support: support@bizbridge.local", Toast.LENGTH_SHORT).show()
        }

        // Fetch and display profile
        val progressBar = view.findViewById<ProgressBar>(R.id.profile_progress)
        progressBar?.visibility = View.VISIBLE

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            progressBar?.visibility = View.GONE
            (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = false)
        } else {
            UserRepository.getProfile(currentUser.uid,
                onSuccess = { profile ->
                    view.findViewById<TextView>(R.id.tv_profile_name)?.text = profile.fullName
                    view.findViewById<TextView>(R.id.tv_profile_email)?.text = profile.email
                    view.findViewById<TextView>(R.id.tv_profile_phone)?.text = profile.phone
                    UserSessionManager.saveProfile(profile.toMap())
                    progressBar?.visibility = View.GONE
                },
                onFailure = { e ->
                    progressBar?.visibility = View.GONE
                    Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to load profile", Toast.LENGTH_LONG).show()
                })
        }

        // Log out - single clean implementation
        view.findViewById<TextView>(R.id.btn_logout)?.setOnClickListener {
            FirebaseAuthManager.signOut()
            UserSessionManager.clear()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.navigateTo(LoginFragment(), addToBackStack = false)
        }

        // Bottom Navigation
        view.findViewById<LinearLayout>(R.id.nav_home)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
        }
        view.findViewById<LinearLayout>(R.id.nav_search)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(SearchBrowseFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_bookings)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(MyBookingsFragment(), addToBackStack = true)
        }
        view.findViewById<LinearLayout>(R.id.nav_notifications)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(NotificationsFragment(), addToBackStack = true)
        }
    }
}
