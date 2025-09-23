package com.example.appoficina

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ChecklistHistoryActivity : AppCompatActivity() {
    private lateinit var obra: String
    private var ano: String? = null
    private lateinit var tipo: String
    private var sectionKey: String? = null

    private lateinit var container: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var startButton: Button
    private lateinit var closeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_history)

        obra = intent.getStringExtra("obra") ?: ""
        ano = intent.getStringExtra("ano")
        tipo = intent.getStringExtra("tipo") ?: "insp_posto02"
        sectionKey = intent.getStringExtra("sectionKey")

        container = findViewById(R.id.history_container)
        scrollView = findViewById(R.id.history_scroll)
        loadingIndicator = findViewById(R.id.history_loading)
        emptyView = findViewById(R.id.history_empty)
        startButton = findViewById(R.id.btnStartInspection)
        closeButton = findViewById(R.id.btnCloseHistory)

        val renderer = ChecklistPreviewRenderer(this, sectionKey)

        startButton.setOnClickListener { iniciarInspecao() }
        closeButton.setOnClickListener { finish() }

        val overrideChecklist = intent.getStringExtra("initialChecklist")?.let { raw ->
            try {
                JSONObject(raw)
            } catch (_: Exception) {
                null
            }
        }

        if (overrideChecklist != null) {
            mostrarEstadoCarregando()
            val rendered = renderer.render(container, overrideChecklist)
            if (rendered) {
                mostrarConteudo()
            } else {
                mostrarEstadoVazio()
            }
        } else {
            carregarChecklistAnterior(renderer)
        }
    }

    private fun carregarChecklistAnterior(renderer: ChecklistPreviewRenderer) {
        mostrarEstadoCarregando()

        Thread {
            val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
                .getString("api_ip", "192.168.0.135")
            if (ip.isNullOrBlank() || obra.isBlank()) {
                mostrarEstadoVazio()
                return@Thread
            }

            val endereco = try {
                val path = if (sectionKey?.equals("posto02", ignoreCase = true) == true) {
                    "/json_api/posto02/checklist"
                } else {
                    "/json_api/checklist"
                }
                val builder = StringBuilder("http://$ip:5000")
                builder.append(path)
                builder.append("?obra=")
                builder.append(URLEncoder.encode(obra, "UTF-8"))
                val anoAtual = ano
                if (!anoAtual.isNullOrBlank()) {
                    builder.append("&ano=")
                    builder.append(URLEncoder.encode(anoAtual, "UTF-8"))
                }
                builder.toString()
            } catch (_: Exception) {
                mostrarEstadoVazio()
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
                    val checklist = ChecklistPayloadUtils.resolveChecklist(sectionKey, json)
                    runOnUiThread {
                        if (renderer.render(container, checklist)) {
                            mostrarConteudo()
                        } else {
                            mostrarEstadoVazio()
                        }
                    }
                } else {
                    mostrarEstadoVazio()
                }
                conn.disconnect()
            } catch (_: Exception) {
                mostrarEstadoVazio()
            }
        }.start()
    }

    private fun mostrarEstadoCarregando() {
        runOnUiThread {
            loadingIndicator.visibility = View.VISIBLE
            scrollView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }
    }

    private fun mostrarConteudo() {
        runOnUiThread {
            loadingIndicator.visibility = View.GONE
            scrollView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun mostrarEstadoVazio() {
        runOnUiThread {
            loadingIndicator.visibility = View.GONE
            scrollView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        }
    }

    private fun iniciarInspecao() {
        val (clazz, extraName, titulo) = when (tipo) {
            "insp_posto02" -> Triple(ChecklistPosto02InspActivity::class.java, "inspetor", getString(R.string.inspetor))
            "insp_posto03_pre" -> Triple(ChecklistPosto03PreInspActivity::class.java, "inspetor", getString(R.string.inspetor))
            "insp_posto04_barramento" -> Triple(ChecklistPosto04BarramentoInspActivity::class.java, "inspetor", getString(R.string.inspetor))
            "insp_posto05_cablagem" -> Triple(ChecklistPosto05CablagemInspActivity::class.java, "inspetor", getString(R.string.inspetor))
            "insp_posto06_pre" -> Triple(ChecklistPosto06PreInspActivity::class.java, "inspetor", getString(R.string.inspetor))
            "insp_posto06_cablagem" -> Triple(ChecklistPosto06Cablagem02InspActivity::class.java, "inspetor", getString(R.string.inspetor))
            "insp_posto08_iqm" -> Triple(ChecklistPosto08IqmActivity::class.java, "inspetor", getString(R.string.inspetor))
            else -> Triple(ChecklistPosto02InspActivity::class.java, "inspetor", getString(R.string.inspetor))
        }

        promptName(this, titulo) { nome ->
            val intent = Intent(this, clazz)
            intent.putExtra("obra", obra)
            intent.putExtra("ano", ano)
            intent.putExtra(extraName, nome)
            startActivity(intent)
            finish()
        }
    }
}
