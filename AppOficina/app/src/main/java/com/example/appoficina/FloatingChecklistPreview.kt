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
import java.util.LinkedHashSet
import java.util.Locale

class FloatingChecklistPreview(
    private val activity: AppCompatActivity,
    private val previewContainer: View,
    private val previewContent: LinearLayout,
    private val previewScroll: ScrollView,
    previewHeader: View,
    previewCloseButton: ImageButton,
) {
    private data class SectionResult(val section: JSONObject, val identifier: String?)
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

    fun loadPreviousChecklist(
        obra: String?,
        ano: String?,
        sectionPaths: List<String> = emptyList(),
    ) {
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
                    val checklist = json.optJSONObject("checklist") ?: json
                    activity.runOnUiThread { mostrarChecklist(checklist, sectionPaths) }
                }
                conn.disconnect()
            } catch (_: Exception) {
            } finally {
                fetchInProgress = false
            }
        }.start()
    }

    private fun mostrarChecklist(checklist: JSONObject, sectionPaths: List<String>) {
        if (previewShown || activity.isFinishing || activity.isDestroyed) {
            return
        }

        val sectionResult = resolveSection(checklist, sectionPaths) ?: return
        val section = sectionResult.section
        val itens = section.optJSONArray("itens") ?: return
        if (itens.length() == 0) {
            return
        }

        previewShown = true

        val density = activity.resources.displayMetrics.density
        val paddingGrande = (16 * density).toInt()
        val paddingPequeno = (8 * density).toInt()

        previewContent.removeAllViews()
        previewContent.setPadding(paddingGrande, paddingGrande, paddingGrande, paddingGrande)

        val headerText = buildHeaderText(checklist, sectionResult)
        if (headerText.isNotBlank()) {
            val headerView = TextView(activity).apply {
                text = headerText
                setPadding(0, 0, 0, paddingPequeno)
            }
            previewContent.addView(headerView)
        }

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
        val ajustado = papel.replace('_', ' ').replace('-', ' ').trim()
        if (ajustado.isEmpty()) {
            return papel
        }
        return ajustado.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { parte ->
                parte.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
    }

    private fun buildHeaderText(
        root: JSONObject,
        sectionResult: SectionResult,
    ): String {
        val section = sectionResult.section
        val builder = StringBuilder()
        val obra = root.optString("obra").ifBlank { section.optString("obra") }
        if (obra.isNotBlank()) {
            builder.append("Obra: ")
            builder.append(obra)
        }

        val ano = root.optString("ano").ifBlank { section.optString("ano") }
        if (ano.isNotBlank()) {
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append("Ano: ")
            builder.append(ano)
        }

        val respondentes = LinkedHashSet<String>()
        addRespondentes(respondentes, root.optJSONObject("respondentes"))
        if (section !== root) {
            addRespondentes(respondentes, section.optJSONObject("respondentes"))
            addRespondenteFields(
                section,
                respondentes,
                listOf("montador", "inspetor", "produção", "producao", "suprimento"),
            )

            val identificador = sectionResult.identifier
            if (!identificador.isNullOrBlank()) {
                if (builder.isNotEmpty()) builder.append('\n')
                builder.append("Seção: ")
                builder.append(identificador)
            }
        }

        if (respondentes.isNotEmpty()) {
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(respondentes.joinToString("\n"))
        }

        return builder.toString()
    }

    private fun addRespondenteFields(
        source: JSONObject?,
        destino: MutableSet<String>,
        campos: List<String>,
    ) {
        if (source == null) return
        for (campo in campos) {
            val valor = source.optString(campo)
            if (valor.isNotBlank()) {
                destino.add("${formatarPapel(campo)}: $valor")
            }
        }
    }

    private fun addRespondentes(destino: MutableSet<String>, origem: JSONObject?) {
        if (origem == null) return
        val iterator = origem.keys()
        while (iterator.hasNext()) {
            val papel = iterator.next()
            val nome = origem.optString(papel)
            if (nome.isNotBlank()) {
                destino.add("${formatarPapel(papel)}: $nome")
            }
        }
    }

    private fun resolveSection(root: JSONObject, sectionPaths: List<String>): SectionResult? {
        val itensDiretos = root.optJSONArray("itens")
        if (itensDiretos != null && hasChecklistItems(itensDiretos)) {
            return SectionResult(root, null)
        }

        for (path in sectionPaths) {
            val alvo = traversePath(root, path)
            if (alvo != null) {
                val itens = alvo.optJSONArray("itens")
                if (itens != null && hasChecklistItems(itens)) {
                    val nome = formatarPapel(path.substringAfterLast('.'))
                    return SectionResult(alvo, nome)
                }
            }
        }

        val fallback = findFirstChecklistSection(root)
        return fallback?.let { SectionResult(it, null) }
    }

    private fun traversePath(root: JSONObject, path: String): JSONObject? {
        var atual: JSONObject? = root
        val partes = path.split('.').map { it.trim() }.filter { it.isNotEmpty() }
        for (parte in partes) {
            atual = atual?.optJSONObject(parte) ?: return null
        }
        return atual
    }

    private fun findFirstChecklistSection(obj: JSONObject?): JSONObject? {
        if (obj == null) return null
        val itens = obj.optJSONArray("itens")
        if (itens != null && hasChecklistItems(itens)) {
            return obj
        }

        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val nested = obj.optJSONObject(key)
            val encontrado = findFirstChecklistSection(nested)
            if (encontrado != null) {
                return encontrado
            }
        }
        return null
    }

    private fun hasChecklistItems(itens: JSONArray): Boolean {
        for (i in 0 until itens.length()) {
            val item = itens.optJSONObject(i)
            if (item != null && (item.has("respostas") || item.has("resposta"))) {
                return true
            }
        }
        return false
    }
}
