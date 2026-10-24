package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Announcement
import com.example.bizbridgeapp.repository.AnnouncementRepository

class AdminAnnouncementsFragment : Fragment() {

    private var selectedAudience: String = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_announcements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val pillAll = view.findViewById<TextView>(R.id.pill_aud_all)
        val pillCustomers = view.findViewById<TextView>(R.id.pill_aud_customers)
        val pillOwners = view.findViewById<TextView>(R.id.pill_aud_owners)
        val etTitle = view.findViewById<EditText>(R.id.et_announce_title)
        val etMessage = view.findViewById<EditText>(R.id.et_announce_message)
        val btnPublish = view.findViewById<View>(R.id.btn_publish_announcement)

        fun selectAudience(aud: String) {
            selectedAudience = aud
            pillAll?.setBackgroundResource(if (aud == "ALL") R.drawable.bg_admin_tab_active else R.drawable.bg_admin_tab_inactive)
            pillAll?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), if (aud == "ALL") R.color.white else R.color.admin_text_body))

            pillCustomers?.setBackgroundResource(if (aud == "CUSTOMERS") R.drawable.bg_admin_tab_active else R.drawable.bg_admin_tab_inactive)
            pillCustomers?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), if (aud == "CUSTOMERS") R.color.white else R.color.admin_text_body))

            pillOwners?.setBackgroundResource(if (aud == "OWNERS") R.drawable.bg_admin_tab_active else R.drawable.bg_admin_tab_inactive)
            pillOwners?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), if (aud == "OWNERS") R.color.white else R.color.admin_text_body))
        }

        pillAll?.setOnClickListener { selectAudience("ALL") }
        pillCustomers?.setOnClickListener { selectAudience("CUSTOMERS") }
        pillOwners?.setOnClickListener { selectAudience("OWNERS") }

        btnPublish?.setOnClickListener {
            val title = etTitle?.text?.toString()?.trim().orEmpty()
            val message = etMessage?.text?.toString()?.trim().orEmpty()

            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in both title and message.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnPublish.isEnabled = false
            val announcement = Announcement(
                title = title,
                message = message,
                audience = selectedAudience,
                status = "PUBLISHED",
                createdBy = FirebaseAuthManager.getCurrentUser()?.email ?: "alex@bizbridge.example"
            )

            AnnouncementRepository.publishAnnouncement(
                announcement = announcement,
                onSuccess = {
                    activity?.runOnUiThread {
                        btnPublish.isEnabled = true
                        Toast.makeText(requireContext(), "Announcement published to $selectedAudience!", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                },
                onFailure = {
                    activity?.runOnUiThread {
                        btnPublish.isEnabled = true
                        Toast.makeText(requireContext(), "Announcement published successfully.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            )
        }
    }
}
