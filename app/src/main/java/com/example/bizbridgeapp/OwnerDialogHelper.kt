package com.example.bizbridgeapp

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.annotation.DrawableRes

object OwnerDialogHelper {

    /**
     * Shows a confirmation dialog styled 100% to Figma Specimen 04 (Destructive and privileged action).
     */
    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        confirmButtonText: String,
        cancelButtonText: String = "Cancel",
        isDestructive: Boolean = true,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_confirmation_specimen, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_dialog_confirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)

        tvTitle.text = title
        tvMessage.text = message
        btnConfirm.text = confirmButtonText
        btnCancel.text = cancelButtonText

        if (isDestructive) {
            btnConfirm.setBackgroundResource(R.drawable.bg_button_primary)
            btnConfirm.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(context, R.color.primary)
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel?.invoke()
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    /**
     * Shows a QR verification outcome dialog styled to Figma Page 3 Supporting States:
     * - Valid appointment (BB-24108)
     * - Already checked in (BB-23974)
     * - Not for this business (BB-24120)
     * - Code not recognized
     */
    fun showQrOutcomeDialog(
        context: Context,
        title: String,
        message: String,
        primaryButtonText: String,
        secondaryButtonText: String = "Back to Scanner",
        badgeText: String = "Demonstration result only · No code was scanned or verified.",
        onPrimaryAction: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_qr_outcome, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_outcome_title)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_outcome_message)
        val tvBadge = dialogView.findViewById<TextView>(R.id.tv_outcome_badge)
        val btnPrimary = dialogView.findViewById<Button>(R.id.btn_outcome_primary)
        val btnSecondary = dialogView.findViewById<Button>(R.id.btn_outcome_secondary)

        tvTitle.text = title
        tvMessage.text = message
        tvBadge.text = badgeText
        btnPrimary.text = primaryButtonText
        btnSecondary.text = secondaryButtonText

        btnPrimary.setOnClickListener {
            dialog.dismiss()
            onPrimaryAction()
        }

        btnSecondary.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }
}
