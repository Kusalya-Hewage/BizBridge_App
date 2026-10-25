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
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.BusinessRepository

class OwnerQrVerificationFragment : Fragment() {

    private var initialRef: String = "BB-24108"
    private var currentBusinessId: String = "biz_juniper_studio"

    companion object {
        fun newInstance(reference: String = "BB-24108"): OwnerQrVerificationFragment {
            val fragment = OwnerQrVerificationFragment()
            val args = Bundle()
            args.putString("reference", reference)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("reference")?.let { initialRef = it }
        currentBusinessId = BusinessRepository.getOwnerBusinessId()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner_qr_verification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_qr)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val etRef = view.findViewById<EditText>(R.id.et_qr_reference)
        etRef?.setText(initialRef)

        // Quick test pill shortcuts
        view.findViewById<TextView>(R.id.pill_test_valid)?.setOnClickListener {
            etRef?.setText("BB-24108")
        }
        view.findViewById<TextView>(R.id.pill_test_checked_in)?.setOnClickListener {
            etRef?.setText("BB-23914")
        }
        view.findViewById<TextView>(R.id.pill_test_wrong_biz)?.setOnClickListener {
            etRef?.setText("BB-99999")
        }

        val btnLookup = view.findViewById<Button>(R.id.btn_lookup_booking)

        // Look Up & Enter Booking using real Firestore verification
        btnLookup?.setOnClickListener {
            val code = etRef?.text?.toString()?.trim()?.uppercase().orEmpty()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a booking reference or QR code.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLookup.isEnabled = false
            btnLookup.text = "Verifying…"

            BookingRepository.verifyBookingQr(
                businessId = currentBusinessId,
                qrRef = code,
                onSuccess = { booking ->
                    if (!isAdded) return@verifyBookingQr
                    btnLookup.isEnabled = true
                    btnLookup.text = "Look Up & Enter"

                    OwnerDialogHelper.showQrOutcomeDialog(
                        context = requireContext(),
                        title = "Valid appointment",
                        message = "${booking.qrReference.ifEmpty { booking.id }} belongs to your business.\nService: ${booking.serviceNameSnapshot}\nDate: ${booking.appointmentDate} at ${booking.appointmentTime}.",
                        primaryButtonText = "Confirm Arrival & Complete",
                        secondaryButtonText = "Back to Scanner",
                        badgeText = "Verified Live via Firebase Cloud Database",
                        onPrimaryAction = {
                            Toast.makeText(requireContext(), "Arrival confirmed and appointment marked COMPLETED.", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    )
                },
                onFailure = { e ->
                    if (!isAdded) return@verifyBookingQr
                    btnLookup.isEnabled = true
                    btnLookup.text = "Look Up & Enter"

                    val msg = e.message.orEmpty()
                    when {
                        msg.contains("Already checked in", ignoreCase = true) -> {
                            OwnerDialogHelper.showQrOutcomeDialog(
                                context = requireContext(),
                                title = "Already checked in",
                                message = "$code has already been checked in and marked completed. This QR code cannot be reused.",
                                primaryButtonText = "View Booking History",
                                secondaryButtonText = "Back to Scanner",
                                badgeText = "Live Firestore Record Check",
                                onPrimaryAction = {
                                    (activity as? MainActivity)?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = true)
                                }
                            )
                        }
                        msg.contains("Not for this business", ignoreCase = true) -> {
                            OwnerDialogHelper.showQrOutcomeDialog(
                                context = requireContext(),
                                title = "Not for this business",
                                message = "$code is booked for a different service provider. Do not check in this appointment.",
                                primaryButtonText = "Scan Another Code",
                                secondaryButtonText = "Back to Scanner",
                                badgeText = "Live Firestore Cross-Business Security Check",
                                onPrimaryAction = {
                                    etRef?.setText("")
                                }
                            )
                        }
                        else -> {
                            OwnerDialogHelper.showQrOutcomeDialog(
                                context = requireContext(),
                                title = "Code not recognized",
                                message = "The reference '$code' does not match any appointment in Firestore. Please verify the code or ask the customer to open Booking Details.",
                                primaryButtonText = "Try Another Reference",
                                secondaryButtonText = "Back to Scanner",
                                badgeText = "Firestore Search Result",
                                onPrimaryAction = {
                                    etRef?.selectAll()
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}
