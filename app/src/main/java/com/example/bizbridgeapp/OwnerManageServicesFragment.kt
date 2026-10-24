package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class OwnerManageServicesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_manage_services, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bottom Navigation
        OwnerNavigationHelper.setupBottomNav(this, view, OwnerTab.SERVICES)

        val mainActivity = activity as? MainActivity

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_services)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Filter chips
        val chipAll = view.findViewById<TextView>(R.id.chip_all_services)
        val chipActive = view.findViewById<TextView>(R.id.chip_active_services)
        val chipInactive = view.findViewById<TextView>(R.id.chip_inactive_services)

        val selectChip = { active: TextView, others: List<TextView> ->
            active.setBackgroundResource(R.drawable.bg_chip_selected)
            active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            others.forEach {
                it.setBackgroundResource(R.drawable.bg_chip_default)
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }

        chipAll?.setOnClickListener { selectChip(chipAll, listOfNotNull(chipActive, chipInactive)) }
        chipActive?.setOnClickListener { selectChip(chipActive, listOfNotNull(chipAll, chipInactive)) }
        chipInactive?.setOnClickListener { selectChip(chipInactive, listOfNotNull(chipAll, chipActive)) }

        // + Add Service Button
        view.findViewById<Button>(R.id.btn_add_new_service)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerEditServiceFragment.newInstance(isEditMode = false), addToBackStack = true)
        }

        // Edit buttons
        view.findViewById<View>(R.id.btn_edit_service_1)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerEditServiceFragment.newInstance(isEditMode = true, serviceName = "Signature haircut"), addToBackStack = true)
        }
        view.findViewById<View>(R.id.btn_edit_service_2)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerEditServiceFragment.newInstance(isEditMode = true, serviceName = "Color refresh"), addToBackStack = true)
        }
        view.findViewById<View>(R.id.btn_edit_service_3)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerEditServiceFragment.newInstance(isEditMode = true, serviceName = "Blow-dry & style"), addToBackStack = true)
        }

        // Delete buttons with Specimen 04 Confirmation Dialog
        view.findViewById<View>(R.id.btn_delete_service_1)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Delete Signature haircut?",
                message = "Delete Signature haircut? Existing bookings keep their service details.",
                confirmButtonText = "Delete Service",
                cancelButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    view.findViewById<View>(R.id.card_service_1)?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Signature haircut deleted.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        view.findViewById<View>(R.id.btn_delete_service_2)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Delete Color refresh?",
                message = "Delete Color refresh? Existing bookings keep their service details.",
                confirmButtonText = "Delete Service",
                cancelButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    view.findViewById<View>(R.id.card_service_2)?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Color refresh deleted.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        view.findViewById<View>(R.id.btn_delete_service_3)?.setOnClickListener {
            OwnerDialogHelper.showConfirmationDialog(
                context = requireContext(),
                title = "Delete Blow-dry & style?",
                message = "Delete Blow-dry & style? Existing bookings keep their service details.",
                confirmButtonText = "Delete Service",
                cancelButtonText = "Cancel",
                isDestructive = true,
                onConfirm = {
                    view.findViewById<View>(R.id.card_service_3)?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Blow-dry & style deleted.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
