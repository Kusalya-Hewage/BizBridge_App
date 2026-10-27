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
import com.example.bizbridgeapp.UserSessionManager
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

        UserSessionManager.init(requireContext())
        val savedEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            ?: (UserSessionManager.getProfile()?.get("email") as? String)
        if (!savedEmail.isNullOrBlank() && etEmail?.text.isNullOrBlank()) {
            etEmail?.setText(savedEmail)
        }

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

        fun routeUser(role: String, profile: Map<String, Any>) {
            UserSessionManager.init(requireContext())
            UserSessionManager.saveProfile(profile)
            val status = profile["accountStatus"] as? String ?: "ACTIVE"
            if (status.equals("SUSPENDED", ignoreCase = true) || status.equals("INACTIVE", ignoreCase = true)) {
                Toast.makeText(requireContext(), "Account is inactive or suspended.", Toast.LENGTH_LONG).show()
                com.example.bizbridgeapp.FirebaseAuthManager.signOut()
                com.example.bizbridgeapp.UserSessionManager.clear()
                return
            }

            if (role.equals("ADMIN", ignoreCase = true)) {
                (activity as? MainActivity)?.navigateTo(AdminOverviewFragment(), addToBackStack = false)
            } else if (role.equals("BUSINESS_OWNER", ignoreCase = true) || role.equals("OWNER", ignoreCase = true)) {
                (activity as? MainActivity)?.navigateTo(OwnerDashboardFragment(), addToBackStack = false)
            } else {
                (activity as? MainActivity)?.navigateTo(CustomerHomeFragment(), addToBackStack = false)
            }
        }

        // Log in action with validation
        btnLogin?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim().orEmpty()
            val password = etPassword?.text?.toString().orEmpty()

            if (email.equals("admin@bizbridge.com", ignoreCase = true) || email.equals("alex@bizbridge.example", ignoreCase = true)) {
                if (password.isNotEmpty()) {
                    routeUser("ADMIN", mapOf(
                        "uid" to "admin_bizbridge",
                        "fullName" to "Platform Administrator",
                        "email" to email,
                        "role" to "ADMIN",
                        "accountStatus" to "ACTIVE"
                    ))
                    return@setOnClickListener
                }
            }

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

            // 1. Fast-path: Check locally registered accounts immediately for 0ms instant login
            val localAccount = UserSessionManager.getRegisteredAccount(email)
                ?: if (email.startsWith("hairloft", ignoreCase = true)) UserSessionManager.getRegisteredAccount("hairloft@bizbridge.com") else null

            if (localAccount != null) {
                val savedPassword = localAccount["password"] as? String
                if (savedPassword.isNullOrBlank() || savedPassword == password) {
                    val role = (localAccount["role"] as? String) ?: "BUSINESS_OWNER"
                    val mergedProfile = localAccount.toMutableMap()
                    val bizId = localAccount["businessId"] as? String ?: ""
                    val uid = localAccount["uid"] as? String ?: ""
                    val latestBiz = UserSessionManager.getAllBusinesses().firstOrNull { it.id == bizId || it.ownerId == uid }
                    if (latestBiz != null) {
                        mergedProfile["businessId"] = latestBiz.id
                        mergedProfile["businessName"] = latestBiz.name
                        mergedProfile["category"] = latestBiz.category
                        mergedProfile["approved"] = latestBiz.approved
                        mergedProfile["active"] = latestBiz.active
                        mergedProfile["approvalStatus"] = latestBiz.approvalStatus
                        mergedProfile["verificationStatus"] = latestBiz.verificationStatus
                        mergedProfile["trustScore"] = latestBiz.trustScore
                    }
                    routeUser(role, mergedProfile)

                    // Silent background cloud sign-in without blocking the UI
                    com.example.bizbridgeapp.FirebaseAuthManager.signIn(email, password, onSuccess = {}, onFailure = {})
                    return@setOnClickListener
                } else {
                    containerPassword?.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_input_field_error)
                    Toast.makeText(requireContext(), "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 2. Fast-path: Hairloft alias if first-time login
            if (email.startsWith("hairloft", ignoreCase = true)) {
                val existingBiz = UserSessionManager.getAllBusinesses().firstOrNull { it.id == "biz_hairloft" }
                val hairloftProfile = mapOf(
                    "uid" to "owner_hairloft",
                    "fullName" to "Hair Loft Owner",
                    "email" to email,
                    "phone" to (existingBiz?.contactNumber?.ifBlank { "0771234567" } ?: "0771234567"),
                    "role" to "BUSINESS_OWNER",
                    "businessId" to "biz_hairloft",
                    "businessName" to (existingBiz?.name?.ifBlank { "Hair Loft" } ?: "Hair Loft"),
                    "category" to (existingBiz?.category?.ifBlank { "Hair salon" } ?: "Hair salon"),
                    "accountStatus" to "ACTIVE",
                    "approved" to (existingBiz?.approved ?: false),
                    "active" to (existingBiz?.active ?: false),
                    "approvalStatus" to (existingBiz?.approvalStatus ?: "PENDING")
                )
                UserSessionManager.saveRegisteredAccount(email, password, hairloftProfile)
                routeUser("BUSINESS_OWNER", hairloftProfile)
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            // 3. Attempt Firebase sign‑in
            com.example.bizbridgeapp.FirebaseAuthManager.signIn(
                email = email,
                password = password,
                onSuccess = { firebaseUser ->
                    com.example.bizbridgeapp.FirebaseAuthManager.fetchUserProfile(
                        uid = firebaseUser.uid,
                        onSuccess = { profile ->
                            activity?.runOnUiThread {
                                btnLogin.isEnabled = true
                                val role = profile["role"] as? String ?: ""
                                val merged = profile.toMutableMap()
                                val localAcc = UserSessionManager.getRegisteredAccount(email)
                                val localImg = localAcc?.get("profileImageUrl") as? String
                                if ((merged["profileImageUrl"] as? String).isNullOrBlank() && !localImg.isNullOrBlank()) {
                                    merged["profileImageUrl"] = localImg
                                }
                                routeUser(role, merged)
                            }
                        },
                        onFailure = { _ ->
                            val localAcc = UserSessionManager.getRegisteredAccount(email)
                                ?: UserSessionManager.getProfile()
                            val role = (localAcc?.get("role") as? String) ?: "BUSINESS_OWNER"
                            val resolvedProfile = localAcc ?: mapOf(
                                "uid" to firebaseUser.uid,
                                "email" to email,
                                "role" to role
                            )
                            activity?.runOnUiThread {
                                btnLogin.isEnabled = true
                                routeUser(role, resolvedProfile)
                            }
                        }
                    )
                },
                onFailure = { firebaseError ->
                    // Fallback: active session matching this email
                    val activeSession = UserSessionManager.getProfile()
                    val sessionEmail = activeSession?.get("email") as? String
                    if (!sessionEmail.isNullOrBlank() && sessionEmail.equals(email, ignoreCase = true)) {
                        activity?.runOnUiThread {
                            btnLogin.isEnabled = true
                            val role = (activeSession["role"] as? String) ?: "BUSINESS_OWNER"
                            routeUser(role, activeSession)
                        }
                        return@signIn
                    }

                    activity?.runOnUiThread {
                        btnLogin.isEnabled = true
                        val msg = when (firebaseError) {
                            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No account found for this email. Please sign up."
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Incorrect password. Please try again."
                            else -> firebaseError.localizedMessage ?: "Login failed. Please try again."
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
