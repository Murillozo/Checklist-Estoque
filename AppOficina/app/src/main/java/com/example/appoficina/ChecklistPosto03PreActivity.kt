package com.example.appoficina

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

class ChecklistPosto03PreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto03_pre_montagem_01)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""

        val montadoresPrefs = getSharedPreferences("config", MODE_PRIVATE)
            .getString("montadores", "") ?: ""
        val montadoresList = montadoresPrefs.split("\n").filter { it.isNotBlank() }

        val perguntas = listOf(
            "3.1 - COMPONENTES: Montagem",
            "3.1 - COMPONENTES: Montagem de acessórios",
            "3.1 - COMPONENTES: Identificação",
            "3.2 - RÉGUAS DE BORNES: Montagem",
            "3.2 - RÉGUAS DE BORNES: Montagem de acessórios",
            "3.2 - RÉGUAS DE BORNES: Identificação",
            "3.3 - BARRAMENTO FRONTAL: Montagem",
            "3.3 - BARRAMENTO FRONTAL: Parafusos/ Porcas/ Arruelas - Campo",
            "3.3 - BARRAMENTO FRONTAL: Fabricação de anteparos em policarbonato",
            "3.3 - BARRAMENTO FRONTAL: Colagem etiquetas dos anteparos",
            "3.3 - BARRAMENTO FRONTAL: Identificação por projeto de anteparos",
            "3.3 - BARRAMENTO FRONTAL: Separação de anteparos do barramento POSTO - 07",
            "3.4 - PORTA: Nome do painel",
            "3.4 - PORTA: Etiqueta de dados",
            "3.4 - PORTA: Etiqueta de advertencia",
            "3.4 - PORTA: Etiquetas externas dispositivos",
            "3.4 - PORTA: Etiqueta de circuitos",
            "3.4 - PORTA: Etiqueta de componentes",
        )

        val container = findViewById<LinearLayout>(R.id.questions_container)
        val triplets = mutableListOf<Triple<CheckBox, CheckBox, CheckBox>>()
        val spinners = mutableListOf<Spinner>()
        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto03Pre)

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
                obj.put("numero", 301 + idx)
                obj.put("pergunta", perguntas[idx])
                val montadorSelecionado = spinners[idx].selectedItem.toString()
                val resp = JSONArray().put(
                    when {
                        c.isChecked -> "C"
                        nc.isChecked -> "NC"
                        na.isChecked -> "NA"
                        else -> ""
                    }
                )
                obj.put("resposta", resp)
                obj.put("montador", montadorSelecionado)
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
        val ip = getSharedPreferences("config", MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val address = "http://$ip:5000/json_api/posto03_pre/upload"
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
