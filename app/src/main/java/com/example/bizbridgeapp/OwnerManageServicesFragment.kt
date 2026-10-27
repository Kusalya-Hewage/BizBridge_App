package com.example.bizbridgeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.model.Service
import com.example.bizbridgeapp.repository.BusinessRepository
import com.example.bizbridgeapp.repository.ServiceRepository
import com.google.firebase.firestore.ListenerRegistration

class OwnerManageServicesFragment : Fragment() {

    private var servicesListener: ListenerRegistration? = null
    private var allServices: List<Service> = emptyList()
    private var activeFilter: String = "ALL" // ALL, ACTIVE, INACTIVE
    private var currentBusinessId: String = ""

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
        currentBusinessId = BusinessRepository.getOwnerBusinessId()

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_services)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Filter chips
        val chipAll = view.findViewById<TextView>(R.id.chip_all_services)
        val chipActive = view.findViewById<TextView>(R.id.chip_active_services)
        val chipInactive = view.findViewById<TextView>(R.id.chip_inactive_services)

        val selectChip = { active: TextView, others: List<TextView>, filter: String ->
            activeFilter = filter
            active.setBackgroundResource(R.drawable.bg_chip_selected)
            active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            others.forEach {
                it.setBackgroundResource(R.drawable.bg_chip_default)
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            renderServices(view)
        }

        chipAll?.setOnClickListener { selectChip(chipAll, listOfNotNull(chipActive, chipInactive), "ALL") }
        chipActive?.setOnClickListener { selectChip(chipActive, listOfNotNull(chipAll, chipInactive), "ACTIVE") }
        chipInactive?.setOnClickListener { selectChip(chipInactive, listOfNotNull(chipAll, chipActive), "INACTIVE") }

        // + Add Service Button
        view.findViewById<Button>(R.id.btn_add_new_service)?.setOnClickListener {
            mainActivity?.navigateTo(OwnerEditServiceFragment.newInstance(isEditMode = false), addToBackStack = true)
        }

        // Initial state before listener fires
        chipAll?.text = "All · 0"
        chipActive?.text = "Active · 0"
        chipInactive?.text = "Inactive · 0"
        renderServices(view)

        // Real-time listener for Services
        servicesListener = ServiceRepository.listenToServicesForBusiness(currentBusinessId) { list ->
            if (!isAdded) return@listenToServicesForBusiness
            allServices = list

            val activeCount = list.count { it.isActive }
            val inactiveCount = list.count { !it.isActive }

            chipAll?.text = "All · ${list.size}"
            chipActive?.text = "Active · $activeCount"
            chipInactive?.text = "Inactive · $inactiveCount"

            renderServices(view)
        }
    }

    private fun renderServices(rootView: View) {
        val filtered = when (activeFilter) {
            "ACTIVE" -> allServices.filter { it.isActive }
            "INACTIVE" -> allServices.filter { !it.isActive }
            else -> allServices
        }

        val cards = listOf(
            rootView.findViewById<LinearLayout>(R.id.card_service_1),
            rootView.findViewById<LinearLayout>(R.id.card_service_2),
            rootView.findViewById<LinearLayout>(R.id.card_service_3)
        )

        val editButtons = listOf(
            rootView.findViewById<View>(R.id.btn_edit_service_1),
            rootView.findViewById<View>(R.id.btn_edit_service_2),
            rootView.findViewById<View>(R.id.btn_edit_service_3)
        )

        val deleteButtons = listOf(
            rootView.findViewById<View>(R.id.btn_delete_service_1),
            rootView.findViewById<View>(R.id.btn_delete_service_2),
            rootView.findViewById<View>(R.id.btn_delete_service_3)
        )

        for (i in 0 until 3) {
            val card = cards[i] ?: continue
            val btnEdit = editButtons[i]
            val btnDelete = deleteButtons[i]

            if (i < filtered.size) {
                val service = filtered[i]
                card.visibility = View.VISIBLE

                val topRel = card.getChildAt(0) as? RelativeLayout
                val tvTitle = topRel?.getChildAt(0) as? TextView
                val tvDesc = card.getChildAt(1) as? TextView
                val bottomLinear = card.getChildAt(2) as? LinearLayout
                val tvPriceDuration = bottomLinear?.getChildAt(0) as? TextView
                val tvBadge = bottomLinear?.getChildAt(1) as? TextView

                tvTitle?.text = service.name
                tvDesc?.text = service.description
                tvPriceDuration?.text = "Rs. ${service.price.toInt()} · ${service.durationMinutes} min"

                if (service.isActive) {
                    tvBadge?.text = "Active"
                    tvBadge?.setBackgroundResource(R.drawable.bg_tag_verified)
                    tvBadge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
                } else {
                    tvBadge?.text = "Inactive"
                    tvBadge?.setBackgroundResource(R.drawable.bg_badge_cancelled)
                    tvBadge?.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_cancelled_text))
                }

                // Edit click
                btnEdit?.setOnClickListener {
                    (activity as? MainActivity)?.navigateTo(
                        OwnerEditServiceFragment.newInstance(
                            isEditMode = true,
                            serviceId = service.id,
                            serviceName = service.name,
                            serviceDesc = service.description,
                            servicePrice = service.price,
                            serviceDuration = service.durationMinutes
                        ),
                        addToBackStack = true
                    )
                }

                // Delete click with confirmation dialog and real Firestore delete
                btnDelete?.setOnClickListener {
                    OwnerDialogHelper.showConfirmationDialog(
                        context = requireContext(),
                        title = "Delete ${service.name}?",
                        message = "Delete ${service.name}? Existing bookings will keep their service details.",
                        confirmButtonText = "Delete Service",
                        cancelButtonText = "Cancel",
                        isDestructive = true,
                        onConfirm = {
                            ServiceRepository.deleteService(
                                businessId = currentBusinessId,
                                serviceId = service.id,
                                onSuccess = {
                                    if (isAdded) {
                                        Toast.makeText(requireContext(), "${service.name} deleted.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onFailure = { e ->
                                    if (isAdded) {
                                        Toast.makeText(requireContext(), e.localizedMessage ?: "Failed to delete.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    )
                }
            } else {
                card.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        servicesListener?.remove()
    }
}
