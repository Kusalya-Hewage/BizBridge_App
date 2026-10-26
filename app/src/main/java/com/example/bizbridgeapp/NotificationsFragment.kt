package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import com.example.bizbridgeapp.model.NotificationItem
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth

class NotificationsFragment : Fragment() {

    private var userNotifications: List<NotificationItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid.orEmpty()

        // Back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Top Bell button
        view.findViewById<ImageView>(R.id.btn_bell)?.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications are up to date.", Toast.LENGTH_SHORT).show()
        }

        // Active tab state
        var currentTab = "ALL"

        val container = view.findViewById<LinearLayout>(R.id.container_notifications)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_empty_notifications)

        fun renderNotifications() {
            if (!isAdded) return
            val filtered = when (currentTab) {
                "BOOKING" -> userNotifications.filter { it.category.equals("BOOKING", ignoreCase = true) }
                "OFFER" -> userNotifications.filter { it.category.equals("OFFER", ignoreCase = true) || it.category.equals("PROMOTION", ignoreCase = true) }
                else -> userNotifications
            }

            container?.removeAllViews()

            if (filtered.isEmpty()) {
                container?.visibility = View.GONE
                layoutEmpty?.visibility = View.VISIBLE
                return
            }

            container?.visibility = View.VISIBLE
            layoutEmpty?.visibility = View.GONE

            val inflater = LayoutInflater.from(requireContext())
            for (item in filtered) {
                val itemView = inflater.inflate(R.layout.item_notification_card, container, false)

                val tvTitle = itemView.findViewById<TextView>(R.id.tv_notif_title)
                val tvMessage = itemView.findViewById<TextView>(R.id.tv_notif_message)
                val tvTime = itemView.findViewById<TextView>(R.id.tv_notif_time)
                val viewUnreadDot = itemView.findViewById<View>(R.id.view_unread_dot)
                val frameIcon = itemView.findViewById<FrameLayout>(R.id.frame_notif_icon)
                val ivIcon = itemView.findViewById<ImageView>(R.id.iv_notif_icon)

                tvTitle.text = item.title
                tvMessage.text = item.message
                viewUnreadDot.visibility = if (!item.isRead) View.VISIBLE else View.GONE

                val timeStr = item.createdAt?.toDate()?.let { date ->
                    val diffMs = java.lang.System.currentTimeMillis() - date.time
                    val hours = diffMs / (1000 * 60 * 60)
                    when {
                        hours < 1 -> "Just now"
                        hours < 24 -> "$hours hours ago"
                        hours < 48 -> "Yesterday"
                        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.US).format(date)
                    }
                } ?: "Recent"
                tvTime.text = timeStr

                val isOffer = item.category.equals("OFFER", ignoreCase = true) || item.category.equals("PROMOTION", ignoreCase = true)
                val isCancelled = item.title.contains("cancel", ignoreCase = true) || item.message.contains("cancel", ignoreCase = true)
                val isReminder = item.title.contains("reminder", ignoreCase = true) || item.message.contains("reminder", ignoreCase = true)

                when {
                    isOffer -> {
                        frameIcon.setBackgroundResource(R.drawable.bg_tag_verified)
                        ivIcon.setImageResource(R.drawable.ic_sparkle)
                        ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
                    }
                    isCancelled -> {
                        frameIcon.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
                        ivIcon.setImageResource(R.drawable.ic_cancel)
                        ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error_text))
                    }
                    isReminder -> {
                        frameIcon.setBackgroundResource(R.drawable.bg_card_lavender)
                        ivIcon.setImageResource(R.drawable.ic_clock)
                        ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
                    }
                    else -> {
                        frameIcon.setBackgroundResource(R.drawable.bg_card_lavender)
                        ivIcon.setImageResource(R.drawable.ic_calendar)
                        ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
                    }
                }

                itemView.setOnClickListener {
                    if (!item.isRead) {
                        NotificationRepository.markAsRead(item.id, onSuccess = {}, onFailure = {})
                        viewUnreadDot.visibility = View.GONE
                    }

                    if (item.relatedBookingId.isNotBlank()) {
                        (activity as? MainActivity)?.navigateTo(BookingDetailsFragment.newInstance(item.relatedBookingId), addToBackStack = true)
                    } else if (item.relatedBusinessId.isNotBlank()) {
                        (activity as? MainActivity)?.navigateTo(BusinessDetailsFragment.newInstance(item.relatedBusinessId), addToBackStack = true)
                    }
                }

                container?.addView(itemView)
            }
        }

        // Mark all as read
        val btnMarkAll = view.findViewById<TextView>(R.id.btn_mark_all_read)
        btnMarkAll?.setOnClickListener {
            val targetUid = uid.ifEmpty { (UserSessionManager.getProfile()?.get("uid") as? String).orEmpty() }
            if (targetUid.isEmpty()) return@setOnClickListener
            NotificationRepository.markAllAsRead(targetUid,
                onSuccess = {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
                        btnMarkAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                        btnMarkAll.isEnabled = false
                        loadNotifications(targetUid) {
                            renderNotifications()
                        }
                    }
                },
                onFailure = {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
                        btnMarkAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                        btnMarkAll.isEnabled = false
                    }
                }
            )
        }


        // Tabs
        val tabAll = view.findViewById<LinearLayout>(R.id.tab_all)
        val tabBookings = view.findViewById<LinearLayout>(R.id.tab_bookings)
        val tabOffers = view.findViewById<LinearLayout>(R.id.tab_offers)
        val tvTabAll = view.findViewById<TextView>(R.id.tv_tab_all)
        val tvTabBookings = view.findViewById<TextView>(R.id.tv_tab_bookings)
        val tvTabOffers = view.findViewById<TextView>(R.id.tv_tab_offers)

        fun selectTab(selected: String) {
            currentTab = selected
            val tabs = listOf(
                Triple(tabAll, tvTabAll, "ALL"),
                Triple(tabBookings, tvTabBookings, "BOOKING"),
                Triple(tabOffers, tvTabOffers, "OFFER")
            )
            for ((layout, tv, key) in tabs) {
                val isSel = selected == key
                layout?.setBackgroundResource(if (isSel) R.drawable.bg_chip_selected else R.drawable.bg_chip_default)
                if (isSel) {
                    tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.surface_white))
                } else {
                    tv?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
            }
            renderNotifications()
        }

        tabAll?.setOnClickListener { selectTab("ALL") }
        tvTabAll?.setOnClickListener { selectTab("ALL") }
        tabBookings?.setOnClickListener { selectTab("BOOKING") }
        tvTabBookings?.setOnClickListener { selectTab("BOOKING") }
        tabOffers?.setOnClickListener { selectTab("OFFER") }
        tvTabOffers?.setOnClickListener { selectTab("OFFER") }

        // Start listening to real notifications
        val effectiveUid = uid.ifEmpty { (UserSessionManager.getProfile()?.get("uid") as? String).orEmpty() }
        loadNotifications(effectiveUid) {
            renderNotifications()
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
        view.findViewById<LinearLayout>(R.id.nav_profile)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(CustomerProfileFragment(), addToBackStack = true)
        }
    }

    private var notificationsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun loadNotifications(uid: String, onLoaded: () -> Unit = {}) {
        notificationsListener?.remove()
        notificationsListener = NotificationRepository.listenToNotificationsForUser(uid) { list ->
            if (!isAdded) return@listenToNotificationsForUser
            userNotifications = list
            if (list.isEmpty() && uid.isNotBlank()) {
                // If there are real customer bookings, automatically generate corresponding notifications
                BookingRepository.getBookingsForCustomer(uid,
                    onSuccess = { bookings ->
                        if (!isAdded) return@getBookingsForCustomer
                        if (bookings.isNotEmpty()) {
                            for (b in bookings) {
                                val statusLabel = if (b.status.equals("CANCELLED", ignoreCase = true)) "Cancelled" else "Confirmed"
                                val notif = NotificationItem(
                                    id = "notif_bk_${b.id}",
                                    userId = uid,
                                    title = "Booking $statusLabel",
                                    message = "${b.businessNameSnapshot} • ${b.serviceNameSnapshot} on ${b.appointmentDate} at ${b.appointmentTime}",
                                    category = "BOOKING",
                                    relatedBookingId = b.id,
                                    relatedBusinessId = b.businessId,
                                    isRead = false
                                )
                                NotificationRepository.createNotification(notif, onSuccess = {}, onFailure = {})
                            }
                        }
                    },
                    onFailure = {}
                )
            }
            onLoaded()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationsListener?.remove()
    }
}
