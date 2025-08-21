package com.example.appoficina

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChecklistPosto08IqeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto08_iqe)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""
        val inspetor = intent.getStringExtra("inspetor") ?: ""

        val perguntas = listOf(
            "Continuidade ponto-a-ponto força",
            "Continuidade ponto-a-ponto comando",
            "Continuidade elétrica entre aterramento e estrutura",
            "Continuidade elétrica entre estrutura e portas",
            "Ausência de curto - circuito força",
            "Ausência de curto - circuito comando",
            "Torque de cabos nos componentes",
            "Torque de cabos no barramento",
        )

        val container = findViewById<LinearLayout>(R.id.questions_container)
        val pairs = mutableListOf<Pair<CheckBox, CheckBox>>()

        perguntas.forEach { pergunta ->
            val tv = TextView(this)
            tv.text = pergunta
            container.addView(tv)
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            val c = CheckBox(this)
            c.text = "C"
            val nc = CheckBox(this)
            nc.text = "N.C"
            nc.setPadding(24, 0, 0, 0)
            row.addView(c)
            row.addView(nc)
            container.addView(row)
            pairs.add(Pair(c, nc))
        }

        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto08Iqe)

        fun updateButtonState() {
            concluirButton.isEnabled = pairs.all { (c, nc) ->
                c.isChecked || nc.isChecked
            }
        }

        pairs.forEach { (c, nc) ->
            c.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    nc.isChecked = false
                }
                updateButtonState()
            }
            nc.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    c.isChecked = false
                }
                updateButtonState()
            }
        }

        updateButtonState()

        concluirButton.setOnClickListener {
            Thread {
                val payload = buildPayload(perguntas, pairs, obra, ano, inspetor)
                enviarChecklist(payload, "/json_api/posto08_teste/upload")
            }.start()
            finish()
        }
    }

    private fun buildPayload(
        perguntas: List<String>,
        pairs: List<Pair<CheckBox, CheckBox>>,
        obra: String,
        ano: String,
        inspetor: String,
    ): JSONObject {
        val itens = JSONArray()
        pairs.forEachIndexed { idx, (c, nc) ->
            val obj = JSONObject()
            obj.put("numero", 8201 + idx)
            obj.put("pergunta", perguntas[idx])
            val resp = JSONArray()
            resp.put(if (c.isChecked) "C" else "NC")
            obj.put("resposta", resp)
            itens.put(obj)
        }
        val payload = JSONObject()
        payload.put("obra", obra)
        payload.put("ano", ano)
        payload.put("inspetor", inspetor)
        payload.put("itens", itens)
        return payload
    }

    private fun enviarChecklist(json: JSONObject, path: String) {
        val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val address = "http://$ip:5000$path"
        try {
            val url = URL(address)
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
