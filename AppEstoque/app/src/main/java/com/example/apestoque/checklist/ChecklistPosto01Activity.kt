package com.example.apestoque.checklist

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.ChecklistItem
import com.example.apestoque.data.ChecklistRequest
import com.example.apestoque.data.ComprasRequest
import com.example.apestoque.data.Item
import com.example.apestoque.data.NetworkModule
import com.example.apestoque.data.JsonNetworkModule
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ChecklistPosto01Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto01)

        val id = intent.getIntExtra("id", -1)
        if (id == -1) return finish()
        val jsonPend = intent.getStringExtra("pendentes")
        val obra = intent.getStringExtra("obra") ?: ""
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val pendentes = jsonPend?.let {
            val type = Types.newParameterizedType(List::class.java, Item::class.java)
            moshi.adapter<List<Item>>(type).fromJson(it)
        }

        val pairs = listOf(
            R.id.cbQ1C to R.id.cbQ1NC,
            R.id.cbQ2C to R.id.cbQ2NC,
            R.id.cbQ3C to R.id.cbQ3NC,
            R.id.cbQ4C to R.id.cbQ4NC,
            R.id.cbQ5C to R.id.cbQ5NC,
            R.id.cbQ6C to R.id.cbQ6NC,
            R.id.cbQ7C to R.id.cbQ7NC,
            R.id.cbQ8C to R.id.cbQ8NC,
            R.id.cbQ9C to R.id.cbQ9NC,
            R.id.cbQ10C to R.id.cbQ10NC,
            R.id.cbQ11C to R.id.cbQ11NC,
            R.id.cbQ12C to R.id.cbQ12NC,
            R.id.cbQ13C to R.id.cbQ13NC,
            R.id.cbQ14C to R.id.cbQ14NC,
            R.id.cbQ15C to R.id.cbQ15NC,
            R.id.cbQ16C to R.id.cbQ16NC,
            R.id.cbQ17C to R.id.cbQ17NC,
            R.id.cbQ18C to R.id.cbQ18NC,
            R.id.cbQ19C to R.id.cbQ19NC,
            R.id.cbQ20C to R.id.cbQ20NC,
        ).map { (c, nc) -> findViewById<CheckBox>(c) to findViewById<CheckBox>(nc) }

        pairs.forEach { (cbC, cbNC) ->
            cbC.setOnCheckedChangeListener { _, isChecked -> if (isChecked) cbNC.isChecked = false }
            cbNC.setOnCheckedChangeListener { _, isChecked -> if (isChecked) cbC.isChecked = false }
        }

        val questions = listOf(
            "1.2 - INVÓLUCRO - CAIXA: Identificação do projeto",
            "1.2 - INVÓLUCRO - CAIXA: Separação - POSTO - 07",
            "1.2 - INVÓLUCRO - CAIXA: Referências x Projeto",
            "1.2 - INVÓLUCRO - CAIXA: Material em bom estado",
            "1.3 - INVÓLUCRO - AUTOPORTANTE: Identificação do projeto",
            "1.3 - INVÓLUCRO - AUTOPORTANTE: Separação - POSTO - 07",
            "1.3 - INVÓLUCRO - AUTOPORTANTE: Referências x Projeto",
            "1.3 - INVÓLUCRO - AUTOPORTANTE: Material em bom estado",
            "1.4 - INVÓLUCRO - PLACAS DE MONTAGEM: Identificação do projeto",
            "1.4 - INVÓLUCRO - PLACAS DE MONTAGEM: Separação - POSTO - 07",
            "1.4 - INVÓLUCRO - PLACAS DE MONTAGEM: Referências x Projeto",
            "1.4 - INVÓLUCRO - PLACAS DE MONTAGEM: Material em bom estado",
            "1.5 - INVÓLUCRO - FLANGES: Identificação do projeto",
            "1.5 - INVÓLUCRO - FLANGES: Separação - POSTO - 07",
            "1.5 - INVÓLUCRO - FLANGES: Referências x Projeto",
            "1.5 - INVÓLUCRO - FLANGES: Material em bom estado",
            "1.6 - INVÓLUCRO - PORTAS COM RECORTE: Identificação do projeto",
            "1.6 - INVÓLUCRO - PORTAS COM RECORTE: Separação - POSTO - 07",
            "1.6 - INVÓLUCRO - PORTAS COM RECORTE: Referências x Projeto",
            "1.6 - INVÓLUCRO - PORTAS COM RECORTE: Material em bom estado",
        )

        findViewById<Button>(R.id.btnConcluirPosto01).setOnClickListener {
            val respostas = pairs.mapIndexed { index, (cbC, cbNC) ->
                val marcados = mutableListOf<String>()
                if (cbC.isChecked) marcados.add("C")
                if (cbNC.isChecked) marcados.add("NC")
                if (marcados.isEmpty()) {
                    Toast.makeText(this, "Selecione uma opção em: ${questions[index]}", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                marcados
            }

            val itensChecklist = questions.indices.map { i ->
                ChecklistItem(questions[i], respostas[i])
            }

            val ano = Calendar.getInstance().get(Calendar.YEAR).toString()

            lifecycleScope.launch {
                try {
                    val filePath = withContext(Dispatchers.IO) {
                        val request = ChecklistRequest(obra, ano, itensChecklist)
                        val response = JsonNetworkModule.api.salvarChecklist(request)
                        if (pendentes == null) {
                            NetworkModule.api.aprovarSolicitacao(id)
                        } else {
                            NetworkModule.api.marcarCompras(id, ComprasRequest(pendentes))
                        }
                        response.caminho
                    }
                    Toast.makeText(
                        this@ChecklistPosto01Activity,
                        "Checklist concluído. Arquivo salvo em:\n$filePath",
                        Toast.LENGTH_LONG,
                    ).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ChecklistPosto01Activity,
                        "Erro ao concluir",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }
}
