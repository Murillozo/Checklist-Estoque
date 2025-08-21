package com.example.apestoque.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ExpandableListView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.FotoNode
import com.example.apestoque.data.NetworkModule
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.widget.SimpleExpandableListAdapter
import android.content.Context


class CameraFragment : Fragment() {

    private lateinit var listView: ExpandableListView
    private lateinit var btnCamera: FloatingActionButton

    private var currentPhoto: File? = null
    private var anoSelecionado: String = ""
    private var obraSelecionada: String = ""

    private val takePicture = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhoto != null) {
            uploadPhoto(currentPhoto!!, anoSelecionado, obraSelecionada)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        listView = view.findViewById(R.id.fotoList)
        btnCamera = view.findViewById(R.id.btnCamera)

        btnCamera.setOnClickListener { showInputDialog() }

        loadPhotos()

        return view
    }

    private fun showInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_save_photo, null)
        val edtAno = dialogView.findViewById<EditText>(R.id.edtAno)
        val edtObra = dialogView.findViewById<EditText>(R.id.edtObra)
        AlertDialog.Builder(requireContext())
            .setTitle("Salvar foto")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                anoSelecionado = edtAno.text.toString()
                obraSelecionada = edtObra.text.toString()
                openCamera()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openCamera() {
        val context = requireContext()
        val photoDir = context.getExternalFilesDir(null) ?: return
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(photoDir, "IMG_${'$'}timeStamp.jpg")
        currentPhoto = photoFile
        val uri = FileProvider.getUriForFile(context, "${'$'}{context.packageName}.fileprovider", photoFile)
        takePicture.launch(uri)
    }

    private fun uploadPhoto(file: File, ano: String, obra: String) {
        val api = NetworkModule.api(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val anoBody = ano.toRequestBody("text/plain".toMediaType())
                val obraBody = obra.toRequestBody("text/plain".toMediaType())
                val reqFile = file.asRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("foto", file.name, reqFile)
                api.enviarFoto(anoBody, obraBody, part)
                loadPhotos()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadPhotos() {
        val api = NetworkModule.api(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tree = api.listarFotos()
                val data = flatten(tree, "")
                val groups = data.keys.sorted()
                val groupData = groups.map { mapOf("NAME" to it) }
                val childData = groups.map { dir ->
                    data[dir]!!.sorted().map { mapOf("NAME" to it) }
                }
                val adapter = SimpleExpandableListAdapter(
                    context,
                    groupData,
                    android.R.layout.simple_expandable_list_item_1,
                    arrayOf("NAME"),
                    intArrayOf(android.R.id.text1),
                    childData,
                    android.R.layout.simple_list_item_1,
                    arrayOf("NAME"),
                    intArrayOf(android.R.id.text1)
                )
                listView.setAdapter(adapter)
                listView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
                    val dir = groups[groupPosition]
                    val fileName = data[dir]!![childPosition]
                    openImage(dir, fileName)
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun flatten(nodes: List<FotoNode>, base: String): MutableMap<String, MutableList<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        for (node in nodes) {
            val currentPath = if (base.isEmpty()) node.name else "$base/${node.name}"
            if (node.children.isNullOrEmpty()) {
                val dir = base
                if (dir.isNotEmpty()) {
                    map.getOrPut(dir) { mutableListOf() }.add(node.name)
                }
            } else {
                val childMap = flatten(node.children, currentPath)
                for ((k, v) in childMap) {
                    val list = map.getOrPut(k) { mutableListOf() }
                    list.addAll(v)
                }
            }
        }
        return map
    }

    private fun openImage(dir: String, file: String) {
        val prefs = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        val ip = prefs.getString("api_ip", "192.168.0.135")
        val url = "http://$ip:5000/projetista/api/fotos/raw/${Uri.encode("$dir/$file")}".replace("%2F", "/")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
