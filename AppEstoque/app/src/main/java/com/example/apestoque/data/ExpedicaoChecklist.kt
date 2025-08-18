package com.example.apestoque.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExpedicaoChecklist(
    val obra: String,
    val ano: String,
    val itens: List<ExpedicaoItem>
)

@JsonClass(generateAdapter = true)
data class ExpedicaoItem(
    val numero: Int,
    val pergunta: String
)

@JsonClass(generateAdapter = true)
data class ExpedicaoRespostaItem(
    val numero: Int,
    val pergunta: String,
    val resposta: List<String>?
)

@JsonClass(generateAdapter = true)
data class ExpedicaoUploadRequest(
    val obra: String,
    val itens: List<ExpedicaoRespostaItem>
)
