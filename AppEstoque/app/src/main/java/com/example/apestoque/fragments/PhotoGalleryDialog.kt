package com.example.apestoque.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.net.Uri
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.util.ImageLoader

class PhotoGalleryDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val dir = requireArguments().getString(ARG_DIR)!!
        val files = requireArguments().getStringArrayList(ARG_FILES)!!
        val recycler = view.findViewById<RecyclerView>(R.id.rvGallery)
        recycler.layoutManager = GridLayoutManager(context, 3)

        val prefs = requireContext().getSharedPreferences("app", 0)
        val ip = prefs.getString("api_ip", "192.168.0.135")
        val baseUrl = "http://$ip:5000/projetista/api/fotos/raw"

        recycler.adapter = GalleryAdapter(dir, files, baseUrl) { url ->
            ImageViewerDialog.newInstance(url).show(parentFragmentManager, "viewer")
        }
    }

    private class GalleryAdapter(
        private val baseDir: String,
        private val files: List<String>,
        private val baseUrl: String,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<GalleryAdapter.VH>() {

        class VH(val image: ImageView) : RecyclerView.ViewHolder(image)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val image = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery_photo, parent, false) as ImageView
            return VH(image)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val file = files[position]
            val path = "$baseDir/$file"
            val encoded = Uri.encode(path).replace("%2F", "/")
            val url = "$baseUrl/$encoded"
            val size = holder.image.resources.displayMetrics.widthPixels / 3
            ImageLoader.loadThumbnail(holder.image, url, size, size)
            holder.image.setOnClickListener { onClick(url) }
        }

        override fun getItemCount() = files.size
    }

    companion object {
        private const val ARG_DIR = "dir"
        private const val ARG_FILES = "files"

        fun newInstance(ano: String, obra: String, arquivos: ArrayList<String>): PhotoGalleryDialog {
            val dir = "$ano/$obra"
            return PhotoGalleryDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIR, dir)
                    putStringArrayList(ARG_FILES, arquivos)
                }
            }
        }
    }
}
