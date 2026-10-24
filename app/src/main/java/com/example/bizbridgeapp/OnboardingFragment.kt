package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class OnboardingFragment : Fragment() {

    private var currentStep = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chipStep1 = view.findViewById<TextView>(R.id.chip_step_1)
        val chipStep2 = view.findViewById<TextView>(R.id.chip_step_2)
        val chipStep3 = view.findViewById<TextView>(R.id.chip_step_3)
        val btnTopBack = view.findViewById<ImageView>(R.id.btn_top_back)
        val btnSecondary = view.findViewById<Button>(R.id.btn_secondary)
        val btnPrimary = view.findViewById<Button>(R.id.btn_primary)
        val btnGetStarted = view.findViewById<Button>(R.id.btn_get_started)

        updateStepUI(view, 1)

        chipStep1?.setOnClickListener { updateStepUI(view, 1) }
        chipStep2?.setOnClickListener { updateStepUI(view, 2) }
        chipStep3?.setOnClickListener { updateStepUI(view, 3) }

        btnTopBack?.setOnClickListener {
            if (currentStep > 1) {
                updateStepUI(view, currentStep - 1)
            }
        }

        btnSecondary?.setOnClickListener {
            if (currentStep == 1) {
                navigateToLogin()
            } else if (currentStep == 2) {
                updateStepUI(view, 1)
            }
        }

        btnPrimary?.setOnClickListener {
            if (currentStep < 3) {
                updateStepUI(view, currentStep + 1)
            } else {
                navigateToLogin()
            }
        }

        btnGetStarted?.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun updateStepUI(root: View, step: Int) {
        currentStep = step
        val context = requireContext()

        val chipStep1 = root.findViewById<TextView>(R.id.chip_step_1)
        val chipStep2 = root.findViewById<TextView>(R.id.chip_step_2)
        val chipStep3 = root.findViewById<TextView>(R.id.chip_step_3)
        val btnTopBack = root.findViewById<ImageView>(R.id.btn_top_back)
        val ivTopBell = root.findViewById<ImageView>(R.id.iv_top_bell)
        val tvTopTitle = root.findViewById<TextView>(R.id.tv_top_title)
        val ivCenterPillIcon = root.findViewById<ImageView>(R.id.iv_center_pill_icon)
        val tvContentTitle = root.findViewById<TextView>(R.id.tv_content_title)
        val tvContentDesc = root.findViewById<TextView>(R.id.tv_content_desc)
        val tvContentHighlight = root.findViewById<TextView>(R.id.tv_content_highlight)
        val llInfoBox = root.findViewById<View>(R.id.ll_info_box)
        val ivInfoIcon = root.findViewById<ImageView>(R.id.iv_info_icon)
        val tvInfoText = root.findViewById<TextView>(R.id.tv_info_text)
        val llDualButtons = root.findViewById<View>(R.id.ll_dual_buttons)
        val btnGetStarted = root.findViewById<Button>(R.id.btn_get_started)
        val btnSecondary = root.findViewById<Button>(R.id.btn_secondary)
        val btnPrimary = root.findViewById<Button>(R.id.btn_primary)

        val selectedBg = ContextCompat.getDrawable(context, R.drawable.bg_chip_selected)
        val defaultBg = ContextCompat.getDrawable(context, R.drawable.bg_chip_default)
        val whiteColor = ContextCompat.getColor(context, R.color.white)
        val defaultTextColor = ContextCompat.getColor(context, R.color.chip_default_text)

        chipStep1?.background = if (step == 1) selectedBg else defaultBg
        chipStep1?.setTextColor(if (step == 1) whiteColor else defaultTextColor)

        chipStep2?.background = if (step == 2) selectedBg else defaultBg
        chipStep2?.setTextColor(if (step == 2) whiteColor else defaultTextColor)

        chipStep3?.background = if (step == 3) selectedBg else defaultBg
        chipStep3?.setTextColor(if (step == 3) whiteColor else defaultTextColor)

        when (step) {
            1 -> {
                btnTopBack?.visibility = View.GONE
                ivTopBell?.visibility = View.VISIBLE
                tvTopTitle?.text = getString(R.string.onboarding_1_top_title)

                ivCenterPillIcon?.setImageResource(R.drawable.ic_location_pin)
                tvContentTitle?.text = getString(R.string.onboarding_1_title)
                tvContentDesc?.text = getString(R.string.onboarding_1_desc)
                tvContentHighlight?.visibility = View.VISIBLE
                tvContentHighlight?.text = getString(R.string.onboarding_1_highlight)

                llInfoBox?.visibility = View.GONE

                llDualButtons?.visibility = View.VISIBLE
                btnGetStarted?.visibility = View.GONE
                btnSecondary?.text = getString(R.string.btn_skip)
                btnPrimary?.text = getString(R.string.btn_next)
            }
            2 -> {
                btnTopBack?.visibility = View.VISIBLE
                ivTopBell?.visibility = View.GONE
                tvTopTitle?.text = getString(R.string.onboarding_2_top_title)

                ivCenterPillIcon?.setImageResource(R.drawable.ic_calendar_check_white)
                tvContentTitle?.text = getString(R.string.onboarding_2_title)
                tvContentDesc?.text = getString(R.string.onboarding_2_desc)
                tvContentHighlight?.visibility = View.GONE

                llInfoBox?.visibility = View.VISIBLE
                ivInfoIcon?.setImageResource(R.drawable.ic_info_circle)
                tvInfoText?.text = getString(R.string.onboarding_2_info)

                llDualButtons?.visibility = View.VISIBLE
                btnGetStarted?.visibility = View.GONE
                btnSecondary?.text = getString(R.string.btn_back)
                btnPrimary?.text = getString(R.string.btn_next)
            }
            3 -> {
                btnTopBack?.visibility = View.VISIBLE
                ivTopBell?.visibility = View.GONE
                tvTopTitle?.text = getString(R.string.onboarding_3_top_title)

                ivCenterPillIcon?.setImageResource(R.drawable.ic_bell_ringing_white)
                tvContentTitle?.text = getString(R.string.onboarding_3_title)
                tvContentDesc?.text = getString(R.string.onboarding_3_desc)
                tvContentHighlight?.visibility = View.GONE

                llInfoBox?.visibility = View.VISIBLE
                ivInfoIcon?.setImageResource(R.drawable.ic_info_circle)
                tvInfoText?.text = getString(R.string.onboarding_3_info)

                llDualButtons?.visibility = View.GONE
                btnGetStarted?.visibility = View.VISIBLE
            }
        }
    }

    private fun navigateToLogin() {
        (activity as? MainActivity)?.navigateTo(LoginFragment())
    }
}
