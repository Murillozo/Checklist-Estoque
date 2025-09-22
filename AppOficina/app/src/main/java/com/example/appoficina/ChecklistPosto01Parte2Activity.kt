package com.example.appoficina

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class ChecklistPosto01Parte2Activity : AppCompatActivity() {
    private var previewDialogShown = false
    private lateinit var previewContainer: View
    private lateinit var previewContent: LinearLayout
    private lateinit var previewScroll: ScrollView

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

        previewContainer = findViewById(R.id.preview_container)
        previewContent = findViewById(R.id.preview_content)
        previewScroll = findViewById(R.id.preview_scroll)

        val previewHeader = findViewById<View>(R.id.preview_header)
        val previewCloseButton = findViewById<ImageButton>(R.id.preview_close_button)

        previewCloseButton.setOnClickListener {
            previewContainer.visibility = View.GONE
        }

        var dragOffsetX = 0f
        var dragOffsetY = 0f
        previewHeader.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragOffsetX = previewContainer.x - event.rawX
                    dragOffsetY = previewContainer.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val parent = previewContainer.parent
                    if (parent is View && parent.width > 0 && parent.height > 0) {
                        val maxX = (parent.width - previewContainer.width).coerceAtLeast(0)
                        val maxY = (parent.height - previewContainer.height).coerceAtLeast(0)
                        val newX = (event.rawX + dragOffsetX).coerceIn(0f, maxX.toFloat())
                        val newY = (event.rawY + dragOffsetY).coerceIn(0f, maxY.toFloat())
                        previewContainer.x = newX
                        previewContainer.y = newY
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }

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

        buscarPreVisualizacao(obra)
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

    private fun buscarPreVisualizacao(obra: String) {
        if (obra.isBlank() || previewDialogShown) {
            return
        }

        Thread {
            val ip = getSharedPreferences("config", MODE_PRIVATE)
                .getString("api_ip", "192.168.0.135")
            if (ip.isNullOrBlank()) {
                return@Thread
            }

            val endereco = try {
                "http://$ip:5000/json_api/checklist?obra=" +
                    URLEncoder.encode(obra, "UTF-8")
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
                    val checklist = json.optJSONObject("checklist")
                    if (checklist != null) {
                        runOnUiThread { exibirPreVisualizacao(checklist) }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun exibirPreVisualizacao(checklist: JSONObject) {
        if (previewDialogShown || isFinishing || isDestroyed) {
            return
        }

        val itens = checklist.optJSONArray("itens") ?: return
        if (itens.length() == 0) {
            return
        }

        previewDialogShown = true

        val density = resources.displayMetrics.density
        val paddingGrande = (16 * density).toInt()
        val paddingPequeno = (8 * density).toInt()

        previewContent.removeAllViews()
        previewContent.setPadding(paddingGrande, paddingGrande, paddingGrande, paddingGrande)

        val cabecalho = StringBuilder().apply {
            append("Obra: ")
            append(checklist.optString("obra"))
            val ano = checklist.optString("ano")
            if (ano.isNotBlank()) {
                append("\nAno: ")
                append(ano)
            }
        }

        val respondentes = checklist.optJSONObject("respondentes")
        if (respondentes != null) {
            val partes = mutableListOf<String>()
            val iterator = respondentes.keys()
            while (iterator.hasNext()) {
                val papel = iterator.next()
                val nome = respondentes.optString(papel)
                if (nome.isNotBlank()) {
                    partes.add("${formatarPapel(papel)}: $nome")
                }
            }
            if (partes.isNotEmpty()) {
                cabecalho.append("\n")
                cabecalho.append(partes.joinToString("\n"))
            }
        }

        val headerView = TextView(this).apply {
            text = cabecalho.toString()
            setPadding(0, 0, 0, paddingPequeno)
        }
        previewContent.addView(headerView)

        for (i in 0 until itens.length()) {
            val item = itens.optJSONObject(i) ?: continue
            val pergunta = item.optString("pergunta")
            if (pergunta.isNotBlank()) {
                val perguntaView = TextView(this).apply {
                    text = pergunta
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, if (i == 0) 0 else paddingPequeno, 0, 0)
                }
                previewContent.addView(perguntaView)
            }

            var adicionouResposta = false
            val respostas = item.optJSONObject("respostas")
            if (respostas != null) {
                val chaves = mutableListOf<String>()
                val iterator = respostas.keys()
                while (iterator.hasNext()) {
                    chaves.add(iterator.next())
                }
                chaves.sort()
                for (papel in chaves) {
                    val valor = formatarValor(respostas.opt(papel))
                    if (valor.isBlank()) {
                        continue
                    }
                    val respostaView = TextView(this).apply {
                        text = "\u2022 ${formatarPapel(papel)}: $valor"
                        setPadding(0, paddingPequeno / 2, 0, 0)
                    }
                    previewContent.addView(respostaView)
                    adicionouResposta = true
                }
            }

            if (!adicionouResposta) {
                val valorSimples = formatarValor(item.opt("resposta"))
                if (valorSimples.isNotBlank()) {
                    val respostaView = TextView(this).apply {
                        text = "\u2022 Resposta: $valorSimples"
                        setPadding(0, paddingPequeno / 2, 0, 0)
                    }
                    previewContent.addView(respostaView)
                }
            }
        }

        previewScroll.scrollTo(0, 0)
        previewContainer.alpha = 0f
        previewContainer.visibility = View.VISIBLE
        previewContainer.animate().alpha(1f).setDuration(200L).start()
    }

    private fun formatarValor(valor: Any?): String {
        return when (valor) {
            null -> ""
            JSONObject.NULL -> ""
            is JSONArray -> {
                val partes = mutableListOf<String>()
                for (i in 0 until valor.length()) {
                    val entrada = valor.opt(i)
                    if (entrada == null || entrada == JSONObject.NULL) {
                        continue
                    }
                    partes.add(entrada.toString())
                }
                if (partes.isEmpty()) {
                    ""
                } else if (partes.size == 1) {
                    partes[0]
                } else {
                    val resposta = partes.first()
                    val nomes = partes.drop(1).joinToString(", ")
                    "$resposta ($nomes)"
                }
            }
            else -> valor.toString()
        }
    }

    private fun formatarPapel(papel: String): String {
        return papel.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}
