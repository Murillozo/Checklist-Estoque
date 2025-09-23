package com.example.appoficina

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ChecklistPosto02Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto02)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""
        val montadoresPrefs = getSharedPreferences("config", MODE_PRIVATE)
            .getString("montadores", "") ?: ""
        val montadoresList = montadoresPrefs.split("\n").filter { it.isNotBlank() }

        val previewHelper = FloatingChecklistPreview(
            this,
            findViewById(R.id.preview_container),
            findViewById<LinearLayout>(R.id.preview_content),
            findViewById<ScrollView>(R.id.preview_scroll),
            findViewById(R.id.preview_header),
            findViewById<ImageButton>(R.id.preview_close_button),
            findViewById<ImageButton>(R.id.preview_toggle_button),
            sectionKey = "posto02",
        )
        previewHelper.loadPreviousChecklist(obra, ano)

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
        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto02)
        val spinners = mutableListOf<Spinner>()

        fun updateButtonState() {
            concluirButton.isEnabled = triplets.all { (c, nc, na) ->
                c.isChecked || nc.isChecked || na.isChecked
            } && spinners.all { spinner ->
                val selected = spinner.selectedItem
                selected != null && selected.toString().isNotBlank()
            }
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
            spinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                montadoresList
            ).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
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

        fun aplicarChecklistAnterior(respostas: List<Triple<Int, String?, String?>>) {
            respostas.forEach { (index, opcaoOriginal, montadorAnterior) ->
                if (index !in triplets.indices || index !in spinners.indices) {
                    return@forEach
                }

                val (c, nc, na) = triplets[index]
                val opcao = opcaoOriginal?.replace(".", "")?.uppercase()

                c.isChecked = false
                nc.isChecked = false
                na.isChecked = false

                when (opcao) {
                    "C" -> c.isChecked = true
                    "NC" -> nc.isChecked = true
                    "NA" -> na.isChecked = true
                }

                val montador = montadorAnterior?.takeIf { it.isNotBlank() }

                if (!montador.isNullOrBlank()) {
                    val spinner = spinners[index]
                    val adapter = spinner.adapter as? ArrayAdapter<String>
                    var position = adapter?.getPosition(montador) ?: -1
                    if (position < 0 && adapter != null) {
                        adapter.add(montador)
                        position = adapter.getPosition(montador)
                    }
                    if (position >= 0) {
                        spinner.setSelection(position, false)
                    }
                }
            }

            updateButtonState()
        }

        fun extrairSecaoPosto02(json: JSONObject): JSONObject? {
            val candidatos = mutableListOf<JSONObject>()
            json.optJSONObject("checklist")?.let { candidatos.add(it) }
            candidatos.add(json)

            candidatos.forEach { candidato ->
                candidato.optJSONArray("itens")?.let { return candidato }

                candidato.optJSONObject("posto02")?.let { return it }

                val iterator = candidato.keys()
                while (iterator.hasNext()) {
                    val chave = iterator.next()
                    if (chave.equals("posto02", ignoreCase = true)) {
                        val secao = candidato.optJSONObject(chave)
                        if (secao != null) {
                            return secao
                        }
                    }
                }
            }

            return null
        }

        fun carregarChecklistAnterior() {
            if (obra.isBlank()) {
                return
            }

            Thread {
                val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
                    .getString("api_ip", "192.168.0.135")
                if (ip.isNullOrBlank()) {
                    return@Thread
                }

                val endereco = try {
                    val builder = StringBuilder("http://$ip:5000/json_api/posto02/checklist")
                    builder.append("?obra=")
                    builder.append(URLEncoder.encode(obra, "UTF-8"))
                    if (ano.isNotBlank()) {
                        builder.append("&ano=")
                        builder.append(URLEncoder.encode(ano, "UTF-8"))
                    }
                    builder.toString()
                } catch (_: Exception) {
                    return@Thread
                }

                try {
                    val url = URL(endereco)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    val codigo = conn.responseCode
                    if (codigo in 200..299) {
                        val resposta = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(resposta)
                        val secao = extrairSecaoPosto02(json)
                        val preenchimentos = mutableListOf<Triple<Int, String?, String?>>()

                        val itens = secao?.optJSONArray("itens")
                        if (itens != null) {
                            for (i in 0 until itens.length()) {
                                val item = itens.optJSONObject(i) ?: continue
                                val numero = item.optInt("numero", -1)
                                val indice = when {
                                    numero in 200 until 200 + triplets.size -> numero - 200
                                    else -> perguntas.indexOf(item.optString("pergunta"))
                                }
                                if (indice !in triplets.indices) {
                                    continue
                                }

                                val respostasObj = item.optJSONObject("respostas")
                                val rawRespostas = respostasObj?.opt("montador")
                                var opcao: String? = null
                                var nomeMontador: String? = null

                                when (rawRespostas) {
                                    is JSONArray -> {
                                        opcao = rawRespostas.optString(0)
                                        nomeMontador = rawRespostas.optString(1)
                                    }
                                    is String -> {
                                        opcao = rawRespostas
                                    }
                                }

                                if (nomeMontador.isNullOrBlank()) {
                                    nomeMontador = item.optString("montador")
                                }

                                preenchimentos.add(Triple(indice, opcao, nomeMontador))
                            }
                        }

                        if (preenchimentos.isNotEmpty()) {
                            runOnUiThread {
                                if (!isFinishing && !isDestroyed) {
                                    aplicarChecklistAnterior(preenchimentos)
                                }
                            }
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {
                }
            }.start()
        }

        carregarChecklistAnterior()

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
                val montadorSelecionado = spinners[idx].selectedItem?.toString() ?: ""
                val respostas = JSONObject().put(
                    "montador",
                    JSONArray().put(option).put(montadorSelecionado)
                )
                obj.put("respostas", respostas)
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
        val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val urls = listOf("http://$ip:5000/json_api/posto02/upload")
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
