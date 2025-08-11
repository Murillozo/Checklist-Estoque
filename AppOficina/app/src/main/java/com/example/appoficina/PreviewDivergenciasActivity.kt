package com.example.appoficina

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

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
            val prodArr = obj.optJSONArray("produção") ?: JSONArray()
            val inspArr = obj.optJSONArray("inspetor") ?: JSONArray()
            val prodText = (0 until prodArr.length()).joinToString(", ") { prodArr.optString(it) }
            val inspText = (0 until inspArr.length()).joinToString(", ") { inspArr.optString(it) }
            val tv = TextView(this)
            tv.text = "$numero - $pergunta\nProdução: $prodText\nInspetor: $inspText"
            tv.setPadding(0, 0, 0, 16)
            container.addView(tv)
        }

        findViewById<Button>(R.id.btnCorrigir).setOnClickListener {
            val input = EditText(this)
            val titulo = when (tipo) {
                "posto02" -> "Nome do conferente da produção"
                else -> "Nome do montador"
            }
            AlertDialog.Builder(this)
                .setTitle(titulo)
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val nome = input.text.toString()
                    val (clazz, extraName) = when (tipo) {
                        "posto02" -> ChecklistPosto02Activity::class.java to "producao"
                        "posto03_pre" -> ChecklistPosto03PreActivity::class.java to "montador"
                        "posto04_barramento" -> ChecklistPosto04BarramentoActivity::class.java to "montador"
                        "posto05_cablagem" -> ChecklistPosto05CablagemActivity::class.java to "montador"
                        "posto06_pre" -> ChecklistPosto06PreActivity::class.java to "montador"
                        "posto06_cablagem" -> ChecklistPosto06Cablagem02Activity::class.java to "montador"
                        else -> ChecklistPosto02Activity::class.java to "producao"
                    }
                    val intent = Intent(this, clazz)
                    intent.putExtra("obra", obra)
                    intent.putExtra("ano", ano)
                    intent.putExtra(extraName, nome)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}