package com.equationl.paddleocr4android.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutDialog(private val context: Context) {

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_about, null)

        view.findViewById<TextView>(R.id.tv_about_content).apply {
            movementMethod = LinkMovementMethod.getInstance()
        }

        MaterialAlertDialogBuilder(context)
            .setView(view)
            .setPositiveButton("确定", null)
            .show()
    }
}
