package com.example.apestoque.checklist

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.ComprasRequest
import com.example.apestoque.data.Item
import com.example.apestoque.data.NetworkModule
import com.squareup.moshi.Types
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class ChecklistPosto01Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto01)

        val id = intent.getIntExtra("id", -1)
        if (id == -1) return finish()
        val obra = intent.getStringExtra("obra") ?: ""
        val jsonPend = intent.getStringExtra("pendentes")
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val pendentes = jsonPend?.let {
            val type = Types.newParameterizedType(List::class.java, Item::class.java)
            moshi.adapter<List<Item>>(type).fromJson(it)
        }

        val cbC = findViewById<CheckBox>(R.id.cbC)
        val cbNC = findViewById<CheckBox>(R.id.cbNC)

        cbC.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbNC.isChecked = false
        }
        cbNC.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbC.isChecked = false
        }

        findViewById<Button>(R.id.btnConcluirPosto01).setOnClickListener {
            val isC = cbC.isChecked
            val isNC = cbNC.isChecked
            if (!isC && !isNC) {
                Toast.makeText(this, "Selecione uma opção", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        gerarPdf(obra, isC)
                        if (pendentes == null) {
                            NetworkModule.api.aprovarSolicitacao(id)
                            
                            
                            
                         
                        } else {
                            NetworkModule.api.marcarCompras(id, ComprasRequest(pendentes))
                        }
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@ChecklistPosto01Activity, "Erro ao concluir", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun gerarPdf(obra: String, marcadoC: Boolean) {
        // Carrega o template a partir dos assets
        val templateName = "checklistttt.pdf"
        val tempFile = File.createTempFile("template", ".pdf", cacheDir)
        assets.open(templateName).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        val renderer = PdfRenderer(
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        )
        val templatePage = renderer.openPage(0)

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(templatePage.width, templatePage.height, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        // desenha o template na nova página
        val bitmap = Bitmap.createBitmap(templatePage.width, templatePage.height, Bitmap.Config.ARGB_8888)
        templatePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        canvas.drawBitmap(bitmap, 0f, 0f, null)


        val paint = Paint().apply { textSize = 12f }
        val xC = 94.78f
        val yC = 125.4f
        val xNC = 150.78f
        val yNC = 125.4f
        val (x, y) = if (marcadoC) xC to yC else xNC to yNC
        canvas.drawText("X", x, y, paint)

        pdf.finishPage(page)
        templatePage.close()
        renderer.close()
        tempFile.delete()

        val ano = Calendar.getInstance().get(Calendar.YEAR)
        val base = File(
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "03 - ENGENHARIA/03 - PRODUCAO/$ano/$obra/CHECKLIST"
        )
        if (!base.exists()) {
            base.mkdirs()
        }
        val file = File(base, "checklist_posto01.pdf")
        FileOutputStream(file).use { fos -> pdf.writeTo(fos) }
        pdf.close()
    }
}
