package com.example.bizbridgeapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bizbridgeapp.repository.BookingRepository
import com.example.bizbridgeapp.repository.BusinessRepository
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultPoint
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class OwnerQrVerificationFragment : Fragment() {

    private var initialRef: String = ""
    private var currentBusinessId: String = ""
    private var barcodeScanner: DecoratedBarcodeView? = null
    private var isTorchOn: Boolean = false
    private var isProcessing: Boolean = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            view?.findViewById<View>(R.id.layout_camera_permission_notice)?.visibility = View.GONE
            startCameraScanner()
        } else {
            view?.findViewById<View>(R.id.layout_camera_permission_notice)?.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Camera permission is required to scan QR codes live.", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            processQrFromUri(uri)
        }
    }

    companion object {
        fun newInstance(reference: String = ""): OwnerQrVerificationFragment {
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

        barcodeScanner = view.findViewById(R.id.barcode_scanner)
        barcodeScanner?.setStatusText("Align QR code inside the frame")

        // Back button
        view.findViewById<ImageView>(R.id.btn_back_qr)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val etRef = view.findViewById<EditText>(R.id.et_qr_reference)
        if (initialRef.isNotBlank()) {
            etRef?.setText(initialRef)
        }

        // Quick test pill shortcuts
        view.findViewById<TextView>(R.id.pill_test_valid)?.setOnClickListener {
            etRef?.setText("BB-24108")
            verifyCode("BB-24108")
        }
        view.findViewById<TextView>(R.id.pill_test_checked_in)?.setOnClickListener {
            etRef?.setText("BB-23914")
            verifyCode("BB-23914")
        }
        view.findViewById<TextView>(R.id.pill_test_wrong_biz)?.setOnClickListener {
            etRef?.setText("BB-99999")
            verifyCode("BB-99999")
        }

        // Flashlight Torch button
        view.findViewById<ImageView>(R.id.btn_toggle_torch)?.setOnClickListener {
            toggleTorch()
        }

        // Choose from Gallery button
        view.findViewById<View>(R.id.btn_scan_gallery)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Enable Camera permission button (overlay fallback)
        view.findViewById<Button>(R.id.btn_grant_camera_permission)?.setOnClickListener {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        val btnLookup = view.findViewById<Button>(R.id.btn_lookup_booking)
        btnLookup?.setOnClickListener {
            val code = etRef?.text?.toString()?.trim()?.uppercase().orEmpty()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a booking reference or QR code.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyCode(code)
        }

        // Setup camera scanner with permission check
        checkAndStartScanner()
    }

    private fun checkAndStartScanner() {
        val permission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            view?.findViewById<View>(R.id.layout_camera_permission_notice)?.visibility = View.GONE
            startCameraScanner()
        } else {
            view?.findViewById<View>(R.id.layout_camera_permission_notice)?.visibility = View.VISIBLE
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraScanner() {
        barcodeScanner?.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                val scanned = result?.text?.trim()?.uppercase()
                if (!scanned.isNullOrBlank() && !isProcessing) {
                    isProcessing = true
                    activity?.runOnUiThread {
                        view?.findViewById<EditText>(R.id.et_qr_reference)?.setText(scanned)
                        verifyCode(scanned)
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })
        barcodeScanner?.resume()
    }

    private fun toggleTorch() {
        val torchBtn = view?.findViewById<ImageView>(R.id.btn_toggle_torch)
        if (isTorchOn) {
            barcodeScanner?.setTorchOff()
            isTorchOn = false
            torchBtn?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
        } else {
            barcodeScanner?.setTorchOn()
            isTorchOn = true
            torchBtn?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
        }
    }

    private fun processQrFromUri(uri: Uri) {
        try {
            val stream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream)
            stream?.close()
            if (bitmap != null) {
                val decoded = decodeQrFromBitmap(bitmap)
                if (!decoded.isNullOrBlank()) {
                    val code = decoded.trim().uppercase()
                    view?.findViewById<EditText>(R.id.et_qr_reference)?.setText(code)
                    verifyCode(code)
                } else {
                    Toast.makeText(requireContext(), "No QR code detected in the selected image.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), "Could not decode image from gallery.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Image scan failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }

    private fun verifyCode(code: String) {
        val btnLookup = view?.findViewById<Button>(R.id.btn_lookup_booking)
        btnLookup?.isEnabled = false
        btnLookup?.text = "Verifying…"

        val bizId = if (currentBusinessId.isNotBlank()) currentBusinessId else BusinessRepository.getOwnerBusinessId()

        BookingRepository.verifyBookingQr(
            businessId = bizId,
            qrRef = code,
            onSuccess = { booking ->
                if (!isAdded) return@verifyBookingQr
                btnLookup?.isEnabled = true
                btnLookup?.text = "Look Up & Enter Booking"

                OwnerDialogHelper.showQrOutcomeDialog(
                    context = requireContext(),
                    title = "Valid appointment",
                    message = "${booking.qrReference.ifEmpty { booking.id }} verified successfully.\nService: ${booking.serviceNameSnapshot}\nDate: ${booking.appointmentDate} at ${booking.appointmentTime}.",
                    primaryButtonText = "Confirm Arrival & Complete",
                    secondaryButtonText = "Scan Another QR",
                    badgeText = "Live QR Verification Confirmed",
                    onPrimaryAction = {
                        Toast.makeText(requireContext(), "Arrival confirmed and appointment marked COMPLETED.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    },
                    onSecondaryAction = {
                        isProcessing = false
                        barcodeScanner?.resume()
                    }
                )
            },
            onFailure = { e ->
                if (!isAdded) return@verifyBookingQr
                btnLookup?.isEnabled = true
                btnLookup?.text = "Look Up & Enter Booking"

                val msg = e.message.orEmpty()
                when {
                    msg.contains("Already checked in", ignoreCase = true) -> {
                        OwnerDialogHelper.showQrOutcomeDialog(
                            context = requireContext(),
                            title = "Already checked in",
                            message = "$code has already been checked in and completed. This QR code cannot be reused.",
                            primaryButtonText = "View Bookings",
                            secondaryButtonText = "Scan Another QR",
                            badgeText = "Attendance Recorded Check",
                            onPrimaryAction = {
                                (activity as? MainActivity)?.navigateTo(OwnerManageBookingsFragment(), addToBackStack = true)
                            },
                            onSecondaryAction = {
                                isProcessing = false
                                barcodeScanner?.resume()
                            }
                        )
                    }
                    msg.contains("Not for this business", ignoreCase = true) -> {
                        OwnerDialogHelper.showQrOutcomeDialog(
                            context = requireContext(),
                            title = "Not for this business",
                            message = "$code is booked for a different service provider. Do not check in this appointment.",
                            primaryButtonText = "Scan Another Code",
                            secondaryButtonText = "Close",
                            badgeText = "Cross-Business Security Check",
                            onPrimaryAction = {
                                view?.findViewById<EditText>(R.id.et_qr_reference)?.setText("")
                                isProcessing = false
                                barcodeScanner?.resume()
                            },
                            onSecondaryAction = {
                                isProcessing = false
                                barcodeScanner?.resume()
                            }
                        )
                    }
                    else -> {
                        OwnerDialogHelper.showQrOutcomeDialog(
                            context = requireContext(),
                            title = "Code not recognized",
                            message = "The reference '$code' does not match any appointment record. Please rescan or verify code.",
                            primaryButtonText = "Try Again",
                            secondaryButtonText = "Close",
                            badgeText = "Search Result",
                            onPrimaryAction = {
                                view?.findViewById<EditText>(R.id.et_qr_reference)?.selectAll()
                                isProcessing = false
                                barcodeScanner?.resume()
                            },
                            onSecondaryAction = {
                                isProcessing = false
                                barcodeScanner?.resume()
                            }
                        )
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeScanner?.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeScanner?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        barcodeScanner?.pause()
        barcodeScanner = null
    }
}
