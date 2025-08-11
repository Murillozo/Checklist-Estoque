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
            AlertDialog.Builder(this)
                .setTitle("Nome do conferente da produção")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val producao = input.text.toString()
                    val intent = Intent(this, ChecklistPosto02Activity::class.java)
                    intent.putExtra("obra", obra)
                    intent.putExtra("ano", ano)
                    intent.putExtra("producao", producao)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}
