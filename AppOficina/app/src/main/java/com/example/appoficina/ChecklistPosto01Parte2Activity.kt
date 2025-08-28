package com.example.appoficina

import android.content.Context
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

        val triplets = (55..74).map { i ->
            val cId = resources.getIdentifier("cbQ${i}C", "id", packageName)
            val ncId = resources.getIdentifier("cbQ${i}NC", "id", packageName)
            val naId = resources.getIdentifier("cbQ${i}NA", "id", packageName)
            Triple(
                findViewById<CheckBox>(cId),
                findViewById<CheckBox>(ncId),
                findViewById<CheckBox>(naId),
            )
        }

        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto01Parte2)

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

        val perguntas = listOf(
            "1.15 - COMPONENTES: Identificação do projeto",
            "1.15 - COMPONENTES: Separação - POSTO - 01",
            "1.15 - COMPONENTES: Referências x Projeto",
            "1.15 - COMPONENTES: Material em bom estado",

            "1.16 - INVÓLUCRO - PORTAS SEM RECORTE: Identificação do projeto",
            "1.16 - INVÓLUCRO - PORTAS SEM RECORTE: Separação - POSTO - 07",
            "1.16 - INVÓLUCRO - PORTAS SEM RECORTE: Referências x Projeto",
            "1.16 - INVÓLUCRO - PORTAS SEM RECORTE: Material em bom estado",

            "1.17 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Identificação do projeto",
            "1.17 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Separação - POSTO - 07",
            "1.17 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Referências x Projeto",
            "1.17 - INVÓLUCRO - CONTRAPORTAS SEM RECORTE: Material em bom estado",

            "1.18 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Identificação do projeto",
            "1.18 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Separação - POSTO - 07",
            "1.18 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Referências x Projeto",
            "1.18 - INVÓLUCRO - FECHAMENTOS LATERAIS E TRASEIRO: Material em bom estado",

            "1.19 - POLICARBONATO: Identificação do projeto",
            "1.19 - POLICARBONATO: Separação - POSTO - 03",
            "1.19 - POLICARBONATO: Referências x Projeto",
            "1.19 - POLICARBONATO: Material em bom estado"
        )

        concluirButton.setOnClickListener {
            val itens = JSONArray()
            triplets.forEachIndexed { idx, (c, nc, na) ->
                val obj = JSONObject()
                obj.put("numero", 55 + idx)
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
            payload.put("produção", producao)
            payload.put("itens", itens)
            Thread { enviarChecklist(payload) }.start()
            finish()
        }
    }

    private fun enviarChecklist(json: JSONObject) {
        val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val urls = listOf(
            "http://$ip:5000/json_api/checklist"
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
