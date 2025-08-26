package com.example.apestoque.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ExpandableListView
import android.widget.FrameLayout
import android.widget.SimpleExpandableListAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.FotoNode
import com.example.apestoque.data.NetworkModule
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class CameraFragment : Fragment() {

    private lateinit var listView: ExpandableListView
    private lateinit var btnCamera: FloatingActionButton
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    private var currentPhoto: File? = null
    private var anoSelecionado: String = ""
    private var obraSelecionada: String = ""

    private var fotoTree: List<FotoNode> = emptyList()

    private val capturedPhotos = mutableListOf<File>()

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhoto != null) {
            capturedPhotos.add(currentPhoto!!)
            promptAnotherPhoto()
        } else if (capturedPhotos.isNotEmpty()) {
            uploadAllPhotos()
        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            AlertDialog.Builder(requireContext())
                .setMessage("Permissão da câmera negada")
                .setPositiveButton("OK", null)
                .show()
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
        val bottomSheet: FrameLayout = view.findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        btnCamera.setOnClickListener { showInputDialog() }

        loadPhotos()

        return view
    }

    private fun showInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_save_photo, null)
        val edtAno = dialogView.findViewById<AutoCompleteTextView>(R.id.edtAno)
        val edtObra = dialogView.findViewById<AutoCompleteTextView>(R.id.edtObra)

        val anos = fotoTree.map { it.name }
        val anoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, anos)
        edtAno.setAdapter(anoAdapter)
        edtAno.threshold = 0
        edtAno.setOnClickListener { edtAno.showDropDown() }

        edtObra.threshold = 0
        edtObra.setOnClickListener { edtObra.showDropDown() }

        edtAno.setOnItemClickListener { parent, _, position, _ ->
            anoSelecionado = parent.getItemAtPosition(position) as String
            val obras = fotoTree.firstOrNull { it.name == anoSelecionado }?.children?.map { it.name } ?: emptyList()
            val obraAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, obras)
            edtObra.setAdapter(obraAdapter)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Salvar foto")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                anoSelecionado = edtAno.text.toString()
                obraSelecionada = edtObra.text.toString()
                capturedPhotos.clear()
                openCamera()
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        dialog.show()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val context = requireContext()
        val photoDir = context.getExternalFilesDir(null) ?: return
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(photoDir, "IMG_${timeStamp}.jpg")
        currentPhoto = photoFile
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        takePicture.launch(uri)
    }

    private fun promptAnotherPhoto() {
        AlertDialog.Builder(requireContext())
            .setMessage("Tirar outra foto?")
            .setPositiveButton("Sim") { _, _ -> openCamera() }
            .setNegativeButton("Não") { _, _ -> uploadAllPhotos() }
            .setCancelable(false)
            .show()
    }

    private fun uploadAllPhotos() {
        val api = NetworkModule.api(requireContext())
        val photos = capturedPhotos.toList()
        capturedPhotos.clear()
        viewLifecycleOwner.lifecycleScope.launch {
            for (file in photos) {
                try {
                    val anoBody = anoSelecionado.toRequestBody("text/plain".toMediaType())
                    val obraBody = obraSelecionada.toRequestBody("text/plain".toMediaType())
                    val reqFile = file.asRequestBody("image/jpeg".toMediaType())
                    val part = MultipartBody.Part.createFormData("foto", file.name, reqFile)
                    api.enviarFoto(anoBody, obraBody, part)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadPhotos()
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
        PhotoGalleryDialog.newInstance(ano, obra.name, ArrayList(arquivos))
            .show(parentFragmentManager, "gallery")
    }
}
