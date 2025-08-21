package com.example.appoficina

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

fun promptName(context: Context, title: String, onNameEntered: (String) -> Unit) {
    val input = EditText(context)
    val dialog = AlertDialog.Builder(context)
        .setTitle(title)
        .setView(input)
        .setPositiveButton("OK", null)
        .setNegativeButton("Cancelar", null)
        .create()

    dialog.setOnShowListener {
        val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positive.isEnabled = false
        positive.setOnClickListener {
            onNameEntered(input.text.toString())
            dialog.dismiss()
        }
    }

    input.addTextChangedListener(object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !s.isNullOrBlank()
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    })

    dialog.show()
}
