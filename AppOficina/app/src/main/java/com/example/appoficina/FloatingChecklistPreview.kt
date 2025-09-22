package com.example.appoficina

import android.content.Context
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class FloatingChecklistPreview(
    private val activity: AppCompatActivity,
    private val previewContainer: View,
    private val previewContent: LinearLayout,
    private val previewScroll: ScrollView,
    previewHeader: View,
    previewCloseButton: ImageButton,
) {
    private var previewShown = false
    private var fetchInProgress = false

    init {
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
    }

    fun loadPreviousChecklist(obra: String?, ano: String?) {
        if (obra.isNullOrBlank() || previewShown || fetchInProgress) {
            return
        }

        fetchInProgress = true

        Thread {
            val ip = activity.getSharedPreferences("config", Context.MODE_PRIVATE)
                .getString("api_ip", "192.168.0.135")
            if (ip.isNullOrBlank()) {
                fetchInProgress = false
                return@Thread
            }

            val endereco = try {
                val builder = StringBuilder("http://$ip:5000/json_api/checklist?obra=")
                builder.append(URLEncoder.encode(obra, "UTF-8"))
                if (!ano.isNullOrBlank()) {
                    builder.append("&ano=")
                    builder.append(URLEncoder.encode(ano, "UTF-8"))
                }
                builder.toString()
            } catch (_: Exception) {
                fetchInProgress = false
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
                        activity.runOnUiThread { mostrarChecklist(checklist) }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            } finally {
                fetchInProgress = false
            }
        }.start()
    }

    private fun mostrarChecklist(checklist: JSONObject) {
        if (previewShown || activity.isFinishing || activity.isDestroyed) {
            return
        }

        val itens = checklist.optJSONArray("itens") ?: return
        if (itens.length() == 0) {
            return
        }

        previewShown = true

        val density = activity.resources.displayMetrics.density
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

        val headerView = TextView(activity).apply {
            text = cabecalho.toString()
            setPadding(0, 0, 0, paddingPequeno)
        }
        previewContent.addView(headerView)

        for (i in 0 until itens.length()) {
            val item = itens.optJSONObject(i) ?: continue
            val pergunta = item.optString("pergunta")
            if (pergunta.isNotBlank()) {
                val perguntaView = TextView(activity).apply {
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
                    val respostaView = TextView(activity).apply {
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
                    val respostaView = TextView(activity).apply {
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
                when {
                    partes.isEmpty() -> ""
                    partes.size == 1 -> partes[0]
                    else -> {
                        val resposta = partes.first()
                        val nomes = partes.drop(1).joinToString(", ")
                        "$resposta ($nomes)"
                    }
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

