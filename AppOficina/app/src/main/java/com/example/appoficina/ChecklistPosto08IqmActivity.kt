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

class ChecklistPosto08IqmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto08_iqm)

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
            nc.text = "N.A"
            nc.setPadding(24, 0, 0, 0)
            row.addView(c)
            row.addView(nc)
            container.addView(row)
            pairs.add(Pair(c, nc))
        }

        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto08Iqm)

        fun updateButtonState() {
            concluirButton.isEnabled = pairs.all { (c, nc) -> c.isChecked || nc.isChecked }
        }

        pairs.forEach { (c, nc) ->
            c.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) nc.isChecked = false
                updateButtonState()
            }
            nc.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) c.isChecked = false
                updateButtonState()
            }
        }

        updateButtonState()

        concluirButton.setOnClickListener {
            Thread {
                val itens = JSONArray()
                pairs.forEachIndexed { idx, (c, nc) ->
                    val obj = JSONObject()
                    obj.put("numero", 8101 + idx)
                    obj.put("pergunta", perguntas[idx])
                    val respostas = JSONObject()
                    val arr = JSONArray()
                    arr.put(if (c.isChecked) "C" else "NA")
                    respostas.put("inspetor", arr)
                    obj.put("respostas", respostas)
                    itens.put(obj)
                }
                val root = JSONObject()
                root.put("inspetor", inspetor)
                root.put("itens", itens)
                val payload = JSONObject()
                payload.put("obra", obra)
                payload.put("ano", ano)
                payload.put("posto08_iqm", root)
                enviarChecklist(payload, "/json_api/posto08_iqm/update")
            }.start()
            finish()
        }
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
