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
import com.example.apestoque.data.ChecklistMaterial
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
        val jsonMateriais = intent.getStringExtra("materiais") ?: "[]"
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        val suprimento = prefs.getString("operador_suprimentos", "") ?: ""

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val pendentes = jsonPend?.let {
            val type = Types.newParameterizedType(List::class.java, Item::class.java)
            moshi.adapter<List<Item>>(type).fromJson(it)
        }
        val checklistType = Types.newParameterizedType(List::class.java, ChecklistItem::class.java)
        val prevItems = moshi.adapter<List<ChecklistItem>>(checklistType).fromJson(prevJson) ?: emptyList()
        val typeMateriais = Types.newParameterizedType(List::class.java, ChecklistMaterial::class.java)
        val materiais = moshi.adapter<List<ChecklistMaterial>>(typeMateriais).fromJson(jsonMateriais) ?: emptyList()

        val triplets = (55..128).map { i ->
            val c = resources.getIdentifier("cbQ${i}C", "id", packageName)
            val nc = resources.getIdentifier("cbQ${i}NC", "id", packageName)
            val na = resources.getIdentifier("cbQ${i}NA", "id", packageName)
            Triple(
                findViewById<CheckBox>(c),
                findViewById<CheckBox>(nc),
                findViewById<CheckBox>(na),
            )
        }

        triplets.forEach { (cbC, cbNC, cbNA) ->
            cbC.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    cbNC.isChecked = false
                    cbNA.isChecked = false
                }
            }
            cbNC.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    cbC.isChecked = false
                    cbNA.isChecked = false
                }
            }
            cbNA.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    cbC.isChecked = false
                    cbNC.isChecked = false
                }
            }
        }

        val questions = listOf(
            "1.1 - INVÓLUCRO - CAIXA: Identificação do projeto",
            "1.1 - INVÓLUCRO - CAIXA: Separação - POSTO - 07",
            "1.1 - INVÓLUCRO - CAIXA: Referências x Projeto",
            "1.1 - INVÓLUCRO - CAIXA: Material em bom estado",

            "1.2 - INVÓLUCRO - AUTOPORTANTE: Identificação do projeto",
            "1.2 - INVÓLUCRO - AUTOPORTANTE: Separação - POSTO - 07",
            "1.2 - INVÓLUCRO - AUTOPORTANTE: Referências x Projeto",
            "1.2 - INVÓLUCRO - AUTOPORTANTE: Material em bom estado",

            "1.3 - INVÓLUCRO - PLACAS DE MONTAGEM: Identificação do projeto",
            "1.3 - INVÓLUCRO - PLACAS DE MONTAGEM: Separação - POSTO - 07",
            "1.3 - INVÓLUCRO - PLACAS DE MONTAGEM: Referências x Projeto",
            "1.3 - INVÓLUCRO - PLACAS DE MONTAGEM: Material em bom estado",

            "1.4 - INVÓLUCRO - FLANGES: Identificação do projeto",
            "1.4 - INVÓLUCRO - FLANGES: Separação - POSTO - 07",
            "1.4 - INVÓLUCRO - FLANGES: Referências x Projeto",
            "1.4 - INVÓLUCRO - FLANGES: Material em bom estado",

            "1.5 - INVÓLUCRO - PORTAS COM RECORTE: Identificação do projeto",
            "1.5 - INVÓLUCRO - PORTAS COM RECORTE: Separação - POSTO - 07",
            "1.5 - INVÓLUCRO - PORTAS COM RECORTE: Referências x Projeto",
            "1.5 - INVÓLUCRO - PORTAS COM RECORTE: Material em bom estado",

            "1.6 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Identificação do projeto",
            "1.6 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Separação - POSTO - 07",
            "1.6 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Referências x Projeto",
            "1.6 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Material em bom estado",

            "1.7 - CABOS: Identificação do projeto",
            "1.7 - CABOS: Separação - POSTO - 01",
            "1.7 - CABOS: Referências x Projeto",
            "1.7 - CABOS: Material em bom estado",

            "1.8 - BARRAMENTO: Identificação do projeto",
            "1.8 - BARRAMENTO: Separação - POSTO - 04",
            "1.8 - BARRAMENTO: Referências x Projeto",
            "1.8 - BARRAMENTO: Material em bom estado",

            "1.9 - TRILHOS: Identificação do projeto",
            "1.9 - TRILHOS: Separação - POSTO - 03",
            "1.9 - TRILHOS: Referências x Projeto",
            "1.9 - TRILHOS: Material em bom estado",

            "1.10 - CANALETAS: Identificação do projeto",
            "1.10 - CANALETAS: Separação - POSTO - 03",
            "1.10 - CANALETAS: Referências x Projeto",
            "1.10 - CANALETAS: Material em bom estado",

            "1.11 - ETIQUETAS: Identificação do projeto",
            "1.11 - ETIQUETAS: Separação - POSTO - 01",
            "1.11 - ETIQUETAS: Referências x Projeto",
            "1.11 - ETIQUETAS: Material em bom estado",

            "1.12 - PARAFUSOS/PORCAS/ARRUELAS: Identificação do projeto",
            "1.12 - PARAFUSOS/PORCAS/ARRUELAS: Separação - POSTO - 01",
            "1.12 - PARAFUSOS/PORCAS/ARRUELAS: Referências x Projeto",
            "1.12 - PARAFUSOS/PORCAS/ARRUELAS: Material em bom estado",

            "1.13 - ISOLADORES: Identificação do projeto",
            "1.13 - ISOLADORES: Separação - POSTO - 01",
            "1.13 - ISOLADORES: Referências x Projeto",
            "1.13 - ISOLADORES: Material em bom estado",

            "1.14 - PALETIZAÇÃO: Fabricação do palete",
            "1.14 - PALETIZAÇÃO: Fixação no invólucro",

            "1.15 - COMPONENTES: Identificação do projeto",
            "1.15 - COMPONENTES: Separação - POSTO - 01",
            "1.15 - COMPONENTES: Referências x Projeto",
            "1.15 - COMPONENTES: Material em bom estado",

            "1.16 - INVÓLUCRO - PORTAS SEM RECORTE: Identificação do projeto",
            "1.16 - INVÓLUCRO - PORTAS SEM RECORTE: Separação - POSTO - 07",
            "1.16 - INVÓLUCRO - PORTAS SEM RECORTE: Referências x Projeto",
            "1.16 - INVÓLUCRO - PORTAS SEM RECORTE: Material em bom estado",

            "1.17 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Identificação do projeto",
            "1.17 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Separação - POSTO - 07",
            "1.17 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Referências x Projeto",
            "1.17 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Material em bom estado",

            "1.18 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Identificação do projeto",
            "1.18 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Separação - POSTO - 07",
            "1.18 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Referências x Projeto",
            "1.18 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Material em bom estado",

            "1.19 - POLICARBONATO: Identificação do projeto",
            "1.19 - POLICARBONATO: Separação - POSTO - 03",
            "1.19 - POLICARBONATO: Referências x Projeto",
            "1.19 - POLICARBONATO: Material em bom estado",
        )


        findViewById<Button>(R.id.btnConcluirPosto01Parte2).setOnClickListener {
            val respostasSelecionadas = triplets.mapIndexed { index, (cbC, cbNC, cbNA) ->
                val marcados = mutableListOf<String>()
                if (cbC.isChecked) marcados.add("C")
                if (cbNC.isChecked) marcados.add("NC")
                if (cbNA.isChecked) marcados.add("NA")
                if (marcados.isEmpty()) {
                    Toast.makeText(this, "Selecione uma opção em: ${questions[index]}", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                marcados
            }


            
            val itensChecklist = prevItems + questions.indices.map { i ->
                ChecklistItem(i + 55, questions[i], mapOf("producao" to respostasSelecionadas[i]))
            }

            val ano = Calendar.getInstance().get(Calendar.YEAR).toString()

            lifecycleScope.launch {
                try {
                    val filePath = withContext(Dispatchers.IO) {
                        val request = ChecklistRequest(obra, ano, suprimento, itensChecklist, materiais)
                        val response = JsonNetworkModule.api(this@ChecklistPosto01Parte2Activity).salvarChecklist(request)
                        if (pendentes == null) {
                            NetworkModule.api(this@ChecklistPosto01Parte2Activity).aprovarSolicitacao(id)
                        } else {
                            NetworkModule.api(this@ChecklistPosto01Parte2Activity).marcarCompras(id, ComprasRequest(pendentes))
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
