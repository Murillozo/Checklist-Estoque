package com.example.apestoque.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.example.apestoque.R
import com.example.apestoque.util.ImageLoader
import com.github.chrisbanes.photoview.PhotoView

class ImageViewerDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_image_viewer, null)
        val photo = view.findViewById<PhotoView>(R.id.photoView)
        val url = requireArguments().getString(ARG_URL)!!
        ImageLoader.loadFullScreen(photo, url)
        photo.setOnClickListener { dismiss() }
        dialog.setContentView(view)
        return dialog
    }

    companion object {
        private const val ARG_URL = "url"
        fun newInstance(url: String) = ImageViewerDialog().apply {
            arguments = Bundle().apply { putString(ARG_URL, url) }
        }
    }
}
