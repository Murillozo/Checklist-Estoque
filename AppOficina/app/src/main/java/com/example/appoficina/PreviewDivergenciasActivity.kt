package com.example.appoficina

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
                    val value = respostas.opt(role)
                    val ans = when (value) {
                        is JSONArray -> (0 until value.length()).joinToString(", ") { value.optString(it) }
                        null -> null
                        else -> value.toString()
                    }
                    if (!ans.isNullOrEmpty()) {
                        builder.append("\n" + role.replaceFirstChar { it.uppercase() } + ": " + ans)
                    }
                }
            } else {
                val prodKey = if (tipo == "posto02") "montador" else "produção"
                val label = if (tipo == "posto02") "Montador" else "Produção"
                val prodArr = obj.optJSONArray(prodKey) ?: JSONArray()
                val inspArr = obj.optJSONArray("inspetor") ?: JSONArray()
                val prodText = (0 until prodArr.length()).joinToString(", ") { prodArr.optString(it) }
                val inspText = (0 until inspArr.length()).joinToString(", ") { inspArr.optString(it) }
                builder.append("\n$label: $prodText\nInspetor: $inspText")
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
            if (tipo == "insp_posto02") {
                val intent = Intent(this, ChecklistPosto02InspActivity::class.java)
                intent.putExtra("obra", obra)
                intent.putExtra("ano", ano)
                startActivity(intent)
                finish()
            } else if (tipo.startsWith("insp_") || tipo == "posto08_iqm") {
                val titulo = "Nome do inspetor"
                promptName(this, titulo) { nome ->
                    if (tipo == "posto08_iqm") {
                        Thread { marcarDivergenciasTratadas(obra) }.start()
                    }
                    val (clazz, extraName) = when (tipo) {
                        "insp_posto03_pre" -> ChecklistPosto03PreInspActivity::class.java to "inspetor"
                        "insp_posto04_barramento" -> ChecklistPosto04BarramentoInspActivity::class.java to "inspetor"
                        "insp_posto05_cablagem" -> ChecklistPosto05CablagemInspActivity::class.java to "inspetor"
                        "insp_posto06_pre" -> ChecklistPosto06PreInspActivity::class.java to "inspetor"
                        "insp_posto06_cablagem" -> ChecklistPosto06Cablagem02InspActivity::class.java to "inspetor"
                        "posto08_iqm" -> ChecklistPosto08IqmActivity::class.java to "inspetor"
                        else -> ChecklistPosto02InspActivity::class.java to "inspetor"
                    }
                    val intent = Intent(this, clazz)
                    intent.putExtra("obra", obra)
                    intent.putExtra("ano", ano)
                    intent.putExtra(extraName, nome)
                    startActivity(intent)
                    finish()
                }
            } else {
                val clazz = when (tipo) {
                    "posto02" -> ChecklistPosto02Activity::class.java
                    "posto03_pre" -> ChecklistPosto03PreActivity::class.java
                    "posto04_barramento" -> ChecklistPosto04BarramentoActivity::class.java
                    "posto05_cablagem" -> ChecklistPosto05CablagemActivity::class.java
                    "posto06_pre" -> ChecklistPosto06PreActivity::class.java
                    "posto06_cablagem" -> ChecklistPosto06Cablagem02Activity::class.java
                    else -> ChecklistPosto02Activity::class.java
                }
                if (tipo == "posto02") {
                    promptName(this, "Nome do montador") { nome ->
                        val intent = Intent(this, clazz)
                        intent.putExtra("obra", obra)
                        intent.putExtra("ano", ano)
                        intent.putExtra("montador", nome)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val intent = Intent(this, clazz)
                    intent.putExtra("obra", obra)
                    intent.putExtra("ano", ano)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun marcarDivergenciasTratadas(obra: String) {
        val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val address = "http://$ip:5000/json_api/posto08_iqm/resolve"
        try {
            val url = URL(address)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject()
            payload.put("obra", obra)
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {
        }
    }
}
