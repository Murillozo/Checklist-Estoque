package com.example.appoficina

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChecklistPosto02Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto02)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""

        val montadoresPrefs = getSharedPreferences("config", MODE_PRIVATE)
            .getString("montadores", "") ?: ""
        val montadoresList = montadoresPrefs.split("\n").filter { it.isNotBlank() }

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

        val container = findViewById<LinearLayout>(R.id.questions_container)
        val triplets = mutableListOf<Triple<CheckBox, CheckBox, CheckBox>>()
        val spinners = mutableListOf<Spinner>()
        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto02)

        fun updateButtonState() {
            concluirButton.isEnabled = triplets.all { (c, nc, na) ->
                c.isChecked || nc.isChecked || na.isChecked
            } && spinners.all { it.selectedItem != null }
        }

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

            val spinner = Spinner(this)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, montadoresList).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    updateButtonState()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    updateButtonState()
                }
            }
            container.addView(spinner)
            spinners.add(spinner)
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
                obj.put("numero", 200 + idx)
                obj.put("pergunta", perguntas[idx])
                val option = when {
                    c.isChecked -> "C"
                    nc.isChecked -> "NC"
                    na.isChecked -> "NA"
                    else -> ""
                }
                val operadorNome = spinners[idx].selectedItem.toString()
                val statusArray = JSONArray().apply { put(option) }
                val uniqueStatus = JSONArray((0 until statusArray.length()).map { statusArray.getString(it) }.distinct())
                val respostas = JSONObject().put("montador", uniqueStatus)
                obj.put("respostas", respostas)
                obj.put("montador", operadorNome)
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
        val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val urls = listOf(
            "http://$ip:5000/json_api/posto02/upload",
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

