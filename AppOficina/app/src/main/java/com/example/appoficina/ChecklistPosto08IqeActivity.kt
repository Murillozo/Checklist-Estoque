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
            "Torque parafusos dos componentes",
            "Torque parafusos barra/Isolador",
            "Torque parafusos barra/barra",
            "Lacre parafusos dos componentes",
            "Intertravamento mecânico",
            "Montagem dos componentes conforme o projeto",
            "Montagem de acessórios dos componentes",
            "Montagem de bornes",
            "Montagem de acessórios dos bornes",
            "Montagem de barramentos",
            "Montagem das portas",
            "Montagem das etiquetas",
            "Funcionamento mecânico dos componentes",
            "Funcionamento mecânico partes móveis invólucro",
        )

        val container = findViewById<LinearLayout>(R.id.questions_container)
        val triplets = mutableListOf<Triple<CheckBox, CheckBox, CheckBox>>()

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
            val na = CheckBox(this)
            na.text = "N.A"
            na.setPadding(24, 0, 0, 0)
            row.addView(c)
            row.addView(nc)
            row.addView(na)
            container.addView(row)
            triplets.add(Triple(c, nc, na))
        }

        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto08Iqe)

        fun updateButtonState() {
            concluirButton.isEnabled = triplets.all { (c, nc, na) ->
                c.isChecked || nc.isChecked || na.isChecked
            }
        }

        triplets.forEach { (c, nc, na) ->
            c.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    nc.isChecked = false
                    na.isChecked = false
                }
                updateButtonState()
            }
            nc.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    c.isChecked = false
                    na.isChecked = false
                }
                updateButtonState()
            }
            na.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    c.isChecked = false
                    nc.isChecked = false
                }
                updateButtonState()
            }
        }

        updateButtonState()

        concluirButton.setOnClickListener {
            Thread {
                val payload = buildPayload(perguntas, triplets, obra, ano, inspetor)
                enviarChecklist(payload, "/json_api/posto08_iqe/upload")
            }.start()
            finish()
        }
    }

    private fun buildPayload(
        perguntas: List<String>,
        triplets: List<Triple<CheckBox, CheckBox, CheckBox>>,
        obra: String,
        ano: String,
        inspetor: String,
    ): JSONObject {
        val itens = JSONArray()
        triplets.forEachIndexed { idx, (c, nc, na) ->
            val obj = JSONObject()
            obj.put("numero", 801 + idx)
            obj.put("pergunta", perguntas[idx])
            val resp = JSONArray()
            resp.put(
                when {
                    c.isChecked -> "C"
                    nc.isChecked -> "NC"
                    na.isChecked -> "NA"
                    else -> ""
                }
            )
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
