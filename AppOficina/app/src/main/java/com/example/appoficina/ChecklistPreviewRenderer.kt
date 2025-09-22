package com.example.appoficina

import android.content.Context
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ChecklistPreviewRenderer(
    private val context: Context,
    private val sectionKey: String? = null,
) {
    private data class SectionInfo(val container: JSONObject, val itens: JSONArray)

    fun render(target: LinearLayout, checklist: JSONObject): Boolean {
        val section = selecionarSecao(checklist) ?: return false
        val itens = section.itens
        if (itens.length() == 0) {
            return false
        }

        target.removeAllViews()

        val density = context.resources.displayMetrics.density
        val paddingGrande = (16 * density).toInt()
        val paddingPequeno = (8 * density).toInt()

        target.setPadding(paddingGrande, paddingGrande, paddingGrande, paddingGrande)

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

        val headerView = TextView(context).apply {
            text = cabecalho.toString()
            setPadding(0, 0, 0, paddingPequeno)
        }
        target.addView(headerView)

        for (i in 0 until itens.length()) {
            val item = itens.optJSONObject(i) ?: continue
            val pergunta = item.optString("pergunta")
            if (pergunta.isNotBlank()) {
                val perguntaView = TextView(context).apply {
                    text = pergunta
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, if (i == 0) 0 else paddingPequeno, 0, 0)
                }
                target.addView(perguntaView)
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
                    val respostaView = TextView(context).apply {
                        text = "\u2022 ${formatarPapel(papel)}: $valor"
                        setPadding(0, paddingPequeno / 2, 0, 0)
                    }
                    target.addView(respostaView)
                    adicionouResposta = true
                }
            }

            if (!adicionouResposta) {
                val valorSimples = formatarValor(item.opt("resposta"))
                if (valorSimples.isNotBlank()) {
                    val respostaView = TextView(context).apply {
                        text = "\u2022 Resposta: $valorSimples"
                        setPadding(0, paddingPequeno / 2, 0, 0)
                    }
                    target.addView(respostaView)
                }
            }
        }

        return target.childCount > 0
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
