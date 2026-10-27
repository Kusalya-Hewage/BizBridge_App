package com.example.bizbridgeapp

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.BusinessRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChooseDateTimeFragment : Fragment() {

    private var businessId: String = ""
    private var businessName: String = ""
    private var serviceId: String = ""
    private var serviceName: String = ""
    private var servicePrice: Double = 0.0
    private var serviceDuration: Int = 45
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    private val calendarDates = mutableListOf<CalendarDateItem>()
    private var selectedDateIndex = 0
    private val slotViews = mutableListOf<TextView>()

    data class CalendarDateItem(
        val fullDateString: String,
        val dayOfWeek: String,
        val dayOfMonth: String,
        val monthShort: String
    )

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
            val args = Bundle()
            args.putString("businessId", businessId)
            args.putString("businessName", businessName)
            args.putString("serviceId", serviceId)
            args.putString("serviceName", serviceName)
            args.putDouble("servicePrice", servicePrice)
            args.putInt("serviceDuration", serviceDuration)
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
        servicePrice = arguments?.getDouble("servicePrice") ?: 0.0
        serviceDuration = arguments?.getInt("serviceDuration") ?: 45
        return inflater.inflate(R.layout.fragment_choose_date_time, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Header back
        view.findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Action back
        view.findViewById<Button>(R.id.btn_action_back)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<TextView>(R.id.tv_choose_service_summary)?.text =
            "$serviceName • $serviceDuration min • $${servicePrice.toInt()}"

        // Generate upcoming 10 days starting from today
        generateUpcomingDates()
        setupDatePicker(view)

        // Date Picker Dialog from Calendar button
        view.findViewById<View>(R.id.btn_open_calendar_picker)?.setOnClickListener {
            val cal = Calendar.getInstance()
            val dpd = android.app.DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val chosenCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    val fullFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
                    val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.US)
                    val dayOfMonthFormat = SimpleDateFormat("d", Locale.US)
                    val monthFormat = SimpleDateFormat("MMM", Locale.US)

                    val full = fullFormat.format(chosenCal.time)
                    val dow = dayOfWeekFormat.format(chosenCal.time)
                    val dom = dayOfMonthFormat.format(chosenCal.time)
                    val mon = monthFormat.format(chosenCal.time)

                    val existingIndex = calendarDates.indexOfFirst { it.fullDateString == full }
                    if (existingIndex >= 0) {
                        selectedDateIndex = existingIndex
                        setupDatePicker(view)
                    } else {
                        calendarDates.add(0, CalendarDateItem(full, dow, dom, mon))
                        selectedDateIndex = 0
                        setupDatePicker(view)
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dpd.datePicker.minDate = System.currentTimeMillis() - 1000
            dpd.show()
        }

        // Custom Time Picker
        view.findViewById<View>(R.id.btn_custom_time_picker)?.setOnClickListener {
            val cal = Calendar.getInstance()
            val tpd = android.app.TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val startCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }
                    val endCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute + serviceDuration)
                    }
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
                    val customSlot = "${timeFormat.format(startCal.time)}–${timeFormat.format(endCal.time)}"
                    selectedTime = customSlot

                    for (tv in slotViews) {
                        tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_default)
                        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                        tv.setTypeface(tv.typeface, android.graphics.Typeface.NORMAL)
                    }
                    updateSelectedPill(view)
                    android.widget.Toast.makeText(requireContext(), "Selected custom time: $customSlot", android.widget.Toast.LENGTH_SHORT).show()
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false
            )
            tpd.show()
        }

        // Fetch business opening hours and generate dynamic slots
        loadBusinessAndGenerateSlots(view)

        // Continue button
        val btnContinue = view.findViewById<Button>(R.id.btn_continue)
        btnContinue?.setOnClickListener {
            if (selectedDate.isBlank() || selectedTime.isBlank()) {
                android.widget.Toast.makeText(requireContext(), "Please select an available date and time slot.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val frag = ReviewBookingFragment.newInstance(
                businessId = businessId,
                businessName = businessName,
                serviceId = serviceId,
                serviceName = serviceName,
                servicePrice = servicePrice,
                serviceDuration = serviceDuration,
                appointmentDate = selectedDate,
                appointmentTime = selectedTime
            )
            (activity as? MainActivity)?.navigateTo(frag, addToBackStack = true)
        }
    }

    private fun generateUpcomingDates() {
        calendarDates.clear()
        val cal = Calendar.getInstance()
        val fullFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.US)
        val dayOfMonthFormat = SimpleDateFormat("d", Locale.US)
        val monthFormat = SimpleDateFormat("MMM", Locale.US)

        for (i in 0 until 10) {
            val full = fullFormat.format(cal.time)
            val dow = if (i == 0) "Today" else if (i == 1) "Tmrw" else dayOfWeekFormat.format(cal.time)
            val dom = dayOfMonthFormat.format(cal.time)
            val mon = monthFormat.format(cal.time)
            calendarDates.add(CalendarDateItem(full, dow, dom, mon))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun setupDatePicker(rootView: View) {
        val container = rootView.findViewById<LinearLayout>(R.id.ll_dates_container) ?: return
        val tvCalendarMonth = rootView.findViewById<TextView>(R.id.tv_calendar_month)
        val tvSelectedDateTitle = rootView.findViewById<TextView>(R.id.tv_selected_date_title)
        val density = resources.displayMetrics.density

        container.removeAllViews()

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        tvCalendarMonth?.text = monthFormat.format(Calendar.getInstance().time)

        val pillViews = mutableListOf<LinearLayout>()

        fun selectDate(index: Int) {
            selectedDateIndex = index
            val item = calendarDates[index]
            selectedDate = item.fullDateString
            tvSelectedDateTitle?.text = item.fullDateString

            for (i in pillViews.indices) {
                val pill = pillViews[i]
                val tvDay = pill.getChildAt(0) as? TextView
                val tvDate = pill.getChildAt(1) as? TextView
                val tvMonth = pill.getChildAt(2) as? TextView

                if (i == index) {
                    pill.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_selected)
                    tvDay?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    tvDate?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    tvMonth?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    pill.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_default)
                    tvDay?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    tvDate?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    tvMonth?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
            }

            updateSelectedPill(rootView)
        }

        for ((index, item) in calendarDates.withIndex()) {
            val pill = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (68 * density).toInt(),
                    (80 * density).toInt()
                ).apply {
                    marginEnd = (10 * density).toInt()
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_default)
                elevation = (1 * density)
                val pad = (6 * density).toInt()
                setPadding(pad, pad, pad, pad)
                isClickable = true
                isFocusable = true
            }

            val tvDay = TextView(requireContext()).apply {
                text = item.dayOfWeek.uppercase()
                textSize = 10f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }

            val tvDate = TextView(requireContext()).apply {
                text = item.dayOfMonth
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }

            val tvMonth = TextView(requireContext()).apply {
                text = item.monthShort
                textSize = 11f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }

            pill.addView(tvDay)
            pill.addView(tvDate)
            pill.addView(tvMonth)

            pill.setOnClickListener {
                selectDate(index)
            }

            pillViews.add(pill)
            container.addView(pill)
        }

        if (calendarDates.isNotEmpty()) {
            selectDate(0)
        }
    }

    private fun loadBusinessAndGenerateSlots(rootView: View) {
        val tvHint = rootView.findViewById<TextView>(R.id.tv_business_hours_hint)

        if (businessId.isNotBlank()) {
            BusinessRepository.getBusinessById(businessId,
                onSuccess = { biz ->
                    if (!isAdded) return@getBusinessById
                    val hours = biz?.openingHours?.ifBlank { "9:00 AM - 7:00 PM" } ?: "9:00 AM - 7:00 PM"
                    tvHint?.text = hours
                    generateTimeSlots(rootView, hours, serviceDuration)
                },
                onFailure = {
                    val defaultHours = "9:00 AM - 7:00 PM"
                    tvHint?.text = defaultHours
                    generateTimeSlots(rootView, defaultHours, serviceDuration)
                }
            )
        } else {
            val defaultHours = "9:00 AM - 7:00 PM"
            tvHint?.text = defaultHours
            generateTimeSlots(rootView, defaultHours, serviceDuration)
        }
    }

    /**
     * Dynamically generates booking time slots based on business open/close hours and service duration.
     * Decided by the business owner!
     */
    private fun generateTimeSlots(rootView: View, openingHours: String, durationMin: Int) {
        val container = rootView.findViewById<LinearLayout>(R.id.ll_dynamic_slots_container) ?: return
        container.removeAllViews()

        val safeDuration = durationMin.coerceIn(15, 240)
        val density = resources.displayMetrics.density

        // Determine start and end hours (default 9:00 AM to 6:30 PM)
        var startMin = 9 * 60 // 9:00 AM in minutes
        var endMin = 18 * 60 + 30 // 6:30 PM in minutes

        val timePattern = "(\\d{1,2}):?(\\d{2})?\\s*(AM|PM|am|pm)?".toRegex()
        val matches = timePattern.findAll(openingHours).toList()
        if (matches.size >= 2) {
            fun parseToMinutes(m: MatchResult): Int {
                val hour = m.groupValues[1].toIntOrNull() ?: 9
                val min = m.groupValues[2].toIntOrNull() ?: 0
                val ampm = m.groupValues[3].uppercase()
                var total = hour % 12 * 60 + min
                if (ampm == "PM") total += 12 * 60
                return total
            }
            try {
                val parsedStart = parseToMinutes(matches[0])
                val parsedEnd = parseToMinutes(matches[1])
                if (parsedEnd > parsedStart && (parsedEnd - parsedStart) >= safeDuration) {
                    startMin = parsedStart
                    endMin = parsedEnd
                }
            } catch (_: Exception) {}
        }

        // Generate dynamic slots list
        val slotsList = mutableListOf<String>()
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

            slotsList.add("$startStr–$endStr")
            current += safeDuration
        }

        if (slotsList.isEmpty()) {
            slotsList.add("9:00–9:45 AM")
            slotsList.add("10:00–10:45 AM")
            slotsList.add("11:00–11:45 AM")
            slotsList.add("2:00–2:45 PM")
            slotsList.add("3:00–3:45 PM")
        }

        // Render slots in rows of 2 or 3
        slotViews.clear()
        val columnsPerRow = if (safeDuration > 60) 2 else 3

        fun selectSlot(slotText: String, clickedView: TextView) {
            selectedTime = slotText
            for (tv in slotViews) {
                if (tv == clickedView) {
                    tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_selected)
                    tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
                } else {
                    tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_default)
                    tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    tv.setTypeface(tv.typeface, android.graphics.Typeface.NORMAL)
                }
            }
            updateSelectedPill(rootView)
        }

        var currentRow = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * density).toInt()
            }
            orientation = LinearLayout.HORIZONTAL
        }

        for ((index, slot) in slotsList.withIndex()) {
            if (index > 0 && index % columnsPerRow == 0) {
                container.addView(currentRow)
                currentRow = LinearLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (10 * density).toInt()
                    }
                    orientation = LinearLayout.HORIZONTAL
                }
            }

            val tvSlot = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    (42 * density).toInt(),
                    1f
                ).apply {
                    if (index % columnsPerRow > 0) marginStart = (8 * density).toInt()
                }
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_default)
                gravity = Gravity.CENTER
                text = slot
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                isClickable = true
                isFocusable = true
            }

            tvSlot.setOnClickListener {
                selectSlot(slot, tvSlot)
            }

            slotViews.add(tvSlot)
            currentRow.addView(tvSlot)
        }

        // Add remaining row
        if (currentRow.childCount > 0) {
            // Fill empty cells if needed for alignment
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

        // Default select first available slot
        if (slotViews.isNotEmpty()) {
            selectSlot(slotsList[0], slotViews[0])
        }
    }

    private fun updateSelectedPill(rootView: View) {
        val tvSelectedPill = rootView.findViewById<TextView>(R.id.tv_selected_pill)
        if (selectedDate.isNotBlank() && selectedTime.isNotBlank()) {
            val shortDate = calendarDates.getOrNull(selectedDateIndex)?.let {
                "${it.dayOfWeek}, ${it.monthShort} ${it.dayOfMonth}"
            } ?: selectedDate
            tvSelectedPill?.text = "Selected: $shortDate • $selectedTime"
        }
    }
}
