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

        // Service summary text
        val tvServiceSummary = view.findViewById<TextView>(R.id.tv_choose_service_summary)
        val priceDisplay = if (servicePrice > 0) "$${servicePrice.toInt()}" else "$45"
        val nameDisplay = if (serviceName.isNotBlank()) serviceName else "Signature haircut"
        tvServiceSummary?.text = "$nameDisplay · $serviceDuration min · $priceDisplay"

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
        var firstAvailableSlot: String? = null

        for (slot in generatedSlots) {
            val isBooked = isSlotBooked(activeBookings, selectedYear, selectedMonth, selectedDay, slot.startStr)
            if (!isBooked && firstAvailableSlot == null) {
                firstAvailableSlot = slot.displaySlot
            }
        }

        if (selectedTimeSlot.isBlank() || isSlotBooked(activeBookings, selectedYear, selectedMonth, selectedDay, selectedTimeSlot)) {
            selectedTimeSlot = firstAvailableSlot.orEmpty()
        }

        fun updateSlotSelection(clickedSlot: String, clickedView: TextView) {
            selectedTimeSlot = clickedSlot
            for (tv in slotViews) {
                val tagSlot = tv.tag as? String ?: continue
                val booked = tv.id == View.NO_ID && !tv.isEnabled

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

            val isBooked = isSlotBooked(activeBookings, selectedYear, selectedMonth, selectedDay, slot.startStr)
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
                text = slot.startStr // e.g. "9:00 AM", "10:00 AM", "11:00 AM" matching screenshot
                textSize = 13f
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
        val displaySlot: String
    )

    private fun generateSlotList(openingHours: String, durationMin: Int): List<SlotInfo> {
        val safeDuration = durationMin.coerceIn(15, 180)
        var startMin = 9 * 60 // 9:00 AM
        var endMin = 18 * 60  // 6:00 PM

        val timePattern = "(\\d{1,2}):?(\\d{2})?\\s*(AM|PM|am|pm)?".toRegex()
        val matches = timePattern.findAll(openingHours).toList()
        if (matches.size >= 2) {
            fun parseToMin(m: MatchResult): Int {
                val hour = m.groupValues[1].toIntOrNull() ?: 9
                val min = m.groupValues[2].toIntOrNull() ?: 0
                val ampm = m.groupValues[3].uppercase()
                var total = hour % 12 * 60 + min
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
            cal.set(Calendar.HOUR_OF_DAY, current / 60)
            cal.set(Calendar.MINUTE, current % 60)
            val startStr = timeFormat.format(cal.time)

            cal.set(Calendar.HOUR_OF_DAY, (current + safeDuration) / 60)
            cal.set(Calendar.MINUTE, (current + safeDuration) % 60)
            val endStr = timeFormat.format(cal.time)

            slots.add(SlotInfo(startStr, endStr, "$startStr–$endStr"))

            // Advance by service duration or 60 min intervals to match screenshot standard slots
            val step = if (safeDuration <= 45) 60 else safeDuration
            current += step
        }

        if (slots.isEmpty()) {
            slots.add(SlotInfo("9:00 AM", "9:45 AM", "9:00–9:45 AM"))
            slots.add(SlotInfo("10:00 AM", "10:45 AM", "10:00–10:45 AM"))
            slots.add(SlotInfo("11:00 AM", "11:45 AM", "11:00–11:45 AM"))
            slots.add(SlotInfo("1:00 PM", "1:45 PM", "1:00–1:45 PM"))
            slots.add(SlotInfo("2:00 PM", "2:45 PM", "2:00–2:45 PM"))
            slots.add(SlotInfo("3:00 PM", "3:45 PM", "3:00–3:45 PM"))
        }

        return slots
    }

    /**
     * Determines if a slot on a given date is already reserved by another customer.
     */
    private fun isSlotBooked(
        bookings: List<Booking>,
        year: Int,
        month: Int, // 0-based
        day: Int,
        slotStart: String
    ): Boolean {
        val testCal = Calendar.getInstance().apply {
            set(year, month, day)
        }
        val monthShort = SimpleDateFormat("MMM", Locale.US).format(testCal.time).lowercase()
        val monthFull = SimpleDateFormat("MMMM", Locale.US).format(testCal.time).lowercase()
        val isoDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
        val slashDate = String.format(Locale.US, "%d/%d/%d", month + 1, day, year)

        val cleanSlot = slotStart.split("–", "-", " ").firstOrNull()?.trim()?.lowercase() ?: ""
        val slotAmPm = if (slotStart.uppercase().contains("PM")) "pm" else "am"

        return bookings.any { b ->
            if (b.status != "PENDING" && b.status != "CONFIRMED") return@any false

            val bDate = b.appointmentDate.lowercase()
            val dateMatches = (bDate.contains(day.toString()) && (bDate.contains(monthShort) || bDate.contains(monthFull))) ||
                    bDate.contains(isoDate) ||
                    bDate.contains(slashDate)

            if (!dateMatches) return@any false

            val bTime = b.appointmentTime.lowercase()
            val bCleanSlot = bTime.split("–", "-", " ").firstOrNull()?.trim() ?: ""
            val bAmPm = if (bTime.uppercase().contains("PM")) "pm" else "am"

            (bCleanSlot == cleanSlot && bAmPm == slotAmPm) || bTime.contains(cleanSlot)
        }
    }
}
