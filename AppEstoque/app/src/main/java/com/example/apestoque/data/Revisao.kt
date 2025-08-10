package com.example.apestoque.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class Divergencia(
    val numero: Int,
    val pergunta: String,
    val suprimento: List<String>?,
    @Json(name = "produção") val producao: List<String>?,
) : Serializable

@JsonClass(generateAdapter = true)
data class RevisaoChecklist(
    val obra: String,
    val ano: String,
    val divergencias: List<Divergencia>
) : Serializable
