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
import android.content.Context

class ChecklistPosto01Parte2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto01_parte2)

        val id = intent.getIntExtra("id", -1)
        if (id == -1) return finish()
        val jsonPend = intent.getStringExtra("pendentes")
        val obra = intent.getStringExtra("obra") ?: ""
        val prevJson = intent.getStringExtra("itens") ?: "[]"
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        val suprimento = prefs.getString("operador_suprimentos", "") ?: ""

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val pendentes = jsonPend?.let {
            val type = Types.newParameterizedType(List::class.java, Item::class.java)
            moshi.adapter<List<Item>>(type).fromJson(it)
        }
        val checklistType = Types.newParameterizedType(List::class.java, ChecklistItem::class.java)
        val prevItems = moshi.adapter<List<ChecklistItem>>(checklistType).fromJson(prevJson) ?: emptyList()

        val pairs = listOf(
            R.id.cbQ55C to R.id.cbQ55NC,
            R.id.cbQ56C to R.id.cbQ56NC,
            R.id.cbQ57C to R.id.cbQ57NC,
            R.id.cbQ58C to R.id.cbQ58NC,
            R.id.cbQ59C to R.id.cbQ59NC,
            R.id.cbQ60C to R.id.cbQ60NC,
            R.id.cbQ61C to R.id.cbQ61NC,
            R.id.cbQ62C to R.id.cbQ62NC,
            R.id.cbQ63C to R.id.cbQ63NC,
            R.id.cbQ64C to R.id.cbQ64NC,
            R.id.cbQ65C to R.id.cbQ65NC,
            R.id.cbQ66C to R.id.cbQ66NC,
            R.id.cbQ67C to R.id.cbQ67NC,
            R.id.cbQ68C to R.id.cbQ68NC,
            R.id.cbQ69C to R.id.cbQ69NC,
            R.id.cbQ70C to R.id.cbQ70NC,
            R.id.cbQ71C to R.id.cbQ71NC,
            R.id.cbQ72C to R.id.cbQ72NC,
            R.id.cbQ73C to R.id.cbQ73NC,
            R.id.cbQ74C to R.id.cbQ74NC,
        ).map { (c, nc) -> findViewById<CheckBox>(c) to findViewById<CheckBox>(nc) }

        pairs.forEach { (cbC, cbNC) ->
            cbC.setOnCheckedChangeListener { _, isChecked -> if (isChecked) cbNC.isChecked = false }
            cbNC.setOnCheckedChangeListener { _, isChecked -> if (isChecked) cbC.isChecked = false }
        }

        val questions = listOf(
            "1.1 - COMPONENTES: Identificação do projeto",
            "1.1 - COMPONENTES: Separação - POSTO - 01",
            "1.1 - COMPONENTES: Referências x Projeto",
            "1.1 - COMPONENTES: Material em bom estado",
            "1.7 - INVÓLUCRO - PORTAS SEM RECORTE: Identificação do projeto",
            "1.7 - INVÓLUCRO - PORTAS SEM RECORTE: Separação - POSTO - 07",
            "1.7 - INVÓLUCRO - PORTAS SEM RECORTE: Referências x Projeto",
            "1.7 - INVÓLUCRO - PORTAS SEM RECORTE: Material em bom estado",
            "1.9 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Identificação do projeto",
            "1.9 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Separação - POSTO - 07",
            "1.9 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Referências x Projeto",
            "1.9 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Material em bom estado",
            "1.10 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Identificação do projeto",
            "1.10 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Separação - POSTO - 07",
            "1.10 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Referências x Projeto",
            "1.10 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Material em bom estado",
            "1.15 - POLICARBONATO: Identificação do projeto",
            "1.15 - POLICARBONATO: Separação - POSTO - 03",
            "1.15 - POLICARBONATO: Referências x Projeto",
            "1.15 - POLICARBONATO: Material em bom estado",
        )

        findViewById<Button>(R.id.btnConcluirPosto01Parte2).setOnClickListener {
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

            val itensChecklist = prevItems + questions.indices.map { i ->
                ChecklistItem(questions[i], respostas[i])
            }

            val ano = Calendar.getInstance().get(Calendar.YEAR).toString()

            lifecycleScope.launch {
                try {
                    val filePath = withContext(Dispatchers.IO) {
                        val request = ChecklistRequest(obra, ano, suprimento, itensChecklist)
                        val response = JsonNetworkModule.api.salvarChecklist(request)
                        if (pendentes == null) {
                            NetworkModule.api.aprovarSolicitacao(id)
                        } else {
                            NetworkModule.api.marcarCompras(id, ComprasRequest(pendentes))
                        }
                        response.caminho
                    }
                    Toast.makeText(
                        this@ChecklistPosto01Parte2Activity,
                        "Checklist concluído. Arquivo salvo em:\n$filePath",
                        Toast.LENGTH_LONG,
                    ).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ChecklistPosto01Parte2Activity,
                        "Erro ao concluir",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }
}
