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
        val producao = intent.getStringExtra("producao") ?: ""

        val pairs = (55..74).map { i ->
            val cId = resources.getIdentifier("cbQ${i}C", "id", packageName)
            val ncId = resources.getIdentifier("cbQ${i}NC", "id", packageName)
            findViewById<CheckBox>(cId) to findViewById<CheckBox>(ncId)
        }

        pairs.forEach { (c, nc) ->
            c.setOnCheckedChangeListener { _, isChecked -> if (isChecked) nc.isChecked = false }
            nc.setOnCheckedChangeListener { _, isChecked -> if (isChecked) c.isChecked = false }
        }

        val perguntas = listOf(
            "Identificação do projeto",
            "Separação - POSTO - 01",
            "Referências x Projeto",
            "Material em bom estado",
            "Identificação do projeto",
            "Separação - POSTO - 07",
            "Referências x Projeto",
            "Material em bom estado",
            "Identificação do projeto",
            "Separação - POSTO - 07",
            "Referências x Projeto",
            "Material em bom estado",
            "Identificação do projeto",
            "Separação - POSTO - 07",
            "Referências x Projeto",
            "Material em bom estado",
            "Identificação do projeto",
            "Separação - POSTO - 03",
            "Referências x Projeto",
            "Material em bom estado"
        )

        findViewById<Button>(R.id.btnConcluirPosto01Parte2).setOnClickListener {
            val itens = JSONArray()
            pairs.forEachIndexed { idx, (c, nc) ->
                val obj = JSONObject()
                obj.put("numero", 55 + idx)
                obj.put("pergunta", perguntas[idx])
                val resp = JSONArray()
                resp.put(
                    when {
                        c.isChecked -> "C"
                        nc.isChecked -> "NC"
                        else -> ""
                    }
                )
                obj.put("resposta", resp)
                itens.put(obj)
            }
            val payload = JSONObject()
            payload.put("obra", obra)
            payload.put("ano", ano)
            payload.put("produção", producao)
            payload.put("itens", itens)
            Thread { enviarChecklist(payload) }.start()
            finish()
        }
    }

    private fun enviarChecklist(json: JSONObject) {
        val urls = listOf(
            "http://10.0.2.2:5000/json_api/upload",
            "http://192.168.0.151:5000/json_api/upload",
            "http://192.168.0.135:5000/json_api/upload"
        )
        for (addr in urls) {
            try {
                val url = URL(addr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..299) break
            } catch (_: Exception) {
                // tenta próximo endereço
            }
        }
    }
}

