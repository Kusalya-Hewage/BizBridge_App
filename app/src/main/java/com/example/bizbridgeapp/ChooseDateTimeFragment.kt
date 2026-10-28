package com.example.bizbridgeapp

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Booking
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.BusinessRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChooseDateTimeFragment : Fragment() {

    private var businessId: String = ""
    private var businessName: String = ""
    private var serviceId: String = ""
    private var serviceName: String = ""
    private var servicePrice: Double = 45.0
    private var serviceDuration: Int = 45

    // Calendar state
    private val calendarViewMonth = Calendar.getInstance()
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDay: Int = 0
    private var selectedTimeSlot: String = ""

    // Bookings to check slot conflicts
    private var activeBookings: List<Booking> = emptyList()
    private var openingHoursString: String = "9:00 AM - 7:00 PM"

    companion object {
        fun newInstance(
            businessId: String,
            businessName: String,
            serviceId: String,
            serviceName: String,
            servicePrice: Double,
            serviceDuration: Int
        ): ChooseDateTimeFragment {
            val frag = ChooseDateTimeFragment()
            val args = Bundle().apply {
                putString("businessId", businessId)
                putString("businessName", businessName)
                putString("serviceId", serviceId)
                putString("serviceName", serviceName)
                putDouble("servicePrice", servicePrice)
                putInt("serviceDuration", serviceDuration)
            }
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        businessId = arguments?.getString("businessId").orEmpty()
        businessName = arguments?.getString("businessName").orEmpty()
        serviceId = arguments?.getString("serviceId").orEmpty()
        serviceName = arguments?.getString("serviceName").orEmpty()
        servicePrice = arguments?.getDouble("servicePrice") ?: 45.0
        serviceDuration = arguments?.getInt("serviceDuration") ?: 45
        return inflater.inflate(R.layout.fragment_choose_date_time, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize selected date to today (or default to current day)
        val today = Calendar.getInstance()
        selectedYear = today.get(Calendar.YEAR)
        selectedMonth = today.get(Calendar.MONTH)
        selectedDay = today.get(Calendar.DAY_OF_MONTH)

        calendarViewMonth.set(Calendar.YEAR, selectedYear)
        calendarViewMonth.set(Calendar.MONTH, selectedMonth)
        calendarViewMonth.set(Calendar.DAY_OF_MONTH, 1)

        // Header back button
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Bottom action back button
        view.findViewById<Button>(R.id.btn_action_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Service summary text formatted dynamically with exact duration & price
        val tvServiceSummary = view.findViewById<TextView>(R.id.tv_choose_service_summary)
        val priceDisplay = if (servicePrice > 0) "Rs. ${servicePrice.toInt()}" else ""
        val nameDisplay = if (serviceName.isNotBlank()) serviceName else "Service"
        tvServiceSummary?.text = if (priceDisplay.isNotBlank()) {
            "$nameDisplay · $serviceDuration min · $priceDisplay"
        } else {
            "$nameDisplay · $serviceDuration min"
        }

        // Setup Month Navigation (< and >)
        val btnPrevMonth = view.findViewById<TextView>(R.id.btn_prev_month)
        val btnNextMonth = view.findViewById<TextView>(R.id.btn_next_month)

        btnPrevMonth?.setOnClickListener {
            val minCal = Calendar.getInstance()
            if (calendarViewMonth.get(Calendar.YEAR) > minCal.get(Calendar.YEAR) ||
                calendarViewMonth.get(Calendar.MONTH) > minCal.get(Calendar.MONTH)
            ) {
                calendarViewMonth.add(Calendar.MONTH, -1)
                renderCalendar(view)
            } else {
                Toast.makeText(requireContext(), "Past months cannot be selected.", Toast.LENGTH_SHORT).show()
            }
        }

        btnNextMonth?.setOnClickListener {
            calendarViewMonth.add(Calendar.MONTH, 1)
            renderCalendar(view)
        }

        // Continue Button
        val btnContinue = view.findViewById<Button>(R.id.btn_continue)
        btnContinue?.setOnClickListener {
            if (selectedTimeSlot.isBlank()) {
                Toast.makeText(requireContext(), "Please select an available time slot.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val chosenCal = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            val fullDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
            val formattedDate = fullDateFormat.format(chosenCal.time)

            val frag = ReviewBookingFragment.newInstance(
                businessId = businessId,
                businessName = businessName,
                serviceId = serviceId,
                serviceName = serviceName,
                servicePrice = servicePrice,
                serviceDuration = serviceDuration,
                appointmentDate = formattedDate,
                appointmentTime = selectedTimeSlot
            )
            (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
        }

        // Immediate render with initial state so calendar & slots appear instantly
        renderCalendar(view)

        // Fetch Business Hours & Active Bookings to detect slot conflicts in real-time
        loadBusinessAndBookings(view)
    }

    private fun loadBusinessAndBookings(rootView: View) {
        // Fetch opening hours
        if (businessId.isNotBlank()) {
            BusinessRepository.getBusinessById(businessId,
                onSuccess = { biz ->
                    if (!isAdded) return@getBusinessById
                    openingHoursString = biz?.openingHours?.ifBlank { "9:00 AM - 7:00 PM" } ?: "9:00 AM - 7:00 PM"
                    fetchActiveBookings(rootView)
                },
                onFailure = {
                    fetchActiveBookings(rootView)
                }
            )
        } else {
            fetchActiveBookings(rootView)
        }
    }

    private fun fetchActiveBookings(rootView: View) {
        BookingRepository.getActiveBookingsForBusiness(businessId,
            onSuccess = { bookings ->
                if (!isAdded) return@getActiveBookingsForBusiness
                activeBookings = bookings
                renderCalendar(rootView)
            },
            onFailure = {
                if (!isAdded) return@getActiveBookingsForBusiness
                activeBookings = emptyList()
                renderCalendar(rootView)
            }
        )
    }

    /**
     * Renders the calendar month view with 7 weekday columns matching the screenshot.
     */
    private fun renderCalendar(rootView: View) {
        val tvMonthYear = rootView.findViewById<TextView>(R.id.tv_calendar_month_year)
        val gridContainer = rootView.findViewById<LinearLayout>(R.id.ll_calendar_grid_container) ?: return
        gridContainer.removeAllViews()

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        tvMonthYear?.text = monthFormat.format(calendarViewMonth.time)

        val density = resources.displayMetrics.density
        val todayCal = Calendar.getInstance()
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH)
        val currentDay = todayCal.get(Calendar.DAY_OF_MONTH)

        val workingCal = calendarViewMonth.clone() as Calendar
        workingCal.set(Calendar.DAY_OF_MONTH, 1)

        // Monday-based offset (0 = Mon, 1 = Tue, ..., 6 = Sun)
        val firstDayOfWeek = workingCal.get(Calendar.DAY_OF_WEEK)
        val leadingEmptyCells = (firstDayOfWeek + 5) % 7

        val maxDaysInMonth = workingCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        var dayCounter = 1
        val totalCells = leadingEmptyCells + maxDaysInMonth
        val numRows = (totalCells + 6) / 7

        val viewYear = workingCal.get(Calendar.YEAR)
        val viewMonth = workingCal.get(Calendar.MONTH)

        for (row in 0 until numRows) {
            val rowLayout = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (40 * density).toInt()
                ).apply {
                    topMargin = (2 * density).toInt()
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                if (cellIndex < leadingEmptyCells || dayCounter > maxDaysInMonth) {
                    // Empty spacer cell
                    val spacer = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    }
                    rowLayout.addView(spacer)
                } else {
                    val dayNum = dayCounter
                    val isPast = (viewYear < currentYear) ||
                            (viewYear == currentYear && viewMonth < currentMonth) ||
                            (viewYear == currentYear && viewMonth == currentMonth && dayNum < currentDay)

                    val isSelected = (viewYear == selectedYear && viewMonth == selectedMonth && dayNum == selectedDay)

                    val dayContainer = LinearLayout(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                        gravity = Gravity.CENTER
                    }

                    val tvDay = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            (34 * density).toInt(),
                            (34 * density).toInt()
                        )
                        gravity = Gravity.CENTER
                        text = dayNum.toString()
                        textSize = 14f

                        if (isSelected) {
                            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_selected)
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                            setTypeface(typeface, Typeface.BOLD)
                        } else if (isPast) {
                            background = null
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_placeholder))
                            setTypeface(typeface, Typeface.NORMAL)
                        } else {
                            background = null
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                            setTypeface(typeface, Typeface.BOLD)
                        }
                    }

                    if (!isPast) {
                        dayContainer.isClickable = true
                        dayContainer.isFocusable = true
                        dayContainer.setOnClickListener {
                            selectedYear = viewYear
                            selectedMonth = viewMonth
                            selectedDay = dayNum
                            renderCalendar(rootView)
                        }
                    }

                    dayContainer.addView(tvDay)
                    rowLayout.addView(dayContainer)
                    dayCounter++
                }
            }
            gridContainer.addView(rowLayout)
        }

        // Update selected date header and available/blocked time slots
        updateDateAndSlots(rootView)
    }

    /**
     * Updates the date title and regenerates time slots with conflict blocking.
     */
    private fun updateDateAndSlots(rootView: View) {
        val tvSelectedDateHeader = rootView.findViewById<TextView>(R.id.tv_selected_date_header)

        val chosenCal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay)
        }
        val headerFormat = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        tvSelectedDateHeader?.text = headerFormat.format(chosenCal.time)

        renderTimeSlots(rootView)
    }

    /**
     * Generates slot times and marks any slot booked by another customer as blocked / struck-through.
     */
    private fun renderTimeSlots(rootView: View) {
        val container = rootView.findViewById<LinearLayout>(R.id.ll_dynamic_slots_container) ?: return
        container.removeAllViews()

        val density = resources.displayMetrics.density

        // Generate base slots based on business hours
        val generatedSlots = generateSlotList(openingHoursString, serviceDuration)

        val slotViews = mutableListOf<TextView>()
        val columnsPerRow = 3

        var currentRow = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            orientation = LinearLayout.HORIZONTAL
        }


        // Check if currently selected slot is still valid and not blocked
        var firstAvailableSlot: SlotInfo? = null

        for (slot in generatedSlots) {
            val isBooked = isSlotBooked(activeBookings, selectedYear, selectedMonth, selectedDay, slot.startMin, slot.endMin)
            if (!isBooked && firstAvailableSlot == null) {
                firstAvailableSlot = slot
            }
        }

        val currentSlotStillValid = generatedSlots.any {
            it.displaySlot == selectedTimeSlot && !isSlotBooked(activeBookings, selectedYear, selectedMonth, selectedDay, it.startMin, it.endMin)
        }

        if (selectedTimeSlot.isBlank() || !currentSlotStillValid) {
            selectedTimeSlot = firstAvailableSlot?.displaySlot.orEmpty()
        }

        fun updateSlotSelection(clickedSlot: String, clickedView: TextView) {
            selectedTimeSlot = clickedSlot
            for (tv in slotViews) {
                val tagSlot = tv.tag as? String ?: continue
                val booked = !tv.isEnabled

                if (booked) continue

                if (tagSlot == clickedSlot) {
                    tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_selected)
                    tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    tv.setTypeface(tv.typeface, Typeface.BOLD)
                } else {
                    tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_default)
                    tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    tv.setTypeface(tv.typeface, Typeface.NORMAL)
                }
            }
            updateConfirmationBanner(rootView)
        }

        for ((index, slot) in generatedSlots.withIndex()) {
            if (index > 0 && index % columnsPerRow == 0) {
                container.addView(currentRow)
                currentRow = LinearLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (8 * density).toInt()
                    }
                    orientation = LinearLayout.HORIZONTAL
                }
            }

            val isBooked = isSlotBooked(activeBookings, selectedYear, selectedMonth, selectedDay, slot.startMin, slot.endMin)
            val isSelected = (!isBooked && selectedTimeSlot == slot.displaySlot)

            val tvSlot = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    (42 * density).toInt(),
                    1f
                ).apply {
                    if (index % columnsPerRow > 0) marginStart = (8 * density).toInt()
                }
                gravity = Gravity.CENTER
                text = slot.startStr // e.g. "9:00 AM", "9:45 AM", "10:30 AM" dynamic start time
                textSize = 12f
                tag = slot.displaySlot

                if (isBooked) {
                    // BLOCKED / UNAVAILABLE: Struck-through text and disabled background
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_unavailable)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    isEnabled = false
                    isClickable = false
                } else if (isSelected) {
                    // SELECTED: Solid purple pill with white bold text
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_selected)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    setTypeface(typeface, Typeface.BOLD)
                    paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    isEnabled = true
                    isClickable = true
                } else {
                    // AVAILABLE: White pill with subtle border
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_default)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    setTypeface(typeface, Typeface.NORMAL)
                    paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    isEnabled = true
                    isClickable = true
                }

                if (!isBooked) {
                    setOnClickListener {
                        updateSlotSelection(slot.displaySlot, this)
                    }
                }
            }

            slotViews.add(tvSlot)
            currentRow.addView(tvSlot)
        }

        // Fill remaining spaces for row alignment
        if (currentRow.childCount > 0) {
            while (currentRow.childCount < columnsPerRow) {
                val spacer = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, (42 * density).toInt(), 1f).apply {
                        marginStart = (8 * density).toInt()
                    }
                }
                currentRow.addView(spacer)
            }
            container.addView(currentRow)
        }

        updateConfirmationBanner(rootView)
    }

    private fun updateConfirmationBanner(rootView: View) {
        val tvSelectedPill = rootView.findViewById<TextView>(R.id.tv_selected_pill)
        val banner = rootView.findViewById<LinearLayout>(R.id.ll_selected_confirmation_banner)

        if (selectedTimeSlot.isNotBlank()) {
            banner?.visibility = View.VISIBLE
            val chosenCal = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            val shortDateFormat = SimpleDateFormat("EEE, MMM d", Locale.US)
            val dateStr = shortDateFormat.format(chosenCal.time)
            tvSelectedPill?.text = "Selected: $dateStr · $selectedTimeSlot"
        } else {
            banner?.visibility = View.VISIBLE
            tvSelectedPill?.text = "No available slots on this date. Please choose another date."
        }
    }

    data class SlotInfo(
        val startStr: String,
        val endStr: String,
        val displaySlot: String,
        val startMin: Int,
        val endMin: Int
    )

    /**
     * Generates slots strictly adhering to the service duration and business operating hours.
     * E.g. for a 45 min service: 9:00 AM, 9:45 AM, 10:30 AM, 11:15 AM...
     * E.g. for a 30 min service: 9:00 AM, 9:30 AM, 10:00 AM, 10:30 AM...
     */
    private fun generateSlotList(openingHours: String, durationMin: Int): List<SlotInfo> {
        val safeDuration = durationMin.coerceIn(15, 240)
        var startMin = 9 * 60 // 9:00 AM
        var endMin = 19 * 60  // 7:00 PM

        val timePattern = "(\\d{1,2}):?(\\d{2})?\\s*(AM|PM|am|pm)?".toRegex()
        val matches = timePattern.findAll(openingHours).toList()
        if (matches.size >= 2) {
            fun parseToMin(m: MatchResult): Int {
                val hour = m.groupValues[1].toIntOrNull() ?: 9
                val min = m.groupValues[2].toIntOrNull() ?: 0
                val ampm = m.groupValues[3].uppercase()
                var total = (hour % 12) * 60 + min
                if (ampm == "PM") total += 12 * 60
                return total
            }
            try {
                val pStart = parseToMin(matches[0])
                val pEnd = parseToMin(matches[1])
                if (pEnd > pStart && (pEnd - pStart) >= safeDuration) {
                    startMin = pStart
                    endMin = pEnd
                }
            } catch (_: Exception) {}
        }

        val slots = mutableListOf<SlotInfo>()
        var current = startMin
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        val cal = Calendar.getInstance()

        while (current + safeDuration <= endMin) {
            val sMin = current
            val eMin = current + safeDuration

            cal.set(Calendar.HOUR_OF_DAY, sMin / 60)
            cal.set(Calendar.MINUTE, sMin % 60)
            val startStr = timeFormat.format(cal.time)

            cal.set(Calendar.HOUR_OF_DAY, eMin / 60)
            cal.set(Calendar.MINUTE, eMin % 60)
            val endStr = timeFormat.format(cal.time)

            slots.add(SlotInfo(
                startStr = startStr,
                endStr = endStr,
                displaySlot = "$startStr–$endStr",
                startMin = sMin,
                endMin = eMin
            ))

            // Step dynamically by the exact service duration
            current += safeDuration
        }

        if (slots.isEmpty()) {
            var c = 9 * 60
            while (c + safeDuration <= 18 * 60) {
                val sMin = c
                val eMin = c + safeDuration
                cal.set(Calendar.HOUR_OF_DAY, sMin / 60)
                cal.set(Calendar.MINUTE, sMin % 60)
                val sStr = timeFormat.format(cal.time)

                cal.set(Calendar.HOUR_OF_DAY, eMin / 60)
                cal.set(Calendar.MINUTE, eMin % 60)
                val eStr = timeFormat.format(cal.time)

                slots.add(SlotInfo(
                    startStr = sStr,
                    endStr = eStr,
                    displaySlot = "$sStr–$eStr",
                    startMin = sMin,
                    endMin = eMin
                ))
                c += safeDuration
            }
        }

        return slots
    }

    /**
     * Determines if a slot time interval [slotStartMin, slotEndMin] on a given date
     * conflicts with any active booking (PENDING or CONFIRMED) or is in the past.
     */
    private fun isSlotBooked(
        bookings: List<Booking>,
        year: Int,
        month: Int, // 0-based
        day: Int,
        slotStartMin: Int,
        slotEndMin: Int
    ): Boolean {
        val today = Calendar.getInstance()
        val isToday = (year == today.get(Calendar.YEAR) &&
                month == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH))

        // If today, block past time slots
        if (isToday) {
            val currentMinNow = today.get(Calendar.HOUR_OF_DAY) * 60 + today.get(Calendar.MINUTE)
            if (slotStartMin <= currentMinNow) {
                return true
            }
        }

        return bookings.any { b ->
            if (b.status != "PENDING" && b.status != "CONFIRMED") return@any false

            if (!isSameDay(b.appointmentDate, year, month, day)) return@any false

            val (bStart, bEnd) = parseBookingWindow(b.appointmentTime, b.durationMinutes) ?: return@any false

            // Standard interval overlap: [slotStartMin, slotEndMin] overlaps with [bStart, bEnd]
            (slotStartMin < bEnd && slotEndMin > bStart)
        }
    }

    /**
     * Checks if the booking's appointment date string matches the target year, month, day.
     */
    private fun isSameDay(bookingDateStr: String, year: Int, month: Int, day: Int): Boolean {
        val bLower = bookingDateStr.trim().lowercase(Locale.US)
        if (bLower.isBlank()) return false

        val cal = Calendar.getInstance().apply { set(year, month, day) }
        val fullDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(cal.time).lowercase(Locale.US)
        val medDate = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(cal.time).lowercase(Locale.US)
        val shortDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(cal.time).lowercase(Locale.US)
        val isoDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
        val slashDate1 = String.format(Locale.US, "%d/%d/%d", month + 1, day, year)
        val slashDate2 = String.format(Locale.US, "%02d/%02d/%04d", month + 1, day, year)

        if (bLower == fullDate || bLower == medDate || bLower == shortDate || bLower == isoDate || bLower == slashDate1 || bLower == slashDate2) {
            return true
        }

        val monthShort = SimpleDateFormat("MMM", Locale.US).format(cal.time).lowercase(Locale.US)
        val monthFull = SimpleDateFormat("MMMM", Locale.US).format(cal.time).lowercase(Locale.US)
        val hasMonth = bLower.contains(monthShort) || bLower.contains(monthFull)
        val hasYear = bLower.contains(year.toString())
        val dayRegex = "(?:^|\\D)$day(?:\\D|$)".toRegex()
        val hasDay = dayRegex.containsMatchIn(bLower)

        return (hasMonth && hasDay && hasYear) || bLower.contains(isoDate)
    }

    /**
     * Parses an appointment time string (e.g. "10:00–10:45 AM" or "2:30 PM") into start and end minute integers.
     */
    private fun parseBookingWindow(appointmentTime: String, durationMinutes: Int): Pair<Int, Int>? {
        val timePattern = "(\\d{1,2}):(\\d{2})\\s*(AM|PM|am|pm)?".toRegex()
        val matches = timePattern.findAll(appointmentTime).toList()
        if (matches.isEmpty()) return null

        val overallAmPm = if (appointmentTime.contains("PM", ignoreCase = true)) "PM" else "AM"

        fun parseMatch(m: MatchResult, fallbackAmPm: String): Int {
            var h = m.groupValues[1].toIntOrNull() ?: 9
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val ampm = m.groupValues[3].ifBlank { fallbackAmPm }.uppercase()
            if (ampm == "PM" && h < 12) h += 12
            if (ampm == "AM" && h == 12) h = 0
            return h * 60 + min
        }

        val safeDuration = if (durationMinutes > 0) durationMinutes else 30

        if (matches.size >= 2) {
            val endAmPm = matches[1].groupValues[3].ifBlank { overallAmPm }
            val startAmPm = matches[0].groupValues[3].ifBlank {
                val h0 = matches[0].groupValues[1].toIntOrNull() ?: 9
                val h1 = matches[1].groupValues[1].toIntOrNull() ?: 9
                if (endAmPm.uppercase() == "PM" && h0 > h1 && h0 != 12) "AM" else endAmPm
            }
            val start = parseMatch(matches[0], startAmPm)
            val end = parseMatch(matches[1], endAmPm)
            if (end > start) return Pair(start, end)
            return Pair(start, start + safeDuration)
        } else {
            val start = parseMatch(matches[0], overallAmPm)
            return Pair(start, start + safeDuration)
        }
    }
}
