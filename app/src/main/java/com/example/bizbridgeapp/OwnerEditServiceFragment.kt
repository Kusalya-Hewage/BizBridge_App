package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class OwnerEditServiceFragment : Fragment() {

    private var isEditMode: Boolean = true
    private var initialServiceName: String = "Signature haircut"

    companion object {
        private const val ARG_IS_EDIT = "arg_is_edit"
        private const val ARG_SERVICE_NAME = "arg_service_name"

        fun newInstance(isEditMode: Boolean, serviceName: String = "Signature haircut"): OwnerEditServiceFragment {
            val fragment = OwnerEditServiceFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_EDIT, isEditMode)
            args.putString(ARG_SERVICE_NAME, serviceName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isEditMode = arguments?.getBoolean(ARG_IS_EDIT, true) ?: true
        initialServiceName = arguments?.putString(ARG_SERVICE_NAME, "Signature haircut")?.let { initialServiceName } ?: "Signature haircut"
        arguments?.getString(ARG_SERVICE_NAME)?.let { initialServiceName = it }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_edit_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Header Title
        val tvHeader = view.findViewById<TextView>(R.id.tv_header_title)
        tvHeader?.text = if (isEditMode) "Edit service" else "Add service"

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_edit_service)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Image picker box
        view.findViewById<View>(R.id.box_add_service_image)?.setOnClickListener {
            Toast.makeText(requireContext(), "Select service image (Demonstration).", Toast.LENGTH_SHORT).show()
        }

        val etName = view.findViewById<EditText>(R.id.et_service_name)
        val etDesc = view.findViewById<EditText>(R.id.et_service_description)
        val etPrice = view.findViewById<EditText>(R.id.et_service_price)
        val etDuration = view.findViewById<EditText>(R.id.et_service_duration)

        val tvPriceError = view.findViewById<TextView>(R.id.tv_price_error)
        val tvDurationError = view.findViewById<TextView>(R.id.tv_duration_error)

        if (!isEditMode) {
            etName?.setText("")
            etDesc?.setText("")
            etPrice?.setText("")
            etDuration?.setText("")
        } else {
            etName?.setText(initialServiceName)
        }

        // Save Service
        val btnSave = view.findViewById<Button>(R.id.btn_save_service)
        btnSave?.setOnClickListener {
            val name = etName?.text?.toString()?.trim().orEmpty()
            val priceStr = etPrice?.text?.toString()?.trim().orEmpty()
            val durationStr = etDuration?.text?.toString()?.trim().orEmpty()

            var hasError = false

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Service name is required.", Toast.LENGTH_SHORT).show()
                hasError = true
            }

            val price = priceStr.toDoubleOrNull()
            if (price == null || price < 0) {
                tvPriceError?.visibility = View.VISIBLE
                etPrice?.setBackgroundResource(R.drawable.bg_input_field_error)
                hasError = true
            } else {
                tvPriceError?.visibility = View.GONE
                etPrice?.setBackgroundResource(R.drawable.bg_input_field)
            }

            val duration = durationStr.toIntOrNull()
            if (duration == null || duration < 5 || duration > 480) {
                tvDurationError?.visibility = View.VISIBLE
                etDuration?.setBackgroundResource(R.drawable.bg_input_field_error)
                hasError = true
            } else {
                tvDurationError?.visibility = View.GONE
                etDuration?.setBackgroundResource(R.drawable.bg_input_field)
            }

            if (hasError) return@setOnClickListener

            btnSave.isEnabled = false
            btnSave.text = "Saving…"

            btnSave.postDelayed({
                if (isAdded) {
                    Toast.makeText(requireContext(), "Service saved successfully.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }, 600)
        }
    }
}
