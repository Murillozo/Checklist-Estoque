package com.example.appoficina

import android.content.Intent
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

class ChecklistPosto06PreInspActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto06_pre_montagem_02)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""
        val inspetor = intent.getStringExtra("inspetor") ?: ""

        val perguntas = listOf(
            "6.1 - COMPONENTES FIXAÇÃO DIRETA: Montagem acessórios dos componentes de fixação direta",
            "6.2 - BARRAMENTO HORIZONTAL/VERTICAL/ TRASEIRO: Montagem",
            "6.2 - BARRAMENTO HORIZONTAL/VERTICAL/ TRASEIRO: Parafusos/ Porcas/ Arruelas - Campo",
            "6.2 - BARRAMENTO HORIZONTAL/VERTICAL/ TRASEIRO: Fabricação de anteparos em policarbonato",
            "6.2 - BARRAMENTO HORIZONTAL/VERTICAL/ TRASEIRO: Colagem etiquetas dos anteparos",
            "6.2 - BARRAMENTO HORIZONTAL/VERTICAL/ TRASEIRO: Identificação por projeto de anteparos",
            "6.2 - BARRAMENTO HORIZONTAL/VERTICAL/ TRASEIRO: Separação de anteparos do barramento POSTO - 07",
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

        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto06Pre)
        val seguirButton = findViewById<Button>(R.id.btnSeguirPosto06Pre)

        fun updateButtonState() {
            val enabled = triplets.all { (c, nc, na) ->
                c.isChecked || nc.isChecked || na.isChecked
            }
            concluirButton.isEnabled = enabled
            seguirButton.isEnabled = enabled
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
            val itens = JSONArray()
            triplets.forEachIndexed { idx, (c, nc, na) ->
                val obj = JSONObject()
                obj.put("numero", 601 + idx)
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
            Thread { enviarChecklist(payload) }.start()
            finish()
        }

        seguirButton.setOnClickListener {
            val intent = Intent(this, ChecklistPosto06Cablagem02InspActivity::class.java)
            intent.putExtra("obra", obra)
            intent.putExtra("ano", ano)
            intent.putExtra("inspetor", inspetor)
            startActivity(intent)
            finish()
        }
    }

    private fun enviarChecklist(json: JSONObject) {
        val ip = getSharedPreferences("config", MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val address = "http://$ip:5000/json_api/posto06_pre/insp/upload"
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
