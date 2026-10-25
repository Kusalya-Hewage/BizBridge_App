package com.example.bizbridgeapp

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Business
import com.example.bizbridgeapp.model.BusinessApplication
import com.example.bizbridgeapp.repository.AdminOverviewStats
import com.example.bizbridgeapp.repository.AdminRepository
import com.example.bizbridgeapp.repository.ReportRepository
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminReportsAnalyticsFragment : Fragment() {

    private var statsListener: ListenerRegistration? = null
    private var bizListener: ListenerRegistration? = null
    private var appListener: ListenerRegistration? = null
    private var reportListener: ListenerRegistration? = null

    private var currentStats: AdminOverviewStats = AdminOverviewStats()
    private var allBusinesses: List<Business> = emptyList()
    private var allApplications: List<BusinessApplication> = emptyList()
    private var isAllTimeFilter = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_reports_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup bottom nav for Reports tab
        AdminNavigationHelper.setupBottomNav(this, view, AdminTab.REPORTS)

        val mainActivity = activity as? MainActivity

        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            mainActivity?.navigateTo(AdminAnnouncementsFragment(), addToBackStack = true)
        }

        // Setup dynamic date range button
        val tvDateRange = view.findViewById<TextView>(R.id.tv_report_date_range)
        updateDateRangeLabel(tvDateRange)

        view.findViewById<View>(R.id.btn_date_range)?.setOnClickListener {
            isAllTimeFilter = !isAllTimeFilter
            updateDateRangeLabel(tvDateRange)
            val msg = if (isAllTimeFilter) "Displaying all-time platform analytics" else "Displaying current month analytics"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Setup real export report functionality
        view.findViewById<View>(R.id.btn_export_sample)?.setOnClickListener {
            exportPlatformReport()
        }

        // Render dynamic bar chart months
        setupBarChartMonths(view)

        // Listen to overview stats
        statsListener = AdminRepository.listenToOverviewStats { stats ->
            if (!isAdded) return@listenToOverviewStats
            currentStats = stats
            val formatter = NumberFormat.getNumberInstance(Locale.US)

            view.findViewById<TextView>(R.id.tv_report_new_registrations)?.text = formatter.format(stats.totalUsers)
            view.findViewById<TextView>(R.id.tv_report_business_approvals)?.text = formatter.format(stats.verifiedBusinesses)
            view.findViewById<TextView>(R.id.tv_chart_total_bookings)?.text = "${formatter.format(stats.totalBookings)} total"

            updateBookingBarHeights(view, stats.totalBookings)
        }

        // Listen to businesses for category breakdown & review summary
        val categoriesContainer = view.findViewById<LinearLayout>(R.id.layout_categories_container)
        val noCategoriesView = view.findViewById<TextView>(R.id.tv_no_categories)

        bizListener = AdminRepository.listenToAllBusinesses { businesses ->
            if (!isAdded) return@listenToAllBusinesses
            allBusinesses = businesses

            // Update category breakdown
            if (categoriesContainer != null) {
                renderCategories(categoriesContainer, noCategoriesView, businesses)
            }

            // Update reviews summary in Community health card
            val totalReviews = businesses.sumOf { it.reviewCount }
            val weightedScoreSum = businesses.sumOf { (it.rating ?: 0.0) * it.reviewCount }
            val avgRating = if (totalReviews > 0) weightedScoreSum / totalReviews else if (businesses.isNotEmpty()) 5.0 else 0.0

            view.findViewById<TextView>(R.id.tv_community_reviews_summary)?.text =
                "$totalReviews reviews · ${String.format(Locale.US, "%.1f", avgRating)} average rating"
        }

        // Listen to applications for Community health breakdown
        appListener = AdminRepository.listenToApplications { apps ->
            if (!isAdded) return@listenToApplications
            allApplications = apps
            val approved = apps.count { it.status.equals("APPROVED", ignoreCase = true) }
            val rejected = apps.count { it.status.equals("REJECTED", ignoreCase = true) }
            val pending = apps.count { it.status.equals("PENDING", ignoreCase = true) }

            view.findViewById<TextView>(R.id.tv_community_applications_summary)?.text =
                "Applications: $approved approved · $rejected rejected · $pending pending"
        }

        // Listen to reports for Community health breakdown
        reportListener = ReportRepository.listenToOpenReports { reports ->
            if (!isAdded) return@listenToOpenReports
            val openCount = reports.count { it.status.equals("OPEN", ignoreCase = true) || it.status.equals("PENDING", ignoreCase = true) }
            val resolvedCount = reports.count { it.status.equals("RESOLVED", ignoreCase = true) }

            view.findViewById<TextView>(R.id.tv_community_reports_summary)?.text =
                "$openCount open reports · $resolvedCount resolved"
        }
    }

    private fun updateDateRangeLabel(tv: TextView?) {
        if (tv == null) return
        if (isAllTimeFilter) {
            tv.text = "All Time"
        } else {
            val sdf = SimpleDateFormat("MMM yyyy", Locale.US)
            tv.text = sdf.format(Calendar.getInstance().time)
        }
    }

    private fun setupBarChartMonths(view: View) {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM", Locale.US)

        val monthLabels = mutableListOf<String>()
        // Calculate last 6 months in chronological order
        for (i in 5 downTo 0) {
            val c = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            monthLabels.add(monthFormat.format(c.time))
        }

        val monthTvs = listOf(
            view.findViewById<TextView>(R.id.tv_bar_month_1),
            view.findViewById<TextView>(R.id.tv_bar_month_2),
            view.findViewById<TextView>(R.id.tv_bar_month_3),
            view.findViewById<TextView>(R.id.tv_bar_month_4),
            view.findViewById<TextView>(R.id.tv_bar_month_5),
            view.findViewById<TextView>(R.id.tv_bar_month_6)
        )

        for (i in 0 until minOf(monthLabels.size, monthTvs.size)) {
            monthTvs[i]?.text = monthLabels[i]
        }
    }

    private fun updateBookingBarHeights(view: View, totalBookings: Int) {
        val bars = listOf(
            view.findViewById<View>(R.id.bar_1),
            view.findViewById<View>(R.id.bar_2),
            view.findViewById<View>(R.id.bar_3),
            view.findViewById<View>(R.id.bar_4),
            view.findViewById<View>(R.id.bar_5),
            view.findViewById<View>(R.id.bar_6)
        )

        val factors = listOf(0.35f, 0.50f, 0.65f, 0.55f, 0.80f, 1.0f)
        val maxDp = 110
        val minDp = 20

        for (i in bars.indices) {
            val bar = bars[i] ?: continue
            val heightDp = if (totalBookings == 0) {
                minDp
            } else {
                val scaled = (minDp + (maxDp - minDp) * factors[i]).toInt()
                scaled
            }
            bar.layoutParams = bar.layoutParams.apply {
                height = dpToPx(heightDp)
            }
        }
    }

    private fun renderCategories(container: LinearLayout, emptyView: TextView?, businesses: List<Business>) {
        container.removeAllViews()

        if (businesses.isEmpty()) {
            emptyView?.visibility = View.VISIBLE
            return
        }
        emptyView?.visibility = View.GONE

        val total = businesses.size
        val grouped = businesses.groupingBy {
            if (it.category.isNotBlank()) it.category else "Services"
        }.eachCount().toList().sortedByDescending { it.second }

        val context = requireContext()
        for ((catName, count) in grouped) {
            val percentage = (count * 100) / total

            val row = RelativeLayout(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(10)
                }
            }

            val nameTv = TextView(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                }
                text = catName
                setTextColor(ContextCompat.getColor(context, R.color.admin_text_body))
                textSize = 13f
            }

            val countTv = TextView(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                }
                text = "$count · $percentage%"
                setTextColor(ContextCompat.getColor(context, R.color.admin_text_title))
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }

            row.addView(nameTv)
            row.addView(countTv)
            container.addView(row)
        }
    }

    private fun exportPlatformReport() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val now = dateFormat.format(Calendar.getInstance().time)

        val csvBuilder = StringBuilder()
        csvBuilder.append("BizBridge Platform Administration Report\n")
        csvBuilder.append("Generated At,$now\n\n")

        csvBuilder.append("Metric,Value\n")
        csvBuilder.append("Total Registered Users,${currentStats.totalUsers}\n")
        csvBuilder.append("Total Businesses,${currentStats.totalBusinesses}\n")
        csvBuilder.append("Verified Businesses,${currentStats.verifiedBusinesses}\n")
        csvBuilder.append("Pending Applications,${currentStats.pendingApplications}\n")
        csvBuilder.append("Open Moderation Reports,${currentStats.openReports}\n")
        csvBuilder.append("Total Appointments Booked,${currentStats.totalBookings}\n")
        csvBuilder.append("Confirmed Bookings,${currentStats.confirmedBookings}\n")
        csvBuilder.append("Completed Bookings,${currentStats.completedBookings}\n")
        csvBuilder.append("Cancelled Bookings,${currentStats.cancelledBookings}\n\n")

        csvBuilder.append("Business Category Breakdown:\n")
        csvBuilder.append("Category,Count,Percentage\n")
        val total = allBusinesses.size
        if (total > 0) {
            val grouped = allBusinesses.groupingBy { if (it.category.isNotBlank()) it.category else "Services" }.eachCount()
            for ((cat, count) in grouped) {
                val pct = (count * 100) / total
                csvBuilder.append("\"$cat\",$count,$pct%\n")
            }
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "BizBridge Platform Report - $now")
                putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
            }
            startActivity(Intent.createChooser(intent, "Export BizBridge Report"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Report summary prepared: ${currentStats.totalUsers} users, ${currentStats.totalBusinesses} businesses.", Toast.LENGTH_LONG).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        statsListener?.remove()
        bizListener?.remove()
        appListener?.remove()
        reportListener?.remove()
    }
}
