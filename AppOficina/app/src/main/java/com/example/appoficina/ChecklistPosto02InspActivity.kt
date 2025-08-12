package com.example.appoficina

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChecklistPosto02InspActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto02)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""
        val inspetor = intent.getStringExtra("inspetor") ?: ""

        val triplets = (200..224).map { i ->
            val cId = resources.getIdentifier("cbQ${i}C", "id", packageName)
            val ncId = resources.getIdentifier("cbQ${i}NC", "id", packageName)
            val naId = resources.getIdentifier("cbQ${i}NA", "id", packageName)
            Triple(
                findViewById<CheckBox>(cId),
                findViewById<CheckBox>(ncId),
                findViewById<CheckBox>(naId),
            )
        }

        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto02)
        val seguirButton = findViewById<Button>(R.id.btnSeguirPosto02)
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

        val perguntas = listOf(
            "2.1 - PORTAS: Identificação do projeto",
            "2.1 - PORTAS: Marcação",
            "2.1 - PORTAS: Furos",
            "2.1 - PORTAS: Cortes",
            "2.1 - PORTAS: Rebarbas",
            "2.1 - PORTAS: Acabamento",
            "2.1 - PORTAS: Porta documentos",
            "2.1 - PORTAS: Montagem no invólucro",
            "2.2 - PLACAS DE MONTAGEM: Identificação do projeto",
            "2.2 - PLACAS DE MONTAGEM: Marcação",
            "2.2 - PLACAS DE MONTAGEM: Furos",
            "2.2 - PLACAS DE MONTAGEM: Cortes",
            "2.2 - PLACAS DE MONTAGEM: Rebarbas",
            "2.2 - PLACAS DE MONTAGEM: Acabamento",
            "2.3 - CANALETAS: Corte",
            "2.3 - CANALETAS: Identificação do projeto",
            "2.3 - CANALETAS: Separação POSTO - 07",
            "2.3 - TRILHOS: Corte",
            "2.3 - TRILHOS: Fixação",
            "2.3 - COMPONENTES FIXAÇÃO DIRETA: Marcação",
            "2.3 - COMPONENTES FIXAÇÃO DIRETA: Furos",
            "2.3 - COMPONENTES FIXAÇÃO DIRETA: Cortes",
            "2.3 - COMPONENTES FIXAÇÃO DIRETA: Rebarbas",
            "2.3 - COMPONENTES FIXAÇÃO DIRETA: Acabamento",
            "2.3 - COMPONENTES FIXAÇÃO DIRETA: Fixação",
        )

        fun buildPayload(): JSONObject {
            val itens = JSONArray()
            triplets.forEachIndexed { idx, (c, nc, na) ->
                val obj = JSONObject()
                obj.put("numero", 200 + idx)
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
            Thread { enviarChecklist(buildPayload()) }.start()
            finish()
        }

        seguirButton.setOnClickListener {
            Thread { enviarChecklist(buildPayload()) }.start()
            val intent = Intent(this, ChecklistPosto03PreInspActivity::class.java)
            intent.putExtra("obra", obra)
            intent.putExtra("ano", ano)
            intent.putExtra("inspetor", inspetor)
            startActivity(intent)
            finish()
        }
    }

    private fun enviarChecklist(json: JSONObject) {
        val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val urls = listOf(
            "http://$ip:5000/json_api/posto02/insp/upload",
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
