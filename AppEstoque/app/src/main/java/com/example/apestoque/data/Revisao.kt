package com.example.apestoque.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class Divergencia(
    val numero: List<Int>,
    val pergunta: String,
    val suprimento: List<String>?,
    @Json(name = "produção") val producao: List<String>?,
    @Json(name = "respostas") private val respostas: Map<String, List<String>>? = null,
) : Serializable {
    fun respostaSuprimento(): List<String>? {
        val direta = suprimento
        if (!direta.isNullOrEmpty()) {
            return direta
        }
        return encontrarRespostas(listOf("suprimento"))
    }

    fun respostaProducao(): List<String>? {
        val direta = producao
        if (!direta.isNullOrEmpty()) {
            return direta
        }
        return encontrarRespostas(listOf("produção", "producao", "montador", "inspetor"))
    }

    private fun encontrarRespostas(chaves: List<String>): List<String>? {
        val mapa = respostas ?: return null
        for (chave in chaves) {
            mapa[chave]?.let { return it }
        }
        val normalizadas = chaves.map { it.lowercase() }.toSet()
        for ((chave, valor) in mapa) {
            if (chave.lowercase() in normalizadas) {
                return valor
            }
        }
        return null
    }
}

@JsonClass(generateAdapter = true)
data class RevisaoChecklist(
    val obra: String,
    val ano: String,
    val divergencias: List<Divergencia>
) : Serializable