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

class OwnerQrVerificationFragment : Fragment() {

    private var initialRef: String = "BB-24108"

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
            etRef?.setText("BB-23974")
        }
        view.findViewById<TextView>(R.id.pill_test_wrong_biz)?.setOnClickListener {
            etRef?.setText("BB-24120")
        }

        // Look Up & Enter Booking
        view.findViewById<Button>(R.id.btn_lookup_booking)?.setOnClickListener {
            val code = etRef?.text?.toString()?.trim()?.uppercase().orEmpty()

            when {
                code == "BB-24108" -> {
                    // Valid appointment dialog (Page 3 Supporting State)
                    OwnerDialogHelper.showQrOutcomeDialog(
                        context = requireContext(),
                        title = "Valid appointment",
                        message = "BB-24108 belongs to Juniper Studio. Maya Chen · Signature haircut · Oct 24, 10:00 AM.",
                        primaryButtonText = "Confirm Arrival",
                        secondaryButtonText = "Back to Scanner",
                        badgeText = "Demonstration result only · No code was scanned or verified.",
                        onPrimaryAction = {
                            Toast.makeText(requireContext(), "Arrival confirmed for Maya Chen (BB-24108).", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    )
                }
                code == "BB-23974" -> {
                    // Already checked in dialog (Page 3 Supporting State)
                    OwnerDialogHelper.showQrOutcomeDialog(
                        context = requireContext(),
                        title = "Already checked in",
                        message = "BB-23974 was checked in and completed on Oct 10, 2026. This code cannot be used again.",
                        primaryButtonText = "View Booking History",
                        secondaryButtonText = "Back to Scanner",
                        badgeText = "Demonstration result only · No code was scanned or verified.",
                        onPrimaryAction = {
                            Toast.makeText(requireContext(), "Opening booking history.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                code == "BB-24120" -> {
                    // Not for this business dialog (Page 3 Supporting State)
                    OwnerDialogHelper.showQrOutcomeDialog(
                        context = requireContext(),
                        title = "Not for this business",
                        message = "BB-24120 is for Bloom Beauty Bar, not Juniper Studio. Do not check in this appointment.",
                        primaryButtonText = "Scan Another Code",
                        secondaryButtonText = "Back to Scanner",
                        badgeText = "Demonstration result only · No code was scanned or verified.",
                        onPrimaryAction = {
                            etRef?.setText("")
                        }
                    )
                }
                else -> {
                    // Code not recognized dialog (Page 3 Supporting State)
                    OwnerDialogHelper.showQrOutcomeDialog(
                        context = requireContext(),
                        title = "Code not recognized",
                        message = "This sample code does not match a booking. Check the reference or ask the customer to open Booking Details.",
                        primaryButtonText = "Try Another Reference",
                        secondaryButtonText = "Back to Scanner",
                        badgeText = "Demonstration result only · No code was scanned or verified.",
                        onPrimaryAction = {
                            etRef?.selectAll()
                        }
                    )
                }
            }
        }
    }
}
