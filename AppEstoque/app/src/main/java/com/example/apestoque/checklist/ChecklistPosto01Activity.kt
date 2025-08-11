package com.example.apestoque.checklist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.ChecklistItem
import com.example.apestoque.data.ChecklistRequest
import com.example.apestoque.data.ChecklistMaterial
import com.example.apestoque.data.ComprasRequest
import com.example.apestoque.data.Item
import com.example.apestoque.data.JsonNetworkModule
import com.example.apestoque.data.NetworkModule
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ChecklistPosto01Activity : AppCompatActivity() {
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto01)

        val id = intent.getIntExtra("id", -1)
        if (id == -1) return finish()
        val jsonPend = intent.getStringExtra("pendentes")
        val jsonMateriais = intent.getStringExtra("materiais") ?: "[]"
        val obra = intent.getStringExtra("obra") ?: ""
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val pendentes = jsonPend?.let {
            val typePend = Types.newParameterizedType(List::class.java, Item::class.java)
            moshi.adapter<List<Item>>(typePend).fromJson(it)
        }
        val typeMateriais = Types.newParameterizedType(List::class.java, ChecklistMaterial::class.java)
        val materiais = moshi.adapter<List<ChecklistMaterial>>(typeMateriais).fromJson(jsonMateriais) ?: emptyList()

        val triplets = (1..54).map { i ->
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
            "1.8 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Identificação do projeto",
            "1.8 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Separação - POSTO - 07",
            "1.8 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Referências x Projeto",
            "1.8 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Material em bom estado",
            "1.11 - CABOS: Identificação do projeto",
            "1.11 - CABOS: Separação - POSTO - 01",
            "1.11 - CABOS: Referências x Projeto",
            "1.11 - CABOS: Material em bom estado",
            "1.12 - BARRAMENTO: Identificação do projeto",
            "1.12 - BARRAMENTO: Separação - POSTO - 04",
            "1.12 - BARRAMENTO: Referências x Projeto",
            "1.12 - BARRAMENTO: Material em bom estado",
            "1.13 - TRILHOS: Identificação do projeto",
            "1.13 - TRILHOS: Separação - POSTO - 03",
            "1.13 - TRILHOS: Referências x Projeto",
            "1.13 - TRILHOS: Material em bom estado",
            "1.14 - CANALETAS: Identificação do projeto",
            "1.14 - CANALETAS: Separação - POSTO - 03",
            "1.14 - CANALETAS: Referências x Projeto",
            "1.14 - CANALETAS: Material em bom estado",
            "1.16 - ETIQUETAS: Identificação do projeto",
            "1.16 - ETIQUETAS: Separação - POSTO - 01",
            "1.16 - ETIQUETAS: Referências x Projeto",
            "1.16 - ETIQUETAS: Material em bom estado",
            "1.17 - PARAFUSOS/PORCAS/ARRUELAS: Identificação do projeto",
            "1.17 - PARAFUSOS/PORCAS/ARRUELAS: Separação - POSTO - 01",
            "1.17 - PARAFUSOS/PORCAS/ARRUELAS: Referências x Projeto",
            "1.17 - PARAFUSOS/PORCAS/ARRUELAS: Material em bom estado",
            "1.18 - ISOLADORES: Identificação do projeto",
            "1.18 - ISOLADORES: Separação - POSTO - 01",
            "1.18 - ISOLADORES: Referências x Projeto",
            "1.18 - ISOLADORES: Material em bom estado",
            "1.19 - PALETIZAÇÃO: Fabricação do palete",
            "1.19 - PALETIZAÇÃO: Fixação no invólucro",
        )

        findViewById<Button>(R.id.btnConcluirPosto01).setOnClickListener {
            val respostas = triplets.mapIndexed { index, (cbC, cbNC, cbNA) ->
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

            val itensChecklist = questions.indices.map { i ->
                ChecklistItem(i + 1, questions[i], respostas[i])
            }

            if (respostas.any { it.contains("NC") }) {
                val ano = Calendar.getInstance().get(Calendar.YEAR).toString()
                val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
                val suprimento = prefs.getString("operador_suprimentos", "") ?: ""

                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val request = ChecklistRequest(obra, ano, suprimento, itensChecklist, materiais)
                            JsonNetworkModule.api(this@ChecklistPosto01Activity).salvarChecklist(request)
                            if (pendentes == null) {
                                NetworkModule.api(this@ChecklistPosto01Activity).aprovarSolicitacao(id)
                            } else {
                                NetworkModule.api(this@ChecklistPosto01Activity).marcarCompras(id, ComprasRequest(pendentes))
                            }
                        }
                        Toast.makeText(this@ChecklistPosto01Activity, "MATERIAL INCOMPLETO", Toast.LENGTH_LONG).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@ChecklistPosto01Activity, "Erro ao concluir", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val type = Types.newParameterizedType(List::class.java, ChecklistItem::class.java)
                val jsonItens = moshi.adapter<List<ChecklistItem>>(type).toJson(itensChecklist)
                val intent = Intent(this, ChecklistPosto01Parte2Activity::class.java)
                intent.putExtra("id", id)
                intent.putExtra("obra", obra)
                jsonPend?.let { intent.putExtra("pendentes", it) }
                intent.putExtra("itens", jsonItens)
                intent.putExtra("materiais", jsonMateriais)
                launcher.launch(intent)
            }
        }
    }
}
