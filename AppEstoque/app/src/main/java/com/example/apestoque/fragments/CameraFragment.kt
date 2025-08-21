package com.example.apestoque.fragments

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
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
import android.widget.ImageView
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.widget.SimpleExpandableListAdapter

class CameraFragment : Fragment() {

    private lateinit var listView: ExpandableListView
    private lateinit var btnCamera: FloatingActionButton
    private lateinit var imagePreview: ImageView

    private var currentPhoto: File? = null
    private var anoSelecionado: String = ""
    private var obraSelecionada: String = ""

    private var fotoTree: List<FotoNode> = emptyList()

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
        imagePreview = view.findViewById(R.id.imagePreview)
        imagePreview.visibility = View.GONE

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
        val photoFile = File(photoDir, "IMG_${timeStamp}.jpg")
        currentPhoto = photoFile
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
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
                fotoTree = api.listarFotos()
                val groupData = fotoTree.map { mapOf("NAME" to it.name) }
                val childData = fotoTree.map { ano ->
                    ano.children?.map { mapOf("NAME" to it.name) } ?: emptyList()
                }
                val adapter = SimpleExpandableListAdapter(
                    requireContext(),
                    groupData,
                    R.layout.list_group,
                    arrayOf("NAME"),
                    intArrayOf(android.R.id.text1),
                    childData,
                    R.layout.list_child,
                    arrayOf("NAME"),
                    intArrayOf(android.R.id.text1)
                )
                listView.setAdapter(adapter)
                listView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
                    val ano = fotoTree[groupPosition]
                    val obra = ano.children?.getOrNull(childPosition)
                    if (obra != null) {
                        showPhotoChooser(ano.name, obra)
                    }
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showPhotoChooser(ano: String, obra: FotoNode) {
        val arquivos = obra.children?.map { it.name } ?: emptyList()
        if (arquivos.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle("Fotos - ${obra.name}")
            .setItems(arquivos.toTypedArray()) { _, which ->
                val nomeArquivo = arquivos[which]
                openImage("${ano}/${obra.name}", nomeArquivo)
            }
            .show()
    }

    private fun openImage(dir: String, file: String) {
        val prefs = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        val ip = prefs.getString("api_ip", "192.168.0.135")
        val encoded = Uri.encode("$dir/AS BUILT/FOTOS/$file").replace("%2F", "/")
        val url = "http://$ip:5000/projetista/api/fotos/raw/$encoded"
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    URL(url).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                imagePreview.setImageBitmap(bitmap)
                imagePreview.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
