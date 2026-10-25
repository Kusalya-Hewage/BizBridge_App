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
import com.example.bizbridgeapp.model.Service
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.ServiceRepository

class OwnerEditServiceFragment : Fragment() {

    private var isEditMode: Boolean = false
    private var serviceId: String = ""
    private var initialServiceName: String = ""
    private var initialServiceDesc: String = ""
    private var initialServicePrice: Double = 0.0
    private var initialServiceDuration: Int = 30
    private var currentBusinessId: String = ""
    private lateinit var imagePickerHelper: ImagePickerHelper

    companion object {
        private const val ARG_IS_EDIT = "arg_is_edit"
        private const val ARG_SERVICE_ID = "arg_service_id"
        private const val ARG_SERVICE_NAME = "arg_service_name"
        private const val ARG_SERVICE_DESC = "arg_service_desc"
        private const val ARG_SERVICE_PRICE = "arg_service_price"
        private const val ARG_SERVICE_DURATION = "arg_service_duration"

        fun newInstance(
            isEditMode: Boolean,
            serviceId: String = "",
            serviceName: String = "",
            serviceDesc: String = "",
            servicePrice: Double = 0.0,
            serviceDuration: Int = 30
        ): OwnerEditServiceFragment {
            val fragment = OwnerEditServiceFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_EDIT, isEditMode)
            args.putString(ARG_SERVICE_ID, serviceId)
            args.putString(ARG_SERVICE_NAME, serviceName)
            args.putString(ARG_SERVICE_DESC, serviceDesc)
            args.putDouble(ARG_SERVICE_PRICE, servicePrice)
            args.putInt(ARG_SERVICE_DURATION, serviceDuration)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerHelper = ImagePickerHelper(this) { uri ->
            Toast.makeText(requireContext(), "Service image selected from ${if (uri.scheme == "file" || uri.path?.contains("photo_") == true) "Camera" else "Gallery"}!", Toast.LENGTH_SHORT).show()
        }
        arguments?.let {
            isEditMode = it.getBoolean(ARG_IS_EDIT, false)
            serviceId = it.getString(ARG_SERVICE_ID, "")
            initialServiceName = it.getString(ARG_SERVICE_NAME, "")
            initialServiceDesc = it.getString(ARG_SERVICE_DESC, "")
            initialServicePrice = it.getDouble(ARG_SERVICE_PRICE, 0.0)
            initialServiceDuration = it.getInt(ARG_SERVICE_DURATION, 30)
        }
        currentBusinessId = BusinessRepository.getOwnerBusinessId()
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

        // Image picker box -> Camera or Gallery
        view.findViewById<View>(R.id.box_add_service_image)?.setOnClickListener {
            imagePickerHelper.showImageSourceDialog("Add Service Photo")
        }

        val etName = view.findViewById<EditText>(R.id.et_service_name)
        val etDesc = view.findViewById<EditText>(R.id.et_service_description)
        val etPrice = view.findViewById<EditText>(R.id.et_service_price)
        val etDuration = view.findViewById<EditText>(R.id.et_service_duration)

        val tvPriceError = view.findViewById<TextView>(R.id.tv_price_error)
        val tvDurationError = view.findViewById<TextView>(R.id.tv_duration_error)

        if (isEditMode) {
            etName?.setText(initialServiceName)
            etDesc?.setText(initialServiceDesc)
            if (initialServicePrice > 0) etPrice?.setText(initialServicePrice.toInt().toString())
            if (initialServiceDuration > 0) etDuration?.setText(initialServiceDuration.toString())
        } else {
            etName?.setText("")
            etDesc?.setText("")
            etPrice?.setText("")
            etDuration?.setText("")
        }

        // Save Service with real Firestore persistence
        val btnSave = view.findViewById<Button>(R.id.btn_save_service)
        btnSave?.setOnClickListener {
            val name = etName?.text?.toString()?.trim().orEmpty()
            val desc = etDesc?.text?.toString()?.trim().orEmpty()
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
            btnSave.text = "Saving to Firestore…"

            val targetServiceId = if (isEditMode && serviceId.isNotEmpty()) serviceId else ""
            val serviceToSave = Service(
                id = targetServiceId,
                businessId = currentBusinessId,
                name = name,
                description = desc,
                price = price ?: 0.0,
                durationMinutes = duration ?: 30,
                isActive = true
            )

            ServiceRepository.createOrUpdateService(
                businessId = currentBusinessId,
                service = serviceToSave,
                onSuccess = {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Service saved successfully in Firestore.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                },
                onFailure = { e ->
                    if (isAdded) {
                        btnSave.isEnabled = true
                        btnSave.text = "Save Service"
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to save service.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
