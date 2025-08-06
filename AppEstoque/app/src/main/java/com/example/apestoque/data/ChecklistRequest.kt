package com.example.apestoque.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChecklistItem(
    val pergunta: String,
    val resposta: List<String>
)

@JsonClass(generateAdapter = true)
data class ChecklistRequest(
    val obra: String,
    val ano: String,
    val suprimento: String,
    val itens: List<ChecklistItem>
)

