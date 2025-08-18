package com.example.apestoque.checklist

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.ExpedicaoRespostaItem
import com.example.apestoque.data.ExpedicaoUploadRequest
import com.example.apestoque.data.JsonNetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChecklistExpedicaoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist)

        val obra = intent.getStringExtra("obra") ?: return finish()
        val container = findViewById<LinearLayout>(R.id.containerChecklist)
        val btn = findViewById<Button>(R.id.btnConcluir)
        val checks = mutableListOf<Pair<Int, CheckBox>>()

        lifecycleScope.launch {
            try {
                val checklist = withContext(Dispatchers.IO) {
                    JsonNetworkModule.api(this@ChecklistExpedicaoActivity).obterExpedicaoChecklist(obra)
                }
                checklist.itens.forEach { item ->
                    val cb = CheckBox(this@ChecklistExpedicaoActivity).apply { text = item.pergunta }
                    container.addView(cb)
                    checks.add(item.numero to cb)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChecklistExpedicaoActivity, "Erro ao carregar", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val itens = checks.map { (numero, cb) ->
                        ExpedicaoRespostaItem(numero, cb.text.toString(), if (cb.isChecked) listOf("C") else null)
                    }
                    val req = ExpedicaoUploadRequest(obra, itens)
                    withContext(Dispatchers.IO) {
                        JsonNetworkModule.api(this@ChecklistExpedicaoActivity).enviarExpedicaoChecklist(req)
                    }
                    Toast.makeText(this@ChecklistExpedicaoActivity, "Enviado", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@ChecklistExpedicaoActivity, "Erro ao enviar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
