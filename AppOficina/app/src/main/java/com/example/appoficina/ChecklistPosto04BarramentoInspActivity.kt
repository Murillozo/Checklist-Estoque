package com.example.appoficina

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChecklistPosto04BarramentoInspActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto04_barramento)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""
        val inspetor = intent.getStringExtra("inspetor") ?: ""

        val previewHelper = FloatingChecklistPreview(
            this,
            findViewById(R.id.preview_container),
            findViewById<LinearLayout>(R.id.preview_content),
            findViewById<ScrollView>(R.id.preview_scroll),
            findViewById(R.id.preview_header),
            findViewById<ImageButton>(R.id.preview_close_button),
            findViewById<ImageButton>(R.id.preview_toggle_button),
        )
        previewHelper.loadPreviousChecklist(obra, ano)

        val perguntas = listOf(
            "4.1 - BARRAMENTO: Corte",
            "4.1 - BARRAMENTO: Furos",
            "4.1 - BARRAMENTO: Roscas",
            "4.1 - BARRAMENTO: Escariamento",
            "4.1 - BARRAMENTO: Dobra",
            "4.1 - BARRAMENTO: Identificação",
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

        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto04Barramento)
        val seguirButton = findViewById<Button>(R.id.btnSeguirPosto04Barramento)
        seguirButton.visibility = View.VISIBLE

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

        fun buildPayload(): JSONObject {
            val itens = JSONArray()
            triplets.forEachIndexed { idx, (c, nc, na) ->
                val obj = JSONObject()
                obj.put("numero", 401 + idx)
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

        concluirButton.setOnClickListener {
            Thread { enviarChecklist(buildPayload(), "/json_api/posto04/insp/upload") }.start()
            finish()
        }

        seguirButton.setOnClickListener {
            val payload = buildPayload()
            seguirButton.isEnabled = false
            concluirButton.isEnabled = false
            Thread { enviarChecklist(payload, "/json_api/posto04/insp/upload") }.start()
            Toast.makeText(this, "Encaminhado ao próximo posto", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun enviarChecklist(json: JSONObject, path: String) {
        val ip = getSharedPreferences("config", MODE_PRIVATE)
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
