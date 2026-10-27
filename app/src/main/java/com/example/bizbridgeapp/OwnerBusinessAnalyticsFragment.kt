package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.model.Service
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.ServiceRepository
import com.google.firebase.firestore.ListenerRegistration

class OwnerBusinessAnalyticsFragment : Fragment() {

    private var bookingsListener: ListenerRegistration? = null
    private var servicesListener: ListenerRegistration? = null
    private var currentBusinessId: String = ""
    private var latestBookings: List<Booking> = emptyList()
    private var latestServices: List<Service> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_business_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bottom Navigation
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.INSIGHTS)

        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Initialize 0 counts initially so mock values don't show
        view.findViewById<TextView>(R.id.tv_analytics_total_bookings)?.text = "0"
        view.findViewById<TextView>(R.id.tv_analytics_completed_count)?.text = "0"
        view.findViewById<TextView>(R.id.tv_analytics_completion_rate)?.text = "0% completion rate"
        view.findViewById<TextView>(R.id.tv_analytics_status_breakdown)?.text = "0 Pending · 0 Confirmed · 0 Cancelled"

        // Timeframe selector
        view.findViewById<TextView>(R.id.tv_analytics_timeframe)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filtering live analytics for current period.", Toast.LENGTH_SHORT).show()
        }

        // Toggle pills for Weekly / Monthly
        val pillWeekly = view.findViewById<TextView>(R.id.pill_chart_weekly)
        val pillMonthly = view.findViewById<TextView>(R.id.pill_chart_monthly)

        pillWeekly?.setOnClickListener {
            pillWeekly.setBackgroundResource(R.drawable.bg_chip_selected)
            pillWeekly.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            pillMonthly?.setBackgroundResource(R.drawable.bg_chip_default)
            pillMonthly?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        pillMonthly?.setOnClickListener {
            pillMonthly.setBackgroundResource(R.drawable.bg_chip_selected)
            pillMonthly.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            pillWeekly?.setBackgroundResource(R.drawable.bg_chip_default)
            pillWeekly?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            Toast.makeText(requireContext(), "Switched to monthly booking overview.", Toast.LENGTH_SHORT).show()
        }

        // Real-time listener for Services to dynamically build Popular Services
        servicesListener?.remove()
        servicesListener = ServiceRepository.listenToServicesForBusiness(currentBusinessId) { services ->
            if (!isAdded) return@listenToServicesForBusiness
            latestServices = services
            updatePopularServices(view)
        }

        // Real-time listener to compute live analytics from actual Firestore bookings
        bookingsListener?.remove()
        bookingsListener = BookingRepository.listenToBookingsForBusiness(currentBusinessId) { list ->
            if (!isAdded) return@listenToBookingsForBusiness
            latestBookings = list
            val total = list.size
            val completed = list.count { it.status == "COMPLETED" }
            val pending = list.count { it.status == "PENDING" }
            val confirmed = list.count { it.status == "CONFIRMED" }
            val cancelled = list.count { it.status == "CANCELLED" }

            val completionRate = if (total > 0) ((completed.toDouble() / total) * 100).toInt() else 0

            view.findViewById<TextView>(R.id.tv_analytics_total_bookings)?.text = total.toString()
            view.findViewById<TextView>(R.id.tv_analytics_completed_count)?.text = completed.toString()
            view.findViewById<TextView>(R.id.tv_analytics_completion_rate)?.text = "$completionRate% completion rate"
            view.findViewById<TextView>(R.id.tv_analytics_status_breakdown)?.text =
                "$pending Pending · $confirmed Confirmed · $cancelled Cancelled"

            updatePopularServices(view)
        }
    }

    private fun updatePopularServices(rootView: View) {
        val container = rootView.findViewById<LinearLayout>(R.id.container_popular_services) ?: return
        val tvEmpty = rootView.findViewById<TextView>(R.id.tv_no_popular_services)

        container.removeAllViews()

        // Tally bookings per service name
        val bookingCounts = mutableMapOf<String, Int>()
        for (b in latestBookings) {
            val sName = b.serviceNameSnapshot.trim()
            if (sName.isNotEmpty()) {
                bookingCounts[sName] = (bookingCounts[sName] ?: 0) + 1
            }
        }

        // Combine with actual registered services so even newly added services without bookings appear
        val serviceItems = mutableListOf<Triple<String, Int, Int>>() // Name, count, percentage
        val registeredNames = latestServices.map { it.name.trim() }.toSet()

        for (svc in latestServices) {
            val count = bookingCounts[svc.name.trim()] ?: 0
            serviceItems.add(Triple(svc.name, count, 0))
        }

        // Any bookings with names not currently in latestServices list
        for ((name, count) in bookingCounts) {
            if (!registeredNames.contains(name)) {
                serviceItems.add(Triple(name, count, 0))
            }
        }

        if (serviceItems.isEmpty()) {
            container.visibility = View.GONE
            tvEmpty?.visibility = View.VISIBLE
            tvEmpty?.text = "No services added yet. Go to Services tab to add your offerings."
            return
        }

        // Sort descending by booking count
        serviceItems.sortByDescending { it.second }

        val totalBookings = latestBookings.size
        val topServices = serviceItems.take(5)

        container.visibility = View.VISIBLE
        tvEmpty?.visibility = View.GONE

        val context = requireContext()
        val density = resources.displayMetrics.density

        topServices.forEachIndexed { index, item ->
            val (name, count, _) = item
            val pct = if (totalBookings > 0) ((count.toDouble() / totalBookings) * 100).toInt() else 0

            val rowLayout = RelativeLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (if (index == 0) 12 * density else 16 * density).toInt()
                }
            }

            val tvName = TextView(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                }
                text = name
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val tvStats = TextView(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                }
                text = if (totalBookings > 0) "$count bookings · $pct% of all visits" else "$count bookings · No visits yet"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            rowLayout.addView(tvName)
            rowLayout.addView(tvStats)
            container.addView(rowLayout)

            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (6 * density).toInt()
                ).apply {
                    topMargin = (6 * density).toInt()
                }
                max = 100
                progress = if (totalBookings > 0) pct.coerceAtLeast(4) else 0
                progressTintList = ContextCompat.getColorStateList(
                    context,
                    if (index % 2 == 0) R.color.primary else R.color.secondary
                )
            }
            container.addView(progressBar)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bookingsListener?.remove()
        servicesListener?.remove()
    }
}
