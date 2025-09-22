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
import com.example.appoficina.R
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
    private val previewToggleButton: ImageButton? = null,
    private val sectionKey: String? = null,
) {
    private var previewLoaded = false
    private var previewVisible = false
    private var fetchInProgress = false

    init {
        previewCloseButton.setOnClickListener {
            hidePreview()
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

        previewToggleButton?.apply {
            visibility = View.GONE
            isEnabled = false
            setImageResource(android.R.drawable.ic_menu_view)
            contentDescription = activity.getString(R.string.show_previous_checklist)
            setOnClickListener {
                if (previewVisible) {
                    hidePreview()
                } else if (previewLoaded) {
                    showPreview(animated = true)
                }
            }
        }
    }

    fun loadPreviousChecklist(obra: String?, ano: String?) {
        if (obra.isNullOrBlank() || previewLoaded || fetchInProgress) {
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

    private data class SectionInfo(val container: JSONObject, val itens: JSONArray)

    private fun mostrarChecklist(checklist: JSONObject) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val section = selecionarSecao(checklist)
        previewContent.removeAllViews()

        if (section == null) {
            exibirMensagemVazia()
            return
        }

        val itens = section.itens
        if (itens.length() == 0) {
            exibirMensagemVazia()
            return
        }

        previewLoaded = true

        val density = activity.resources.displayMetrics.density
        val paddingGrande = (16 * density).toInt()
        val paddingPequeno = (8 * density).toInt()

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

        val nomes = linkedMapOf<String, String>()

        checklist.optJSONObject("respondentes")?.let { respondentes ->
            val iterator = respondentes.keys()
            while (iterator.hasNext()) {
                val papel = iterator.next()
                val nome = respondentes.optString(papel)
                if (nome.isNotBlank()) {
                    nomes[papel] = nome
                }
            }
        }

        val papeisExtras = listOf("suprimento", "produção", "montador", "inspetor")
        for (papel in papeisExtras) {
            val nome = section.container.optString(papel)
            if (nome.isNotBlank()) {
                nomes.putIfAbsent(papel, nome)
            }
        }

        if (nomes.isNotEmpty()) {
            cabecalho.append("\n")
            cabecalho.append(
                nomes.entries.joinToString("\n") { (papel, nome) ->
                    "${formatarPapel(papel)}: $nome"
                },
            )
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

        previewToggleButton?.apply {
            visibility = View.VISIBLE
            isEnabled = true
        }

        showPreview(animated = true)
    }

    private fun selecionarSecao(checklist: JSONObject): SectionInfo? {
        sectionKey?.let { chave ->
            val alvoDireto = checklist.optJSONObject(chave)
            val candidatoDireto = alvoDireto?.optJSONArray("itens")
            if (alvoDireto != null && candidatoDireto != null) {
                return SectionInfo(alvoDireto, candidatoDireto)
            }

            val iterator = checklist.keys()
            while (iterator.hasNext()) {
                val atual = iterator.next()
                if (atual.equals(chave, ignoreCase = true)) {
                    val secao = checklist.optJSONObject(atual)
                    val itens = secao?.optJSONArray("itens")
                    if (secao != null && itens != null) {
                        return SectionInfo(secao, itens)
                    }
                }
            }
        }

        val itensTopo = checklist.optJSONArray("itens")
        if (itensTopo != null) {
            return SectionInfo(checklist, itensTopo)
        }

        val iterator = checklist.keys()
        while (iterator.hasNext()) {
            val chave = iterator.next()
            val secao = checklist.optJSONObject(chave) ?: continue
            val itens = secao.optJSONArray("itens") ?: continue
            if (itens.length() > 0) {
                return SectionInfo(secao, itens)
            }
        }

        return null
    }

    private fun exibirMensagemVazia() {
        previewLoaded = true
        val density = activity.resources.displayMetrics.density
        val padding = (16 * density).toInt()
        previewContent.setPadding(padding, padding, padding, padding)
        previewContent.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.no_previous_checklist)
            },
        )
        previewToggleButton?.apply {
            visibility = View.VISIBLE
            isEnabled = true
        }
        showPreview(animated = true)
    }

    private fun showPreview(animated: Boolean) {
        if (!previewLoaded || activity.isFinishing || activity.isDestroyed) {
            return
        }

        previewScroll.scrollTo(0, 0)
        previewToggleButton?.apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            contentDescription = activity.getString(R.string.hide_previous_checklist)
        }

        if (animated) {
            previewContainer.alpha = 0f
            previewContainer.visibility = View.VISIBLE
            previewContainer.animate().alpha(1f).setDuration(200L).start()
        } else {
            previewContainer.animate().cancel()
            previewContainer.alpha = 1f
            previewContainer.visibility = View.VISIBLE
        }
        previewVisible = true
    }

    private fun hidePreview() {
        previewContainer.animate().cancel()
        previewContainer.visibility = View.GONE
        previewVisible = false
        previewToggleButton?.apply {
            setImageResource(android.R.drawable.ic_menu_view)
            contentDescription = activity.getString(R.string.show_previous_checklist)
        }
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

