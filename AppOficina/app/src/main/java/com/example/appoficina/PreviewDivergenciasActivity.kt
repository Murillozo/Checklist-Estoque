package com.example.appoficina

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class PreviewDivergenciasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_divergencias)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""
        val divergenciasStr = intent.getStringExtra("divergencias") ?: "[]"
        val tipo = intent.getStringExtra("tipo")?.trim()?.lowercase() ?: "posto02"

        val divergencias = try { JSONArray(divergenciasStr) } catch (_: Exception) { JSONArray() }
        val container = findViewById<LinearLayout>(R.id.divergencias_container)
        for (i in 0 until divergencias.length()) {
            val obj = divergencias.getJSONObject(i)
            val numero = obj.optInt("numero")
            val pergunta = obj.optString("pergunta")

            val tv = TextView(this)
            val builder = StringBuilder("$numero - $pergunta")
            val respostas = obj.optJSONObject("respostas")
            if (respostas != null) {
                val roles = listOf("montador", "produção", "inspetor")
                for (role in roles) {
                    val ans = respostas.optString(role, null)
                    if (!ans.isNullOrEmpty()) {
                        builder.append("\n" + role.replaceFirstChar { it.uppercase() } + ": " + ans)
                    }
                }
            } else {
                val prodArr = obj.optJSONArray("produção") ?: JSONArray()
                val inspArr = obj.optJSONArray("inspetor") ?: JSONArray()
                val prodText = (0 until prodArr.length()).joinToString(", ") { prodArr.optString(it) }
                val inspText = (0 until inspArr.length()).joinToString(", ") { inspArr.optString(it) }
                builder.append("\nProdução: $prodText\nInspetor: $inspText")
            }
            tv.text = builder.toString()
            tv.setPadding(0, 0, 0, 16)
            container.addView(tv)
        }


        val actionButton = findViewById<Button>(R.id.btnCorrigir)
        actionButton.text = when {
            tipo.startsWith("insp_") -> "Inspecionar"
            tipo == "posto08_iqm" -> "Demanda tratada - Continuar com testes"
            else -> actionButton.text
        }
        actionButton.setOnClickListener {
            val titulo = when {
                tipo.startsWith("insp_") || tipo == "posto08_iqm" -> "Nome do inspetor"
                tipo == "posto02" -> "Nome do conferente da produção"
                else -> "Nome do montador"
            }
            promptName(this, titulo) { nome ->
                val (clazz, extraName) = when (tipo) {
                    "posto02" -> ChecklistPosto02Activity::class.java to "producao"
                    "posto03_pre" -> ChecklistPosto03PreActivity::class.java to "montador"
                    "posto04_barramento" -> ChecklistPosto04BarramentoActivity::class.java to "montador"
                    "posto05_cablagem" -> ChecklistPosto05CablagemActivity::class.java to "montador"
                    "posto06_pre" -> ChecklistPosto06PreActivity::class.java to "montador"
                    "posto06_cablagem" -> ChecklistPosto06Cablagem02Activity::class.java to "montador"
                    "insp_posto02" -> ChecklistPosto02InspActivity::class.java to "inspetor"
                    "insp_posto03_pre" -> ChecklistPosto03PreInspActivity::class.java to "inspetor"
                    "insp_posto04_barramento" -> ChecklistPosto04BarramentoInspActivity::class.java to "inspetor"
                    "insp_posto05_cablagem" -> ChecklistPosto05CablagemInspActivity::class.java to "inspetor"
                    "insp_posto06_pre" -> ChecklistPosto06PreInspActivity::class.java to "inspetor"
                    "insp_posto06_cablagem" -> ChecklistPosto06Cablagem02InspActivity::class.java to "inspetor"
                    "posto08_iqm" -> ChecklistPosto08IqmActivity::class.java to "inspetor"
                    else -> ChecklistPosto02Activity::class.java to "producao"
                }
                val intent = Intent(this, clazz)
                intent.putExtra("obra", obra)
                intent.putExtra("ano", ano)
                intent.putExtra(extraName, nome)
                startActivity(intent)
                finish()
            }
        }
    }
}
