package com.example.bizbridgeapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

/**
 * Reusable helper that handles Camera and Gallery photo picking with runtime permissions.
 */
class ImagePickerHelper(
    private val fragment: Fragment,
    private val onImageSelected: (Uri) -> Unit
) {
    private var tempCameraUri: Uri? = null

    // Permission launcher for Camera
    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(fragment.requireContext(), "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show()
            }
        }

    // Camera launcher
    private val takePictureLauncher: ActivityResultLauncher<Uri> =
        fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                tempCameraUri?.let { uri ->
                    onImageSelected(uri)
                }
            }
        }

    // Gallery launcher
    private val pickGalleryLauncher: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                onImageSelected(uri)
            }
        }

    fun showImageSourceDialog(title: String = "Select Photo") {
        val context = fragment.requireContext()
        val options = arrayOf("📷 Take Photo with Camera", "🖼️ Choose from Gallery")

        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> pickGalleryLauncher.launch("image/*")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        val context = fragment.requireContext()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val context = fragment.requireContext()
            val tempFile = File.createTempFile("photo_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(fragment.requireContext(), "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
