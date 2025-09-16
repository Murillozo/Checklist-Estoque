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

class ChecklistPosto01Parte2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto01_parte2)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""

        val nomeSuprimento = intent.getStringExtra("suprimento") ?: ""
        val nomeProducao = intent.getStringExtra("produção")
            ?: intent.getStringExtra("producao") ?: ""

        val montadoresPrefs = getSharedPreferences("config", MODE_PRIVATE)
            .getString("montadores", "") ?: ""
        val montadoresList = montadoresPrefs.split("\n").filter { it.isNotBlank() }

        val perguntas = listOf(
            "1.1 - INVÓLUCRO - CAIXA: Identificação do projeto",
            "1.1 - INVÓLUCRO - CAIXA: Separação - POSTO - 07",
            "1.1 - INVÓLUCRO - CAIXA: Referências x Projeto",
            "1.1 - INVÓLUCRO - CAIXA: Material em bom estado",

            "1.2 - INVÓLUCRO - AUTOPORTANTE: Identificação do projeto",
            "1.2 - INVÓLUCRO - AUTOPORTANTE: Separação - POSTO - 07",
            "1.2 - INVÓLUCRO - AUTOPORTANTE: Referências x Projeto",
            "1.2 - INVÓLUCRO - AUTOPORTANTE: Material em bom estado",

            "1.3 - INVÓLUCRO - PLACAS DE MONTAGEM: Identificação do projeto",
            "1.3 - INVÓLUCRO - PLACAS DE MONTAGEM: Separação - POSTO - 07",
            "1.3 - INVÓLUCRO - PLACAS DE MONTAGEM: Referências x Projeto",
            "1.3 - INVÓLUCRO - PLACAS DE MONTAGEM: Material em bom estado",

            "1.4 - INVÓLUCRO - FLANGES: Identificação do projeto",
            "1.4 - INVÓLUCRO - FLANGES: Separação - POSTO - 07",
            "1.4 - INVÓLUCRO - FLANGES: Referências x Projeto",
            "1.4 - INVÓLUCRO - FLANGES: Material em bom estado",

            "1.5 - INVÓLUCRO - PORTAS COM RECORTE: Identificação do projeto",
            "1.5 - INVÓLUCRO - PORTAS COM RECORTE: Separação - POSTO - 07",
            "1.5 - INVÓLUCRO - PORTAS COM RECORTE: Referências x Projeto",
            "1.5 - INVÓLUCRO - PORTAS COM RECORTE: Material em bom estado",

            "1.6 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Identificação do projeto",
            "1.6 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Separação - POSTO - 07",
            "1.6 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Referências x Projeto",
            "1.6 - INVÓLUCRO - CONTRAPORTAS COM RECORTE: Material em bom estado",

            "1.7 - CABOS: Identificação do projeto",
            "1.7 - CABOS: Separação - POSTO - 01",
            "1.7 - CABOS: Referências x Projeto",
            "1.7 - CABOS: Material em bom estado",

            "1.8 - BARRAMENTO: Identificação do projeto",
            "1.8 - BARRAMENTO: Separação - POSTO - 04",
            "1.8 - BARRAMENTO: Referências x Projeto",
            "1.8 - BARRAMENTO: Material em bom estado",

            "1.9 - TRILHOS: Identificação do projeto",
            "1.9 - TRILHOS: Separação - POSTO - 03",
            "1.9 - TRILHOS: Referências x Projeto",
            "1.9 - TRILHOS: Material em bom estado",

            "1.10 - CANALETAS: Identificação do projeto",
            "1.10 - CANALETAS: Separação - POSTO - 03",
            "1.10 - CANALETAS: Referências x Projeto",
            "1.10 - CANALETAS: Material em bom estado",

            "1.11 - ETIQUETAS: Identificação do projeto",
            "1.11 - ETIQUETAS: Separação - POSTO - 01",
            "1.11 - ETIQUETAS: Referências x Projeto",
            "1.11 - ETIQUETAS: Material em bom estado",

            "1.12 - PARAFUSOS/PORCAS/ARRUELAS: Identificação do projeto",
            "1.12 - PARAFUSOS/PORCAS/ARRUELAS: Separação - POSTO - 01",
            "1.12 - PARAFUSOS/PORCAS/ARRUELAS: Referências x Projeto",
            "1.12 - PARAFUSOS/PORCAS/ARRUELAS: Material em bom estado",

            "1.13 - ISOLADORES: Identificação do projeto",
            "1.13 - ISOLADORES: Separação - POSTO - 01",
            "1.13 - ISOLADORES: Referências x Projeto",
            "1.13 - ISOLADORES: Material em bom estado",

            "1.14 - PALETIZAÇÃO: Fabricação do palete",
            "1.14 - PALETIZAÇÃO: Fixação no invólucro",

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
            "1.19 - POLICARBONATO: Material em bom estado",
        )

        val container = findViewById<LinearLayout>(R.id.questions_container)
        val triplets = mutableListOf<Triple<CheckBox, CheckBox, CheckBox>>()
        val spinners = mutableListOf<Spinner>()
        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto01Parte2)

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

            val c = CheckBox(this).apply { text = "C" }
            val nc = CheckBox(this).apply { text = "N.C"; setPadding(24, 0, 0, 0) }
            val na = CheckBox(this).apply { text = "N.A"; setPadding(24, 0, 0, 0) }

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
                if (isChecked) { nc.isChecked = false; na.isChecked = false }
                updateButtonState()
            }
            nc.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) { c.isChecked = false; na.isChecked = false }
                updateButtonState()
            }
            na.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) { c.isChecked = false; nc.isChecked = false }
                updateButtonState()
            }
        }

        updateButtonState()

        concluirButton.setOnClickListener {
            val itens = JSONArray()
            triplets.forEachIndexed { idx, (c, nc, na) ->
                val obj = JSONObject()
                obj.put("numero", 55 + idx)
                obj.put("pergunta", perguntas[idx])

                val option = when {
                    c.isChecked -> "C"
                    nc.isChecked -> "NC"
                    na.isChecked -> "NA"
                    else -> ""
                }

                val respostas = JSONObject()
                if (nomeSuprimento.isNotBlank()) {
                    respostas.put("suprimento", JSONArray().apply {
                        put(option)
                        put(nomeSuprimento)
                    })
                }
                respostas.put("producao", JSONArray().apply {
                    put(option)
                    put(spinners[idx].selectedItem.toString())
                })

                obj.put("respostas", respostas)
                itens.put(obj)
            }

            val payload = JSONObject()
            payload.put("obra", obra)
            payload.put("ano", ano)
            payload.put("itens", itens)
            payload.put("origem", "AppOficina")
            if (nomeProducao.isNotBlank()) {
                payload.put("producao", nomeProducao)
            } else if (nomeSuprimento.isNotBlank()) {
                payload.put("suprimento", nomeSuprimento)
            }
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
