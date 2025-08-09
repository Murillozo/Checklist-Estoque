package com.example.appoficina

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChecklistPosto01Parte2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto01_parte2)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""

        val pairs = (55..74).map { i ->
            val cId = resources.getIdentifier("cbQ${i}C", "id", packageName)
            val ncId = resources.getIdentifier("cbQ${i}NC", "id", packageName)
            findViewById<CheckBox>(cId) to findViewById<CheckBox>(ncId)
        }

        pairs.forEach { (c, nc) ->
            c.setOnCheckedChangeListener { _, isChecked -> if (isChecked) nc.isChecked = false }
            nc.setOnCheckedChangeListener { _, isChecked -> if (isChecked) c.isChecked = false }
        }

        findViewById<Button>(R.id.btnConcluirPosto01Parte2).setOnClickListener {
            val itens = JSONArray()
            pairs.forEachIndexed { idx, (c, nc) ->
                val obj = JSONObject()
                obj.put("id", 55 + idx)
                obj.put("resposta", when {
                    c.isChecked -> "C"
                    nc.isChecked -> "NC"
                    else -> ""
                })
                itens.put(obj)
            }
            val payload = JSONObject()
            payload.put("obra", obra)
            payload.put("ano", ano)
            payload.put("itens", itens)
            Thread { enviarChecklist(payload) }.start()
            finish()
        }
    }

    private fun enviarChecklist(json: JSONObject) {
        try {
            val url = URL("http://192.168.0.135:5000/json_api/upload")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {
        }
    }
}

