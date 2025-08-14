package com.example.appoficina

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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
        if (tipo == "insp_posto08_iqm") {
            for (i in 0 until divergencias.length()) {
                val obj = divergencias.getJSONObject(i)
                val posto = obj.optString("posto")
                val numero = obj.optInt("numero")
                val pergunta = obj.optString("pergunta")
                val respObj = obj.optJSONObject("respostas") ?: JSONObject()
                val entries = mutableListOf<String>()
                val keys = respObj.keys()
                while (keys.hasNext()) {
                    val func = keys.next()
                    val resp = respObj.optString(func)
                    entries.add("$func: $resp")
                }
                val funcsText = entries.joinToString(", ")
                val tv = TextView(this)
                tv.text = "$posto | $numero | $pergunta | $funcsText"
                tv.setPadding(0, 0, 0, 16)
                container.addView(tv)
            }
        } else {
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
        }


        val actionButton = findViewById<Button>(R.id.btnCorrigir)
        if (tipo.startsWith("insp_")) {
            actionButton.text = "Demanda tratada - Continuar com testes"
        }
        actionButton.setOnClickListener {
            val input = EditText(this)
            val titulo = when {
                tipo.startsWith("insp_") -> "Nome do inspetor"
                tipo == "posto02" -> "Nome do conferente da produção"
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
                        "insp_posto02" -> ChecklistPosto02InspActivity::class.java to "inspetor"
                        "insp_posto03_pre" -> ChecklistPosto03PreInspActivity::class.java to "inspetor"
                        "insp_posto04_barramento" -> ChecklistPosto04BarramentoInspActivity::class.java to "inspetor"
                        "insp_posto05_cablagem" -> ChecklistPosto05CablagemInspActivity::class.java to "inspetor"
                        "insp_posto06_pre" -> ChecklistPosto06PreInspActivity::class.java to "inspetor"
                        "insp_posto06_cablagem" -> ChecklistPosto06Cablagem02InspActivity::class.java to "inspetor"
                        "insp_posto08_iqm" -> ChecklistPosto08IqeActivity::class.java to "inspetor"
                        else -> ChecklistPosto02Activity::class.java to "producao"
                    }
                    if (tipo == "insp_posto08_iqm") {
                        corrigirNcIqm(obra)
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

    private fun corrigirNcIqm(obra: String) {
        if (obra.equals("OBRA_DEMO", ignoreCase = true)) {
            return
        }
        Thread {
            val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
                .getString("api_ip", "192.168.0.135")
            val addr = "http://$ip:5000/json_api/posto08_iqm/checklist?obra=" +
                URLEncoder.encode(obra, "UTF-8")
            try {
                val url = URL(addr)
                val conn = url.openConnection() as HttpURLConnection
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(resp)
                val root = json.optJSONObject("posto08_iqm") ?: json
                val itens = root.optJSONArray("itens") ?: JSONArray()
                val correcoes = JSONArray()
                for (i in 0 until itens.length()) {
                    val item = itens.getJSONObject(i)
                    val respostas = item.optJSONObject("respostas") ?: JSONObject()
                    val inspArr = respostas.optJSONArray("inspetor") ?: JSONArray()
                    var corrigiu = false
                    for (j in 0 until inspArr.length()) {
                        val r = inspArr.optString(j)
                        if (r.equals("NC", true) || r.equals("N.C", true)) {
                            inspArr.put(j, "C")
                            corrigiu = true
                        }
                    }
                    if (corrigiu) {
                        correcoes.put(item.optInt("numero"))
                    }
                    respostas.put("inspetor", inspArr)
                    item.put("respostas", respostas)
                }
                root.put("itens", itens)
                root.put("correcoes", correcoes)
                val payload = JSONObject()
                payload.put("obra", json.optString("obra", obra))
                payload.put("ano", json.optString("ano"))
                payload.put("posto08_iqm", root)
                val upUrl = URL("http://$ip:5000/json_api/posto08_iqm/update")
                val upConn = upUrl.openConnection() as HttpURLConnection
                upConn.requestMethod = "POST"
                upConn.doOutput = true
                upConn.setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(upConn.outputStream).use { it.write(payload.toString()) }
                upConn.responseCode
                upConn.disconnect()
            } catch (_: Exception) {
            }
        }.start()
    }
}
